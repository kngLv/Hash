package com.hash.home.home.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hash.bean.home.NewsTypeListBean
import com.hash.common.ext.isNotNullAndEmpty
import com.hash.common.ext.logD
import com.hash.common.ext.mmkvGetObject
import com.hash.common.ext.mmkvPutObject
import com.hash.common.storage.MMKVConst
import com.hash.net.net.request.onBodyOf
import com.hash.repository.home.FeaturedRepository

/**
 * Created by KngLv
 * @time 2026/1/5 09:17
 * @description
 */

class FeaturedViewModel : ViewModel() {

    val repository by lazy { FeaturedRepository() }

    private val _tabList: MutableLiveData<NewsTypeListBean> = MutableLiveData()
    val tabList: LiveData<NewsTypeListBean> = _tabList

    fun newsType() {
        val historyList = MMKVConst.KEY_FEATURED_NEWS_TYPE_LIST.mmkvGetObject<NewsTypeListBean>()
        if (historyList.isNotNullAndEmpty()) {
            logD("FeaturedViewModel newsType use history data")
            _tabList.value = historyList!!
        } else {
            repository.newsType()
                .onBodyOf {
                    //判断和历史数据是否相同
                    if (historyList == it) {
                        logD("FeaturedViewModel newsType data no change")
                        return@onBodyOf
                    }
                    _tabList.value = it
                    if (it.isNotNullAndEmpty()) {
                        MMKVConst.KEY_FEATURED_NEWS_TYPE_LIST.mmkvPutObject(it)
                    }
                }
                .enqueue(viewModelScope)
        }
    }

}