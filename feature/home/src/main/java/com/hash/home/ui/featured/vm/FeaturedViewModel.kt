package com.hash.home.ui.featured.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hash.home.data.model.NewsTypeListBean
import com.hash.common.ext.isNotNullAndEmpty
import com.hash.common.ext.mmkvGetObject
import com.hash.common.ext.mmkvPutObject
import com.hash.common.shared.storage.MMKVConst
import com.hash.home.data.repository.FeaturedRepository

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
        val cachedTabs = MMKVConst.KEY_FEATURED_NEWS_TYPE_LIST.mmkvGetObject<NewsTypeListBean>()
        if (cachedTabs.isNotNullAndEmpty()) {
            _tabList.value = cachedTabs
        }

        repository.newsType()
            .onBody { networkTabs ->
                if (networkTabs.isNotNullAndEmpty() && networkTabs != cachedTabs) {
                    _tabList.value = networkTabs
                    MMKVConst.KEY_FEATURED_NEWS_TYPE_LIST.mmkvPutObject(networkTabs)
                }
            }
            .enqueue(viewModelScope)
    }

}
