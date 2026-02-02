package com.hash.home.home.ui

import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
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

    // 保存对内层 host 回调的引用以便移除
    private var innerPageCallback: ViewPager2.OnPageChangeCallback? = null

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
        // 尝试在 adapter 更新后绑定内层回调（若当前显示的是 host）
        binding.viewpager.post {
            if (binding.viewpager.currentItem == 1) {
                findInnerHost()?.addOnInnerPageChangeCallback(innerPageCallback!!)
                // 同步 indicator 到内层实际当前项
                findInnerHost()?.let { host ->
                    binding.indicator.onPageSelected(host.getCurrentInnerItem() + 1)
                }
            }
        }
    }

    private fun initTabIndicator() {
        commonNavigator = CommonNavigator(requireContext())
        commonNavigator.isAdjustMode = false
        commonNavigator.adapter = getIndicatorNavAdapter(arrayListOf())
        binding.indicator.navigator = commonNavigator
        pagerAdapter = FeaturedPagerAdapter(childFragmentManager, lifecycle, emptyList())
        binding.viewpager.adapter = pagerAdapter

        // 保持若干页面在内存中，避免频繁的 fragment 销毁/重建，可根据需要调整该值。
        binding.viewpager.offscreenPageLimit = 3

        binding.viewpager.enableNestedPaging()
        ViewPager2Helper.bind(binding.indicator, binding.viewpager)

        // 预创建一个可复用的内层 page 回调，统一转发到父指示器
        innerPageCallback = object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(innerPos: Int) {
                binding.indicator.onPageSelected(innerPos + 1)
            }

            override fun onPageScrolled(p: Int, offset: Float, pixels: Int) {
                binding.indicator.onPageScrolled(p + 1, offset, pixels)
            }

            override fun onPageScrollStateChanged(state: Int) {
                binding.indicator.onPageScrollStateChanged(state)
            }
        }

        // 当外层页面变化时同步指示器；如果外层为内层 host，则将内层选中索引映射为 indicator 索引 = inner + 1
        binding.viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val tabs = viewModel.tabList.value ?: return
                if (tabs.size <= 1) {
                    // 简单情况
                    binding.indicator.onPageSelected(position)
                } else {
                    if (position == 0) {
                        binding.indicator.onPageSelected(0)
                        // 显示第一个页面时移除内层回调
                        findInnerHost()?.removeOnInnerPageChangeCallback()
                    } else {
                        // 显示内层 host 时，确保内层 host 更新指示器
                        val innerHost = findInnerHost()
                        // 注册回调，将内层的选中事件转发给指示器
                        innerHost?.let {
                            innerHost.addOnInnerPageChangeCallback(innerPageCallback!!)

                            // 同步指示器为内层实际当前项
                            binding.indicator.onPageSelected(innerHost.getCurrentInnerItem() + 1)
                        }
                     }
                 }
             }
         })
     }

    private fun findInnerHost(): FeaturedInnerHostFragment? {
        // 在子 FragmentManager 中查找 FeaturedInnerHostFragment 实例
        return childFragmentManager.fragments.filterIsInstance<FeaturedInnerHostFragment>().firstOrNull()
    }

    override fun onDestroyView() {
        // 移除之前为 viewpager 添加的嵌套滑动支持，避免内存泄漏或重复 listener
        binding.viewpager.disableNestedPaging()
        // 移除任何内层回调
        findInnerHost()?.removeOnInnerPageChangeCallback()
        innerPageCallback = null
        super.onDestroyView()
    }

    fun getIndicatorNavAdapter(tabList: List<String>): IndicatorNavAdapter {
        return IndicatorNavAdapter(
            tabList, textSize = 15f,
            color = com.hash.common.R.color.textTitle,
            isIndicator = false,
            onClick = {
                if (it == 0) {
                    binding.viewpager.currentItem = 0
                } else {
                    // 跳到 host 页面并请求其切换到正确的内层索引
                    binding.viewpager.currentItem = 1
                    // 当 host 已就绪时，将其内层页面设为 it-1
                    val host = findInnerHost()
                    if (host != null) {
                        host.setCurrentInnerItem(it - 1, true)
                    } else {
                        // host 尚未创建：在 adapter 更新后延迟执行设置
                        binding.viewpager.post {
                            findInnerHost()?.setCurrentInnerItem(it - 1, true)
                        }
                    }
                }
            }
        )
    }
}