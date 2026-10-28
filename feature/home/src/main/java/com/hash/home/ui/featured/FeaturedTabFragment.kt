package com.hash.home.ui.featured

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.hash.common.base.fragment.BaseBindingFragment
import com.hash.common.ui.loading.LoadingStateOwner
import com.hash.common.ext.showToast
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

    override val loadingStateOwner: LoadingStateOwner
        get() = viewModel

    val adapter by lazy { FeaturedTabAdapter() }

    companion object {
        const val KEY_TYPE_ID = "typeId"
        fun newInstance(typeId: String): FeaturedTabFragment {
            val fragment = FeaturedTabFragment()
            fragment.arguments = Bundle().apply {
                putString(KEY_TYPE_ID, typeId)
            }
            return fragment
        }
    }


    override fun layoutId(): Int = R.layout.fragment_featured_tab

    override fun initParam() {
        viewModel.initTypeId(getString(KEY_TYPE_ID).orEmpty())
    }

    override fun initView() {
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
    }

    override fun loadData() {
        viewModel.loadInitial()
    }

    override fun listener() {
        binding.refreshLayout.setOnRefreshListener {
            binding.refreshLayout.resetNoMoreData()
            if (!viewModel.refresh()) binding.refreshLayout.finishRefresh()
        }
        binding.refreshLayout.setOnLoadMoreListener {
            if (!viewModel.loadMore()) binding.refreshLayout.finishLoadMore()
        }
    }

    override fun observer() {
        viewModel.newsList.observe(this) { news ->
            adapter.submitList(news)
            if (viewModel.isLoadingMore) {
                if (viewModel.hasMore) {
                    binding.refreshLayout.finishLoadMore()
                } else {
                    binding.refreshLayout.finishLoadMoreWithNoMoreData()
                }
            } else {
                binding.refreshLayout.finishRefresh()
            }
        }
        viewModel.errorMessage.observe(this) { message ->
            if (viewModel.isLoadingMore) {
                binding.refreshLayout.finishLoadMore(false)
            } else {
                binding.refreshLayout.finishRefresh(false)
            }
            message.showToast()
        }
    }
}
