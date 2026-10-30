package com.hash.home.ui.featured.detail.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hash.common.base.viewmodel.BaseViewModel
import com.hash.home.data.model.NewsDetailBean
import com.hash.home.data.repository.NewsDetailRepository
import kotlinx.coroutines.launch

class NewsDetailViewModel : BaseViewModel() {

    private val repository = NewsDetailRepository()
    private val _detail = MutableLiveData<NewsDetailBean>()
    val detail: LiveData<NewsDetailBean> = _detail
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun load(newsId: String) {
        if (newsId.isBlank()) {
            _errorMessage.value = "新闻参数无效"
            return
        }
        showLoading()
        viewModelScope.launch {
            val detail = repository.load(newsId)
            if (detail == null) {
                _errorMessage.value = "加载失败，请稍后重试"
            } else {
                _detail.value = detail
            }
            dismissLoading()
        }
    }
}
