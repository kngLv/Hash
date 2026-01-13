package com.hash.home.home.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hash.bean.home.NewsListBean
import com.hash.common.ext.logE
import com.hash.net.net.request.onBodyOf
import com.hash.repository.home.FeaturedTabRepository

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


    fun onRefresh() {
        page = 0
        getNewsList()
    }

    fun getNewsList() {
        page++
        repository.newsList(typeId, "$page")
            .onBodyOf {
                _list.value = it
            }
            .onEnd {
                if (it.not() && page > 0) page--
                if ((_list.value?.size ?: 0) >= 10) {
                    _isNextPage.value = true
                } else {
                    _isNextPage.value = false
                }
            }
            .enqueue(viewModelScope)
    }

}