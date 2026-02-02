package com.hash.home.home.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.hash.home.home.ui.FeaturedTabFragment

class FeaturedInnerPagerAdapter(
    fm: FragmentManager,
    lifecycle: Lifecycle,
    private val names: List<String>,
    private val ids: List<Int>
) : FragmentStateAdapter(fm, lifecycle) {

    override fun getItemCount(): Int = names.size

    override fun createFragment(position: Int): Fragment {
        val name = names[position]
        val id = ids.getOrNull(position)?.toString() ?: ""
        return FeaturedTabFragment.newInstance(name, id)
    }

}

