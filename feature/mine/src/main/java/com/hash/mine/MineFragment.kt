package com.hash.mine

import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.hash.common.base.fragment.BaseBindingFragment
import com.hash.mine.databinding.FragmentMineBinding
import com.hash.mine.viewModel.MineViewModel
import com.hash.repository.login.LoginState
import com.hash.router.RouterActivityPath
import com.hash.router.RouterFragmentPath
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.zip

/**
 * @name MineFragment
 * @package com.hash.mine
 * @author 345 QQ:1831712732
 * @time 2024/12/15 20:27
 * @description
 */
@Route(path = RouterFragmentPath.Mine.MINE)
class MineFragment : BaseBindingFragment<FragmentMineBinding>() {

    val viewModel by viewModels<MineViewModel>()

    // 保存上次在界面上渲染的 userInfo，用于避免重复渲染
    private var lastDisplayedUserInfo: com.hash.bean.mine.UserInfoBean? = null

    override fun layoutId(): Int = R.layout.fragment_mine

    override fun initView() {

    }

    override fun observer() {
        // 1) 将 loginState 的监听绑定到 Fragment 的生命周期（onCreate..onDestroy），
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.CREATED) {
                // 使用 zip + drop 对相邻两次发射进行配对，得到 (old, new)
                // 这样可以精确判断“前一次状态 -> 当前状态”的转变，避免把只有一次初始发射误判为变化。
                val flow = viewModel.loginState
                flow.zip(flow.drop(1)) { old, new -> old to new }
                    .collect { (old, new) ->
                        println("=======================> MineFragment observe loginState: $old -> $new")
                        // 从登录中变为已登录（LOGGED）时触发刷新
                        if (old == LoginState.LOGGING && new == LoginState.LOGGED) {
                            viewModel.refreshUserInfo()
                        }
                    }
            }

        }


        // 2) 将与 UI 相关的 collection 绑定到 viewLifecycleOwner，避免在 view 已销毁时访问 binding
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userInfoFlow.collect { userInfo ->
                        // 每次收到 userInfo 时都与 lastDisplayedUserInfo 比较，只有变化才更新 UI。
                        if (userInfo != null && userInfo != lastDisplayedUserInfo) {
                            renderUserInfo(userInfo)
                            lastDisplayedUserInfo = userInfo
                        }
                    }
                }
            }
        }
    }

    private fun renderUserInfo(userInfo: com.hash.bean.mine.UserInfoBean) {
        // 安全地访问 binding 并更新 UI
        if (view != null && ::binding.isInitialized) {
            // 示例：把 userInfo 序列化到根 view 的 contentDescription，便于无障碍和调试
            binding.root.contentDescription = "userInfo:${userInfo.toString()}"
            // TODO: 在这里把实际字段渲染到具体控件上，如：binding.username.text = userInfo.name ?: ""
        }
    }

    override fun loadData() {
        viewModel.refreshUserInfo()
    }

    override fun listener() {
        super.listener()
        binding.login.setOnClickListener {
            ARouter.getInstance().build(RouterActivityPath.Login.LOGIN)
                .navigation(requireActivity())
//            viewModel.refreshUserInfo()
        }
    }
}