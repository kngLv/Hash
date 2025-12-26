package com.hash.home.home.ui

import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.chad.library.adapter4.util.setOnDebouncedItemClick
import com.hash.common.base.fragment.BaseBindingFragment
import com.hash.common.ext.showToast
import com.hash.common.ext.toJson
import com.hash.home.R
import com.hash.home.databinding.FragmentWanBinding
import com.hash.home.home.ui.adapter.WanAdapter
import com.hash.home.home.viewmodel.WanViewModel

/**
 * @name RecommendFragment
 * @package com.hash.home
 * @author 345 QQ:1831712732
 * @time 2024/12/21 00:12
 * @description
 */
class WanFragment : BaseBindingFragment<FragmentWanBinding>() {

    private val viewModel by viewModels<WanViewModel>()

    private val adapter by lazy { WanAdapter() }

    override fun layoutId(): Int = R.layout.fragment_wan

    override fun initView() {
        binding.recycler.layoutManager =
            StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        binding.recycler.adapter = adapter
    }

    override fun observer() {
        super.observer()
        viewModel.data.observe(this){
            adapter.submitList(it)
        }
    }

    override fun listener() {
        adapter.setOnDebouncedItemClick { adapter, view, position ->
            val item = adapter.getItem(position)
            showToast("点击了:${item.title}")
        }
    }

    override fun loadData() {
        viewModel.getHomeList()
    }
}