package com.hash.home.home.ui

import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import com.hash.common.base.fragment.BaseBindingFragment
import com.hash.common.ext.disableNestedPaging
import com.hash.common.ext.enableNestedPaging
import com.hash.home.R
import com.hash.home.databinding.FragmentFeaturedInnerBinding
import com.hash.home.home.ui.adapter.FeaturedInnerPagerAdapter

/**
 * Host fragment that contains an inner ViewPager2 for remaining tabs (index 1..n)
 */
class FeaturedInnerHostFragment : BaseBindingFragment<FragmentFeaturedInnerBinding>() {

    private lateinit var innerAdapter: FeaturedInnerPagerAdapter
    // pageCallback holds the desired callback; isCallbackRegistered表示是否已对 innerViewpager 注册
    private var pageCallback: ViewPager2.OnPageChangeCallback? = null
    private var isCallbackRegistered = false

    companion object {
        private const val KEY_NAMES = "names"
        private const val KEY_IDS = "ids"

        fun newInstance(names: ArrayList<String>, ids: ArrayList<Int>): FeaturedInnerHostFragment {
            val f = FeaturedInnerHostFragment()
            f.arguments = Bundle().apply {
                putStringArrayList(KEY_NAMES, names)
                putIntegerArrayList(KEY_IDS, ids)
            }
            return f
        }
    }

    override fun layoutId(): Int = R.layout.fragment_featured_inner

    override fun initView() {
        val names = arguments?.getStringArrayList(KEY_NAMES) ?: arrayListOf()
        val ids = arguments?.getIntegerArrayList(KEY_IDS) ?: arrayListOf()

        // Build inner adapter fragments
        innerAdapter = FeaturedInnerPagerAdapter(childFragmentManager, lifecycle, names, ids)
        binding.innerViewpager.adapter = innerAdapter

        // keep several pages to improve responsiveness
        binding.innerViewpager.offscreenPageLimit = 2

        binding.innerViewpager.enableNestedPaging()

        // 如果外层提前传入了 pageCallback，确保在 innerViewpager 已创建后完成注册
        pageCallback?.let {
            if (!isCallbackRegistered) {
                binding.innerViewpager.registerOnPageChangeCallback(it)
                isCallbackRegistered = true
            }
        }
    }

    fun setCurrentInnerItem(index: Int, smoothScroll: Boolean = true) {
        if (::innerAdapter.isInitialized && index in 0 until innerAdapter.itemCount) {
            binding.innerViewpager.setCurrentItem(index, smoothScroll)
        }
    }

    fun getCurrentInnerItem(): Int {
        return if (::innerAdapter.isInitialized) binding.innerViewpager.currentItem else 0
    }

    fun addOnInnerPageChangeCallback(cb: ViewPager2.OnPageChangeCallback) {
        // 存储 callback，并在 view 可用时注册
        // 先移除已注册的回调
        removeOnInnerPageChangeCallback()
        pageCallback = cb
        if (::innerAdapter.isInitialized) {
            // binding.innerViewpager 可用
            binding.innerViewpager.registerOnPageChangeCallback(cb)
            isCallbackRegistered = true
        } else {
            // view 未就绪，注册将在 initView 时完成
            isCallbackRegistered = false
        }
    }

    fun removeOnInnerPageChangeCallback() {
        // 如果已经注册到 innerViewpager，尝试注销
        if (isCallbackRegistered && pageCallback != null && ::innerAdapter.isInitialized) {
            binding.innerViewpager.unregisterOnPageChangeCallback(pageCallback!!)
        }
        pageCallback = null
        isCallbackRegistered = false
    }

    override fun onDestroyView() {
        binding.innerViewpager.disableNestedPaging()
        removeOnInnerPageChangeCallback()
        super.onDestroyView()
    }
}
