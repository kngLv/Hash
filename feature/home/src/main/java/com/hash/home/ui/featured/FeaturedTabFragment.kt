package com.hash.home.ui.featured

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.hash.common.base.fragment.BaseBindingFragment
import com.hash.home.R
import com.hash.home.databinding.FragmentFeaturedTabBinding
import com.hash.home.ui.featured.adapter.FeaturedTabAdapter
import com.hash.home.ui.featured.vm.FeaturedTabViewModel

/**
 * Created by KngLv
 * @time 2026/1/7 09:21
 * @description
 */

class FeaturedTabFragment : BaseBindingFragment<FragmentFeaturedTabBinding>() {

    val viewModel by viewModels<FeaturedTabViewModel>()

    val adapter by lazy { FeaturedTabAdapter() }

    companion object {
        const val KEY_TYPE_NAME = "typeName"
        const val KEY_TYPE_ID = "typeId"
        fun newInstance(typeName: String, typeId: String): FeaturedTabFragment {
            val fragment = FeaturedTabFragment()
            fragment.arguments = Bundle().apply {
                putString(KEY_TYPE_NAME, typeName)
                putString(KEY_TYPE_ID, typeId)
            }
            return fragment
        }
    }


    override fun layoutId(): Int = R.layout.fragment_featured_tab

    override fun initParam() {
        viewModel.typeName = getString(KEY_TYPE_NAME) ?: ""
        viewModel.typeId = getString(KEY_TYPE_ID) ?: ""
    }

    override fun initView() {
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
    }

    override fun loadData() {
        if (viewModel.typeId == "532") {
            // 第一页，忽略缓存时间限制，确保能数据快速加载。
            viewModel.autoRefresh(ignoreTtl = true, refreshIfCached = true)
        } else {
            viewModel.autoRefresh()
        }
    }

    override fun listener() {
        binding.refreshLayout.setOnRefreshListener {
            viewModel.useRefresh()
        }
        binding.refreshLayout.setOnLoadMoreListener {
            viewModel.getNewsList(forceRefresh = false)
        }
    }

    override fun observer() {
        viewModel.list.observe(this) {
            if (viewModel.page <= 1) {
                adapter.submitList(it)
            } else {
                adapter.addAll(it)
            }
        }
        viewModel.isNetRequest.observe(this) {
            if (it && viewModel.page <= 1) {
                binding.refreshLayout.autoRefreshAnimationOnly()
            }
        }
        viewModel.isNextPage.observe(this) {
            if (viewModel.page <= 1) {
                binding.refreshLayout.finishRefresh(1000)
            } else {
                if (it) {
                    binding.refreshLayout.finishLoadMore()
                } else {
                    binding.refreshLayout.finishLoadMoreWithNoMoreData()
                }
            }
        }
    }
}