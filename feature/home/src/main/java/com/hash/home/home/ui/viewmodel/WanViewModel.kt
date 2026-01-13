package com.hash.home.home.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hash.bean.home.HomeListBean
import com.hash.common.const.AppConst
import com.hash.net.net.request.onBodyOf
import com.hash.repository.home.HomeRepository

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

    var hasMore = true

    private val _data by lazy { MutableLiveData<MutableList<HomeListBean.HomeListItem>>() }
    val data: LiveData<MutableList<HomeListBean.HomeListItem>> = _data


    fun refresh() {
        page = 0
        getHomeList()
    }


    fun getHomeList() {
        page++
        repository.homeList(page, AppConst.PAGE_LIST_SIZE)
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