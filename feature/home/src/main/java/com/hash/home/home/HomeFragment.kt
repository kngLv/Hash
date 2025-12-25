package com.hash.home.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.alibaba.android.arouter.facade.annotation.Route
import com.hash.common.base.fragment.BaseBindingFragment
import com.hash.common.ui.indicator.IndicatorNavAdapter
import com.hash.common.ui.helper.ViewPager2Helper
import com.hash.home.R
import com.hash.home.databinding.FragmentHomeBinding
import com.hash.home.home.viewmodel.HomeViewModel
import com.hash.router.RouterFragmentPath
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator

/**
 * @name HomeFragment
 * @package com.hash.home
 * @author 345 QQ:1831712732
 * @time 2024/12/15 19:32
 * @description
 */

@Route(path = RouterFragmentPath.Home.HOME)
class HomeFragment : BaseBindingFragment<FragmentHomeBinding>() {

    val viewModel by viewModels<HomeViewModel>()

    private val tabList = arrayListOf("推荐", "精选")


    override fun layoutId(): Int = R.layout.fragment_home

    override fun initView() {
        initTabIndicator()
    }

    private fun initTabIndicator() {
        val commonNavigator = CommonNavigator(requireContext())
        commonNavigator.isAdjustMode = false
        commonNavigator.adapter = IndicatorNavAdapter(tabList, textSize = 18f, onClick = {
            binding.viewpager.currentItem = it
        })
        binding.indicator.navigator = commonNavigator
        ViewPager2Helper.bind(binding.indicator, binding.viewpager)

        binding.viewpager.adapter =
            object : FragmentStateAdapter(childFragmentManager, lifecycle) {
                override fun getItemCount(): Int = tabList.size
                override fun createFragment(position: Int): Fragment {
                    return when (position) {
                        0 -> FeaturedFragment()
                        1 -> WanFragment()
                        else -> Fragment()
                    }
                }
            }
    }
}