package com.hash.home.home.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hash.bean.home.HomeListBean.HomeListItem
import com.hash.common.ui.helper.AdapterHelper
import com.hash.net.api.api
import com.hash.net.net.launch.request
import com.hash.net.net.request.onBodyOf
import com.hash.repository.home.HomeRepository
import kotlinx.coroutines.launch

/**
 * @name RecommendViewModel
 * @package com.hash.home.home.viewmodel
 * @author 345 QQ:1831712732
 * @time 2024/12/21 22:44
 * @description
 */
class WanViewModel : ViewModel() {

    val repository by lazy { HomeRepository() }

    var page = 0
    var pageSize = AdapterHelper.LIST_PAGE_SIZE

    var hasMore = true

    private val _data by lazy { MutableLiveData<MutableList<HomeListItem>>() }
    val data: LiveData<MutableList<HomeListItem>> = _data


    fun refresh() {
        page = 0
        getHomeList()
    }


    fun getHomeList() {
        page++
        repository.homeList(page, pageSize)
            .onBodyOf {
                if (page == 1) {
                    val beans = it.datas.toMutableList()
                    beans.shuffle()
                    _data.value = beans
                } else {
                    hasMore = it.curPage < it.pageCount
                    val currentList = it.datas.toMutableList()
                    currentList.shuffle()
                    _data.value = currentList
                }
            }
            .onEnd {
                if (!it) {
                    page--
                    _data.value = mutableListOf()
                }
            }
            .enqueue(viewModelScope)
    }

}