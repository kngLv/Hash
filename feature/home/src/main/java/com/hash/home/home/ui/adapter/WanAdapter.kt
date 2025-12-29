package com.hash.home.home.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter4.BaseQuickAdapter
import com.hash.bean.home.HomeListBean.HomeListItem
import com.hash.home.databinding.ItemWanBinding
import timber.log.Timber

class WanAdapter : BaseQuickAdapter<HomeListItem, WanAdapter.WanVH>() {
    override fun onCreateViewHolder(
        context: Context,
        parent: ViewGroup,
        viewType: Int
    ): WanVH = WanVH(parent)

    override fun onBindViewHolder(
        holder: WanVH,
        position: Int,
        item: HomeListItem?
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
    ) : RecyclerView.ViewHolder(parent)
}