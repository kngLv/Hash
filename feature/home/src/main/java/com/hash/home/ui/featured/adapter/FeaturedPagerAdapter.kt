package com.hash.home.ui.featured.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hash.home.data.model.NewsTypeListBean
import com.hash.home.ui.featured.FeaturedTabFragment

/**
 * Created by KngLv
 * @time 2026/1/7 09:26
 * @description
 */

class FeaturedPagerAdapter(
    fm: FragmentManager,
    lifecycle: Lifecycle,
    private var tabs: List<NewsTypeListBean.NewsTypeListBeanItem> = emptyList(),
) : FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        val tab = tabs[position]
        return FeaturedTabFragment.newInstance(tab.typeId.toString())
    }

    override fun getItemId(position: Int): Long = tabs[position].typeId.toLong()

    override fun containsItem(itemId: Long): Boolean = tabs.any { it.typeId.toLong() == itemId }

    fun tabIdAt(position: Int): Long? = tabs.getOrNull(position)?.typeId?.toLong()

    fun resolvePosition(selectedTabId: Long?, fallbackPosition: Int): Int {
        return FeaturedTabPositionResolver.resolve(
            tabIds = tabs.map { it.typeId.toLong() },
            selectedTabId = selectedTabId,
            fallbackPosition = fallbackPosition,
        )
    }

    fun updateTabs(newTabs: List<NewsTypeListBean.NewsTypeListBeanItem>) {
        tabs = newTabs
        notifyDataSetChanged()
    }
}

/** 在 Tab 列表更新后尽量保持用户当前浏览的分类。 */
internal object FeaturedTabPositionResolver {

    fun resolve(tabIds: List<Long>, selectedTabId: Long?, fallbackPosition: Int): Int {
        require(tabIds.isNotEmpty())

        // stable ID 未变化时，即使 Tab 顺序变化也仍回到用户正在浏览的分类。
        val selectedPosition = selectedTabId?.let(tabIds::indexOf)
        if (selectedPosition != null && selectedPosition >= 0) return selectedPosition

        // 当前分类被服务端移除时，保持最接近的有效下标。
        return fallbackPosition.coerceIn(0, tabIds.lastIndex)
    }
}
