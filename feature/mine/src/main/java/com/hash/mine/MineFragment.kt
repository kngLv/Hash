package com.hash.mine

import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.google.android.material.appbar.AppBarLayout
import android.animation.ArgbEvaluator
import android.graphics.Color
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.hash.bean.mine.UserInfoBean
import com.hash.common.base.fragment.BaseBindingFragment
import com.hash.common.core.indicator.IndicatorNavAdapter
import com.hash.common.core.indicator.ViewPager2Helper
import com.hash.common.ext.getColor
import com.hash.mine.databinding.FragmentMineBinding
import com.hash.mine.viewModel.MineViewModel
import com.hash.repository.login.LoginState
import com.hash.router.RouterActivityPath
import com.hash.router.RouterFragmentPath
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.zip
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator
import com.hash.common.ui.helper.ViewPagerSwipeRefreshHelper
import com.hash.common.ui.helper.ProfileBgPullScaleHelper
import kotlin.math.abs

/**
 * @name MineFragment
 * @package com.hash.mine
 * @author 345 QQ:1831712732
 * @time 2024/12/15 20:27
 * @description 我的页面 Fragment
 */
@Route(path = RouterFragmentPath.Mine.MINE)
class MineFragment : BaseBindingFragment<FragmentMineBinding>() {

    val viewModel by viewModels<MineViewModel>()

    // Helper 管理 SwipeRefreshLayout 与 ViewPager2 的触摸/刷新冲突逻辑
    private var swipeHelper: ViewPagerSwipeRefreshHelper? = null

    // AppBar 偏移监听引用，便于在 onDestroyView 中移除，防止泄露
    private var appBarOffsetListener: AppBarLayout.OnOffsetChangedListener? = null

    // 标记当前头像是否已显示到位，避免重复触发动画
    private var isAvatarVisible: Boolean = false

    // 保存上次在界面上渲染的 userInfo，用于避免重复渲染
    private var lastDisplayedUserInfo: UserInfoBean? = null

    // Profile 背景缩放 helper（从 common 模块复用）
    private var profileBgScaleHelper: ProfileBgPullScaleHelper? = null

    override fun layoutId(): Int = R.layout.fragment_mine

    override fun initView() {
        setAppBar()
        initTabIndicator()
        initSwipeRefreshLayout()
    }

    private fun refreshUserInfo(userInfo: UserInfoBean) {
        // 安全地访问 binding 并更新 UI
        if (view != null && ::binding.isInitialized) {
            binding.bean = userInfo
        }
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
                        // 从登录中变为已登录（LOGGED）时触发刷新
                        if (old == LoginState.LOGGING && new == LoginState.LOGGED) {
                            viewModel.refreshUserInfo()
                        }
                    }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userInfoFlow.collect { userInfo ->
                        // 每次收到 userInfo 时都与 lastDisplayedUserInfo 比较，只有变化才更新 UI。
                        if (userInfo != null && userInfo != lastDisplayedUserInfo) {
                            refreshUserInfo(userInfo)
                            lastDisplayedUserInfo = userInfo
                        }
                    }
                }
            }
        }
    }


    override fun loadData() {
        lifecycleScope.launch {
            viewModel.refreshUserInfo()
        }
    }

    override fun listener() {
        binding.layoutMineInfo.btnProfileSettings.setOnClickListener {
            ARouter.getInstance().build(RouterActivityPath.Login.LOGIN)
                .navigation(requireActivity())
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                viewModel.refreshUserInfo()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun initTabIndicator() {
        val tabs = viewModel.tabList
        val commonNavigator = CommonNavigator(requireContext())
        commonNavigator.isAdjustMode = false
        commonNavigator.adapter = IndicatorNavAdapter(tabs, textSize = 18f, onClick = {
            binding.viewpager.currentItem = it
        })
        binding.indicator.navigator = commonNavigator
        ViewPager2Helper.bind(binding.indicator, binding.viewpager)

        binding.viewpager.adapter =
            object : FragmentStateAdapter(childFragmentManager, lifecycle) {
                override fun getItemCount(): Int = tabs.size
                override fun createFragment(position: Int): Fragment {
                    return NoteFragment()
                }
            }
    }

    private fun initSwipeRefreshLayout() {
        // 配置 SwipeRefreshLayout 与 ViewPager2 的交互，避免横向滑动/子视图可滚动时触发刷新
        swipeHelper = ViewPagerSwipeRefreshHelper(
            binding.swipeRefreshLayout,
            binding.viewpager,
            binding.appBarLayout,
            binding.root
        )
        swipeHelper?.install()

        // 使用公共 helper 管理背景缩放逻辑（封装触摸处理）
        try {
            val profileBg = binding.layoutMineInfo.ivProfileBg
            profileBgScaleHelper = ProfileBgPullScaleHelper(
                binding.swipeRefreshLayout,
                profileBg,
                maxPullDistanceDp = 380,
                maxProfileScale = 2f
            )
            profileBgScaleHelper?.install()
        } catch (_: Throwable) {
            // 如果找不到 profileBg，忽略（不影响其它功能）
        }
    }


    private fun setAppBar() {
        val avatar = binding.ivToolbarAvatar
        // 颜色：透明 -> 白色（可替换为项目中任意颜色资源）
        val startColor = Color.TRANSPARENT
        val endColor = R.color.mine_toolbar_anim_end_color.getColor()
        val evaluator = ArgbEvaluator()

        // 在视图测量完成后计算初始的 translationY，使用 post 确保测量已完成
        avatar.post {
            // 记录初始下移距离并初始化头像为隐藏状态
            val startTranslation = (avatar.height.takeIf { it > 0 }
                ?: (32 * resources.displayMetrics.density).toInt()).toFloat()
            avatar.translationY = startTranslation
            avatar.alpha = 0f
            isAvatarVisible = false

            appBarOffsetListener =
                AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                    val totalRange = appBarLayout.totalScrollRange.toFloat()
                    val absOffset = abs(verticalOffset).toFloat()
                    // 完成点改为 45%（0.5f），即当折叠达到 totalRange * 0.5f 时完成动画
                    val finishRatio = 0.5f
                    // 计算 finishRange，使用 coerceAtLeast(1f) 避免除数为 0
                    val finishRange = (totalRange * finishRatio).coerceAtLeast(1f)
                    // 用于 Toolbar 颜色插值的归一化进度（0..1），在达到 finishRange 时为 1
                    val colorFraction = (absOffset / finishRange).coerceIn(0f, 1f)

                    // 插值计算 Toolbar 背景色（按 colorFraction 进度）
                    val color = evaluator.evaluate(colorFraction, startColor, endColor) as Int
                    binding.toolbar.setBackgroundColor(color)

                    // 头像仅在到达 finishRatio（即 colorFraction == 1）时开始移动并淡入，移动时长 300ms
                    val shouldShowAvatar = colorFraction >= 1f
                    if (shouldShowAvatar && !isAvatarVisible) {
                        // 取消任何现有动画并启动显示动画
                        avatar.animate().cancel()
                        avatar.animate()
                            .translationY(0f)
                            .alpha(1f)
                            .setDuration(300L)
                            .withStartAction { /* 开始显示 */ }
                            .start()
                        isAvatarVisible = true
                    } else if (!shouldShowAvatar && isAvatarVisible) {
                        // 低于 0.45 时直接反向（隐藏）并用 300ms
                        avatar.animate().cancel()
                        avatar.animate()
                            .translationY(startTranslation)
                            .alpha(0f)
                            .setDuration(300L)
                            .withStartAction { /* 开始隐藏 */ }
                            .start()
                        isAvatarVisible = false
                    }
                    // 注意：在 colorFraction 变化但未跨越阈值时，我们不做逐帧平移，而是只在跨越阈值时做一次 300ms 的平移动画。
                }

            binding.appBarLayout.addOnOffsetChangedListener(appBarOffsetListener)
        }
    }


    override fun onDestroyView() {
        // 移除监听器以避免泄露 fragment/view
        try {
            if (::binding.isInitialized) {
                appBarOffsetListener?.let { binding.appBarLayout.removeOnOffsetChangedListener(it) }
            }
        } catch (_: Exception) {
            // 忽略异常
        }
        // 卸载 helper
        swipeHelper?.uninstall()
        swipeHelper = null
        // 卸载 profileBg 缩放 helper
        profileBgScaleHelper?.uninstall()
        profileBgScaleHelper = null
        super.onDestroyView()
    }
}