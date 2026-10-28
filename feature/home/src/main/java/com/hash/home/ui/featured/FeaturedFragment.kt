package com.hash.home.ui.featured

import androidx.fragment.app.viewModels
import com.hash.home.data.model.NewsTypeListBean
import com.hash.common.base.fragment.BaseBindingFragment
import com.hash.common.ext.disableNestedPaging
import com.hash.common.ext.enableNestedPaging
import com.hash.common.ui.helper.ViewPager2Helper
import com.hash.common.ui.indicator.IndicatorNavAdapter
import com.hash.home.R
import com.hash.home.databinding.FragmentFeaturedBinding
import com.hash.home.ui.featured.adapter.FeaturedPagerAdapter
import com.hash.home.ui.featured.vm.FeaturedViewModel
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator

/**
 * @name FeaturedFragment
 * @package com.hash.home.ui.featured
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
        // Tab 列表可能被服务端重新排序，先记录当前分类 ID，避免用户跳到同下标的其他分类。
        val currentPosition = binding.viewpager.currentItem
        val selectedTabId = pagerAdapter.tabIdAt(currentPosition)
        val tabList = items.map { it.typeName }
        commonNavigator.adapter = getIndicatorNavAdapter(tabList)
        pagerAdapter.updateTabs(items)
        // 优先按原分类 ID 恢复位置；若该分类已被移除，再使用安全的下标兜底。
        binding.viewpager.setCurrentItem(
            pagerAdapter.resolvePosition(selectedTabId, currentPosition),
            false,
        )
    }

    private fun initTabIndicator() {
        commonNavigator = CommonNavigator(requireContext())
        commonNavigator.isAdjustMode = false
        commonNavigator.adapter = getIndicatorNavAdapter(arrayListOf())
        binding.indicator.navigator = commonNavigator
        pagerAdapter = FeaturedPagerAdapter(childFragmentManager, lifecycle, emptyList())
        binding.viewpager.adapter = pagerAdapter

        // 保留相邻页面，减少分类切换时的重复创建。
        binding.viewpager.offscreenPageLimit = 3

        binding.viewpager.enableNestedPaging()
        ViewPager2Helper.bind(binding.indicator, binding.viewpager)
    }

    override fun onDestroyView() {
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
