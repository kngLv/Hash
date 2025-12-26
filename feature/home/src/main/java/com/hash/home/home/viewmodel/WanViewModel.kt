package com.hash.home.home.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hash.bean.home.HomeListBean.HomeListItem
import com.hash.net.api.api
import com.hash.net.net.launch.request
import com.hash.net.net.request.onBodyOf
import kotlinx.coroutines.launch

/**
 * @name RecommendViewModel
 * @package com.hash.home.home.viewmodel
 * @author 345 QQ:1831712732
 * @time 2024/12/21 22:44
 * @description
 */
class WanViewModel : ViewModel() {

    var page = 0
    var pageSize = 40

    var hasMore = true

    private val _data by lazy { MutableLiveData<MutableList<HomeListItem>>() }
    val data: LiveData<MutableList<HomeListItem>> = _data


    fun refresh() {
        page = 0
        getHomeList()
    }

    fun nextHomeList() {
        page++
        getHomeList()
    }

    fun getHomeList() {
        viewModelScope.launch {
            request { api.homeList(page, pageSize) }
                .onBodyOf {
                    if (it.curPage == 1) {
                        _data.value = it.datas.toMutableList()
                    } else {
                        hasMore = it.curPage < it.pageCount
                        val list = _data.value ?: mutableListOf()
                        list.addAll(it.datas)
                        _data.value = list
                    }
                }.enqueue()
        }
    }

}