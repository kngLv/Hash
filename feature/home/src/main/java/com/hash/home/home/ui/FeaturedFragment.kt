package com.hash.home.home.ui

import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.hash.bean.home.NewsTypeListBean
import com.hash.common.base.fragment.BaseBindingFragment
import com.hash.common.ext.disableNestedPaging
import com.hash.common.ext.enableNestedPaging
import com.hash.common.ui.helper.ViewPager2Helper
import com.hash.common.ui.indicator.IndicatorNavAdapter
import com.hash.home.R
import com.hash.home.databinding.FragmentFeaturedBinding
import com.hash.home.home.ui.adapter.FeaturedPagerAdapter
import com.hash.home.home.ui.viewmodel.FeaturedViewModel
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator

/**
 * @name FeaturedFragment
 * @package com.hash.home
 * @author 345 QQ:1831712732
 * @time 2024/12/21 00:14
 * @description
 */
class FeaturedFragment : BaseBindingFragment<FragmentFeaturedBinding>() {

    val viewModel by viewModels<FeaturedViewModel>()
    private lateinit var pagerAdapter: FeaturedPagerAdapter
    private lateinit var commonNavigator: CommonNavigator

    override fun layoutId(): Int = R.layout.fragment_featured

    override fun initView() {
        initTabIndicator()
    }

    override fun observer() {
        viewModel.tabList.observe(this) {
            updateTabIndicator(it)
        }
    }

    override fun loadData() {
        viewModel.newsType()
    }

    private fun updateTabIndicator(items: NewsTypeListBean) {
        val tabList = items.map { it.typeName }
        commonNavigator.adapter = getIndicatorNavAdapter(tabList)
        pagerAdapter.updateTabs(items)
    }

    private fun initTabIndicator() {
        commonNavigator = CommonNavigator(requireContext())
        commonNavigator.isAdjustMode = false
        commonNavigator.adapter = getIndicatorNavAdapter(arrayListOf())
        binding.indicator.navigator = commonNavigator
        pagerAdapter = FeaturedPagerAdapter(childFragmentManager, lifecycle, emptyList())
        binding.viewpager.adapter = pagerAdapter

        // Keep a few pages alive to avoid frequent fragment destroy/create. Tweak value as needed.
        binding.viewpager.offscreenPageLimit = 3


        binding.viewpager.enableNestedPaging()
        ViewPager2Helper.bind(binding.indicator, binding.viewpager)
    }

    override fun onDestroyView() {
        // 移除之前为 viewpager 添加的嵌套滑动支持，避免内存泄漏或重复 listener
        binding.viewpager.disableNestedPaging()
        super.onDestroyView()
    }

    fun getIndicatorNavAdapter(tabList: List<String>): IndicatorNavAdapter {
        return IndicatorNavAdapter(
            tabList, textSize = 15f,
            color = com.hash.common.R.color.textTitle,
            isIndicator = false,
            onClick = { binding.viewpager.currentItem = it }
        )
    }
}