package com.hash.home.home.ui.adapter

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hash.bean.home.NewsTypeListBean
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

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        val bean = tabs[position]
        return FeaturedTabFragment.newInstance(bean.typeName, "${bean.typeId}")
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTabs(newTabs: List<NewsTypeListBean.NewsTypeListBeanItem>) {
        tabs = newTabs
        notifyDataSetChanged()
    }
}