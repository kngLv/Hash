package com.hash.main

import androidx.recyclerview.widget.GridLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.hash.common.base.activity.BaseBindingActivity
import com.hash.common.ui.insets.applyBottomBarInsets
import com.hash.main.adapter.MainNavigationAdapter
import com.hash.main.adapter.MainPageAdapter
import com.hash.main.databinding.ActivityMainBinding
import com.hash.router.RouterActivityPath


@Route(path = RouterActivityPath.Main.MAIN)
class MainActivity : BaseBindingActivity<ActivityMainBinding>(),
    MainNavigationAdapter.MainNavigationItemClickListener {

    private val list = arrayListOf<MainNavigationAdapter.MenuItem>()
    private val navigationAdapter by lazy { MainNavigationAdapter(list) }
    private val pageAdapter by lazy { MainPageAdapter(supportFragmentManager, lifecycle, list) }


    override fun layoutId(): Int = R.layout.activity_main

    override fun initView() {
        binding.mainNavigationContainer.applyBottomBarInsets()
        navigationAdapter.onTabClickPosition = this
        binding.mainNavigation.layoutManager = GridLayoutManager(this, 5)
    }

    override fun loadData() {
        list.add(MainNavigationAdapter.MenuItem("首页", R.drawable.main_home_selector))
        list.add(MainNavigationAdapter.MenuItem("发现", R.drawable.main_discover_selector))
        list.add(MainNavigationAdapter.MenuItem("", 0))
        list.add(MainNavigationAdapter.MenuItem("消息", R.drawable.main_msg_selector))
        list.add(MainNavigationAdapter.MenuItem("我的", R.drawable.main_mine_selector))
        binding.mainNavigation.adapter = navigationAdapter
        binding.mainViewpager.adapter = pageAdapter
        binding.mainViewpager.isUserInputEnabled = false
    }

    override fun onTabClickPositionListener(position: Int) {
        when {
            position < 2 -> binding.mainViewpager.setCurrentItem(position, false)
            position == 2 -> {
                ARouter.getInstance().build(RouterActivityPath.Release.RELEASE)
                    .withTransition(
                        com.hash.common.R.anim.activity_from_bottom_to_top_in,
                        com.hash.common.R.anim.activity_from_bottom_to_top_out
                    )
                    .navigation(this)
//                overridePendingTransition(
//                    com.hash.common.R.anim.activity_from_bottom_to_top_in,
//                    com.hash.common.R.anim.activity_from_bottom_to_top_out
//                )
            }

            else -> binding.mainViewpager.setCurrentItem(position - 1, false)
        }
    }
}
