package com.hash.common.ui.helper

import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter4.BaseQuickAdapter
import com.scwang.smart.refresh.layout.SmartRefreshLayout

/**
 * Created by KngLv
 * @time 2025/12/29 09:29
 * @description
 */

class AdapterHelper(
    private val smartRefresh: SmartRefreshLayout,
    private val refresh: ((AdapterHelper) -> Unit)? = null,
    private val loadRefresh: ((AdapterHelper) -> Unit)? = null,
    private val isLoadMore: Boolean = true,
    private val isRefresh: Boolean = true,
) {
    companion object {
        const val LIST_PAGE_SIZE = 30
    }

    var page: Int = 0

    /** 当前页数量 */
    private var curPageCount = 0

    init {
        smartRefresh.setEnableRefresh(isRefresh)
        smartRefresh.setEnableLoadMore(isLoadMore)
        smartRefresh.setOnRefreshListener {
            refresh?.invoke(this)
        }
        if (isLoadMore) {
            smartRefresh.setOnLoadMoreListener {
                if (curPageCount >= LIST_PAGE_SIZE) {
                    loadRefresh?.invoke(this)
                } else {
                    smartRefresh.finishLoadMoreWithNoMoreData()
                }
            }
        }
    }


    /** 下拉刷新 */
    fun autoRefresh() {
        if (!smartRefresh.autoRefresh()) refresh()
    }

    /** 直接刷新 */
    fun refresh() {
        refresh?.invoke(this)
    }

    fun finishRefresh() {
        smartRefresh.finishRefresh()
    }

    fun <T : Any, VH : RecyclerView.ViewHolder> BaseQuickAdapter<T, VH>.refreshData(pageData: Collection<T>?) {
        page = 1
        pageData?.run {
            submitList(this.toMutableList())
            if (items.isEmpty() && isLoadMore) setEmpty()
            curPageCount = pageData.size
            finishRefresh()
        } ?: kotlin.run {
            if (items.isEmpty() && isLoadMore) setEmpty()
            finishRefresh()
        }
        smartRefresh.setEnableLoadMore(isLoadMore)
    }

    fun <T : Any, VH : RecyclerView.ViewHolder> BaseQuickAdapter<T, VH>.loadData(pageData: Collection<T>?) {
        pageData?.run {
            ++page
            curPageCount = pageData.size
            addAll(this)
            smartRefresh.finishLoadMore(true)
        } ?: kotlin.run {
            smartRefresh.finishLoadMore(false)
        }
    }

    fun <T : Any, VH : RecyclerView.ViewHolder> BaseQuickAdapter<T, VH>.setEmpty() {
        if (items.isEmpty()) {
//            setEmptyView(R.layout.layout_empty_data)
        }
    }

}