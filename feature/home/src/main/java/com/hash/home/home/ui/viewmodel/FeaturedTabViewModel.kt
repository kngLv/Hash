package com.hash.home.home.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hash.bean.home.NewsListBean
import com.hash.common.ext.logD
import com.hash.repository.home.FeaturedTabRepository
import kotlinx.coroutines.launch

/**
 * 创建者：KngLv
 * 时间：2026/1/7 09:50
 * 描述：FeaturedTab 的 ViewModel，负责从仓库获取列表并维护 UI 状态
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


    // 缓存 TTL（毫秒），可根据业务调整
    private val cacheTtlMs: Long = 120_000L


    /** 自动刷新（用于页面切换或自动触发），并重置页码 */
    fun autoRefresh(ignoreTtl: Boolean = false, refreshIfCached: Boolean = false) {
        page = 0
        getNewsList(
            forceRefresh = false, ignoreTtl = ignoreTtl, refreshIfCached = refreshIfCached,
            onNetworkStarted = { isStarted ->
                _isNetRequest.value = isStarted
            },
        )
    }

    /** 手动下拉刷新，强制走网络并重置页码 */
    fun useRefresh() {
        page = 0
        getNewsList(forceRefresh = true)
    }


    /** 获取新闻列表
     * @param forceRefresh 是否强制刷新（跳过缓存直接请求网络）
     * @param ignoreTtl 是否忽略缓存时间限制（即使未过期也从缓存读取）
     * @param refreshIfCached 当命中缓存时是否仍然在后台发起强制网络刷新以获取最新数据（默认 false）
     * */
    fun getNewsList(
        forceRefresh: Boolean?,
        ignoreTtl: Boolean = false,
        refreshIfCached: Boolean = false,
        onNetworkStarted: ((Boolean) -> Unit)? = null
    ) {
        // 分页控制：先增加页码
        page++
        val currentPage = page
        // 使用仓库的缓存接口（先尝试从缓存返回，再在后台强制刷新以获取最新数据）
        viewModelScope.launch {
            try {
                // 1) 先尝试直接从缓存读取（快速返回）
                val cached = repository.getNewsListCachedWithSource(
                    typeId,
                    currentPage,
                    ttlMs = cacheTtlMs,
                    ignoreTtl = ignoreTtl,
                    forceRefresh = forceRefresh ?: false,
                    onNetworkStarted = onNetworkStarted
                )
                logD("FeaturedTabViewModel：1 ${cached?.source}  ${cached?.data?.size}")
                if (cached != null) {
                    _list.value = cached.data
                }
                // 2) 根据 refreshIfCached 决定是否在后台发起强制刷新（forceRefresh=true），拿到 fresh 并更新 UI
                // 如果没有缓存则一定要刷新；如果命中缓存且 refreshIfCached 为 true 且缓存不是来自网络，则发起后台刷新
                if (cached == null || (refreshIfCached && cached.source != FeaturedTabRepository.CacheSource.NETWORK)) {
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
                    logD("FeaturedTabViewModel：2 ${cached?.source}  ${cached?.data?.size}")
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