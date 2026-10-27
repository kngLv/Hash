package com.hash.home.ui.wan.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter4.BaseQuickAdapter
import com.hash.home.data.model.HomeListBean
import com.hash.home.databinding.ItemWanBinding

class WanAdapter : BaseQuickAdapter<HomeListBean.HomeListItem, WanAdapter.WanVH>() {
    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int
    ): WanVH = WanVH(parent)

    override fun onBindViewHolder(
        holder: WanVH,
        position: Int,
        item: HomeListBean.HomeListItem?
    ) {
        item?.run {
            holder.binding.bean = this
        }
    }

    class WanVH(
        parent: ViewGroup,
        val binding: ItemWanBinding = ItemWanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ),
    ) : RecyclerView.ViewHolder(binding.root)
}