package com.hash.home.ui.featured.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hash.home.data.model.NewsListBean
import com.hash.home.data.repository.FeaturedTabRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** 管理单个精选分类的首次加载、刷新与分页状态。 */
class FeaturedTabViewModel : ViewModel() {

    private val repository = FeaturedTabRepository()
    private val news = NewsListBean()

    lateinit var typeId: String
        private set

    private var currentPage = 0
    var hasMore = true
        private set
    private var isRequesting = false
    private var currentLoadMode = FeaturedLoadMode.INITIAL

    val isLoadingMore: Boolean
        get() = currentLoadMode.append

    private val _newsList = MutableLiveData<NewsListBean>()
    val newsList: LiveData<NewsListBean> = _newsList

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun initialize(typeId: String) {
        this.typeId = typeId
    }

    fun loadInitial(): Boolean {
        return load(FeaturedLoadMode.INITIAL)
    }

    fun refresh(): Boolean {
        return load(FeaturedLoadMode.REFRESH)
    }

    fun loadMore(): Boolean {
        return load(FeaturedLoadMode.NEXT_PAGE)
    }

    private fun load(mode: FeaturedLoadMode): Boolean {
        if (isRequesting || !::typeId.isInitialized || (mode.append && !hasMore)) return false

        isRequesting = true
        currentLoadMode = mode
        val page = if (mode.append) currentPage + 1 else FIRST_PAGE
        viewModelScope.launch {
            try {
                val result = repository.loadPage(typeId, page, mode.bypassCache)
                currentPage = page
                hasMore = result.size >= PAGE_SIZE
                if (mode.append) news.addAll(result) else news.apply {
                    clear()
                    addAll(result)
                }
                _newsList.value = NewsListBean().apply { addAll(news) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _errorMessage.value = error.message ?: "加载失败，请稍后重试"
            } finally {
                isRequesting = false
            }
        }
        return true
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 10
    }
}

/** 描述一次加载的业务意图，避免由多个 Boolean 组合表达状态。 */
internal enum class FeaturedLoadMode(
    /** 是否跳过内存和磁盘缓存，直接请求网络。 */
    val bypassCache: Boolean,
    val append: Boolean,
) {
    INITIAL(bypassCache = false, append = false),
    REFRESH(bypassCache = true, append = false),
    NEXT_PAGE(bypassCache = false, append = true),
}
