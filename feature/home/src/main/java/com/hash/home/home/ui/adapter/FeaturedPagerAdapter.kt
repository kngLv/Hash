package com.hash.home.home.ui.adapter

import android.annotation.SuppressLint
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hash.bean.home.NewsTypeListBean
import com.hash.home.home.ui.FeaturedInnerHostFragment
import com.hash.home.home.ui.FeaturedTabFragment

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

    // When there are more than one tab, implement a 2-page outer pager:
    // position 0 -> first tab fragment
    // position 1 -> a host fragment that contains inner ViewPager2 for remaining tabs
    override fun getItemCount(): Int = if (tabs.size <= 1) tabs.size else 2

    override fun createFragment(position: Int): Fragment {
        if (tabs.isEmpty()) return Fragment()
        if (tabs.size == 1) {
            val bean = tabs[0]
            return FeaturedTabFragment.newInstance(bean.typeName, "${bean.typeId}")
        }
        return if (position == 0) {
            val bean = tabs[0]
            FeaturedTabFragment.newInstance(bean.typeName, "${bean.typeId}")
        } else {
            // build arrays of remaining tabs to pass into the inner host fragment
            val remainNames = ArrayList<String>()
            val remainIds = ArrayList<Int>()
            for (i in 1 until tabs.size) {
                remainNames.add(tabs[i].typeName)
                remainIds.add(tabs[i].typeId)
            }
            FeaturedInnerHostFragment.newInstance(remainNames, remainIds)
        }
    }

    override fun getItemId(position: Int): Long {
        // provide stable ids: first page uses its typeId; inner host uses a sentinel id
        return if (tabs.size <= 1) {
            tabs.getOrNull(position)?.typeId?.toLong() ?: super.getItemId(position)
        } else {
            if (position == 0) tabs.getOrNull(0)?.typeId?.toLong() ?: super.getItemId(position)
            else Long.MIN_VALUE // sentinel for inner host
        }
    }

    override fun containsItem(itemId: Long): Boolean {
        if (tabs.size <= 1) return tabs.any { it.typeId.toLong() == itemId }
        if (itemId == Long.MIN_VALUE) return true
        return tabs.any { it.typeId.toLong() == itemId }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTabs(newTabs: List<NewsTypeListBean.NewsTypeListBeanItem>) {
        tabs = newTabs
        notifyDataSetChanged()
    }
}