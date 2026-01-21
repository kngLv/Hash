package com.hash.home.home.ui

import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.hash.common.base.fragment.BaseBindingFragment
import com.hash.home.R
import com.hash.home.databinding.FragmentFeaturedTabBinding
import com.hash.home.home.ui.adapter.FeaturedTabAdapter
import com.hash.home.home.ui.viewmodel.FeaturedTabViewModel

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
            fragment.arguments = android.os.Bundle().apply {
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
        viewModel.autoRefresh()
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
        viewModel.isNetRequest.observe(this){
            if (it && viewModel.page <=1) {
                binding.refreshLayout.autoRefreshAnimationOnly()
            }
        }
        viewModel.isNextPage.observe(this) {
            if (viewModel.page <= 1) {
                binding.refreshLayout.finishRefresh()
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