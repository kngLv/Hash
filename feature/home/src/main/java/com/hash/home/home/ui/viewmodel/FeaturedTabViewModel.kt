package com.hash.home.home.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hash.bean.home.NewsListBean
import com.hash.repository.home.FeaturedTabRepository
import kotlinx.coroutines.launch

/**
 * Created by KngLv
 * @time 2026/1/7 09:50
 * @description
 */

class FeaturedTabViewModel : ViewModel() {

    val repository = FeaturedTabRepository()
    var typeId: String = ""
    var typeName: String = ""

    var page: Int = 0

    private val _list: MutableLiveData<NewsListBean> = MutableLiveData()
    val list: LiveData<NewsListBean> = _list

    private val _isNextPage: MutableLiveData<Boolean> = MutableLiveData()
    val isNextPage: LiveData<Boolean> = _isNextPage

    private val _isNetRequest: MutableLiveData<Boolean> = MutableLiveData()
    val isNetRequest: LiveData<Boolean> = _isNetRequest


    // 缓存 TTL（ms），可根据业务调整
    private val cacheTtlMs: Long = 60_000L


    /** 自动刷新，重置页码  */
    fun autoRefresh() {
        page = 0
        getNewsList(forceRefresh = false, onNetworkStarted = { isStarted ->
            _isNetRequest.postValue(isStarted)
        })
    }

    /** 手动下拉刷新，重置页码  */
    fun useRefresh() {
        page = 0
        getNewsList(forceRefresh = true)
    }


    fun getNewsList(forceRefresh: Boolean?, onNetworkStarted: ((Boolean) -> Unit)? = null) {
        // 分页控制：先增加页码
        page++
        val currentPage = page
        // 使用 repository 的缓存接口（先尝试从缓存返回，再后台强制刷新）
        viewModelScope.launch {
            try {
                // 1) 先尝试直接取缓存（快速返回）
                val cached = repository.getNewsListCachedWithSource(
                    typeId,
                    currentPage,
                    ttlMs = cacheTtlMs,
                    forceRefresh = forceRefresh ?: false,
                    onNetworkStarted = onNetworkStarted
                )
                if (cached != null) {
                    _list.value = cached.data
                }

                // 2) 后台发起强制刷新（forceRefresh=true），拿到 fresh 并更新 UI
                val fresh = repository.getNewsListCachedWithSource(
                    typeId,
                    currentPage,
                    ttlMs = cacheTtlMs,
                    forceRefresh = true,
                    onNetworkStarted = onNetworkStarted
                )
                if (fresh != null) {
                    _list.value = fresh.data
                }

                // 设置是否还有更多（这里根据返回量判断，和原逻辑保持一致）
                if ((_list.value?.size ?: 0) >= 10) {
                    _isNextPage.value = true
                } else {
                    _isNextPage.value = false
                }

            } catch (_: Exception) {
                // 网络错误则回退页码
                if (currentPage > 0) page--
            }
        }
    }

}