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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import android.widget.ScrollView
import android.widget.AbsListView
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

    // 标记 AppBar 是否已经完全折叠（用于禁止在 ViewPager 占满屏 时触发刷新）
    private var isAppBarFullyCollapsed: Boolean = false

    // AppBar 偏移监听引用，便于在 onDestroyView 中移除，防止泄露
    private var appBarOffsetListener: AppBarLayout.OnOffsetChangedListener? = null

    // 标记当前头像是否已显示到位，避免重复触发动画
    private var isAvatarVisible: Boolean = false

    // 保存上次在界面上渲染的 userInfo，用于避免重复渲染
    private var lastDisplayedUserInfo: UserInfoBean? = null

    override fun layoutId(): Int = R.layout.fragment_mine

    override fun initView() {
        setAppBar()
        initTabIndicator()
        // 配置 SwipeRefreshLayout 与 ViewPager2 的交互，避免横向滑动/子视图可滚动时触发刷新
        setupSwipeRefreshBehavior()
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
                        println("=======================> MineFragment observe loginState: $old -> $new")
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
        viewModel.refreshUserInfo()
    }

    override fun listener() {
        binding.layoutMineInfo.btnProfileSettings.setOnClickListener {
            ARouter.getInstance().build(RouterActivityPath.Login.LOGIN)
                .navigation(requireActivity())
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                delay(3000L)
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
                    // 更新 AppBar 是否完全折叠的标志（接近 totalRange 则认为折叠）
                    isAppBarFullyCollapsed = absOffset >= (totalRange - 1f)
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

    /**
     * 配置 SwipeRefreshLayout 与 ViewPager2 的交互逻辑，避免在横向切换页面或子视图可向上滚动时触发刷新
     */
    private fun setupSwipeRefreshBehavior() {
        // 当子视图可以向上滚动时，表示不应该触发刷新（返回 true 表示 child can scroll up -> SwipeRefresh 不拦截）
        binding.swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
            // 如果 AppBar 已经完全折叠（ViewPager 占满屏幕），则不允许触发刷新
            if (isAppBarFullyCollapsed) return@setOnChildScrollUpCallback true

            val currentScrollable = findCurrentScrollable()
            currentScrollable?.canScrollVertically(-1) ?: false
        }

        // ViewPager2 内部用于承载 page 的 RecyclerView（水平滑动逻辑）
        val pagerTouchView = binding.viewpager.getChildAt(0)
        if (pagerTouchView != null) {
            var startX = 0f
            var startY = 0f
            pagerTouchView.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        startY = event.y
                        // 若 AppBar 已折叠（ViewPager 占满屏），则临时禁用刷新
                        binding.swipeRefreshLayout.isEnabled = !isAppBarFullyCollapsed
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = abs(event.x - startX)
                        val dy = abs(event.y - startY)
                        if (dx > dy) {
                            // 明显的水平滑动，禁用下拉刷新以不干扰页面切换
                            binding.swipeRefreshLayout.isEnabled = false
                        } else {
                            // 垂直滑动时，如果当前子视图可以向上滚动（即内容不在顶部），也禁用刷新
                            // 如果 AppBar 完全折叠，继续禁用刷新
                            if (isAppBarFullyCollapsed) {
                                binding.swipeRefreshLayout.isEnabled = false
                            } else {
                                val currentScrollable = findCurrentScrollable()
                                binding.swipeRefreshLayout.isEnabled = (currentScrollable?.canScrollVertically(-1) == false)
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // 恢复，并在抬起时触发 performClick 以满足无障碍/lint 要求
                        v?.performClick()
                        binding.swipeRefreshLayout.isEnabled = true
                    }
                }
                // 不消费事件，让 ViewPager2 和子视图继续处理
                false
            }
        }
    }

    /**
     * 查找当前 ViewPager 页面中第一个“可滚动”子视图（RecyclerView、NestedScrollView、ScrollView、AbsListView 或任何 canScrollVertically 能返回 true 的视图）
     */
    private fun findCurrentScrollable(): View? {
        // 尝试通过 childFragmentManager 找到当前显示的 Fragment（优先已 attach 且 view 可见）
        val fragments = childFragmentManager.fragments
        val currentFragment = fragments.firstOrNull { f ->
            val view = f.view
            view != null && view.isAttachedToWindow && view.visibility == View.VISIBLE
        } ?: fragments.firstOrNull { f ->
            val view = f.view
            view != null && view.visibility == View.VISIBLE
        } ?: fragments.firstOrNull { f -> f.view != null }

        val rootView = currentFragment?.view ?: return null
        return findScrollableInView(rootView)
    }

    private fun findScrollableInView(v: View): View? {
        // 常见可滚动控件
        if (v is RecyclerView || v is NestedScrollView || v is ScrollView || v is AbsListView) return v
        // 任意 view 如果能向上滚动也能作为滚动目标
        if (v.canScrollVertically(-1)) return v
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) {
                val child = v.getChildAt(i)
                val found = findScrollableInView(child)
                if (found != null) return found
            }
        }
        return null
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
        super.onDestroyView()
    }
}