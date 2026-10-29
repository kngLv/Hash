package com.hash.home.ui.featured.vm

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.hash.common.base.viewmodel.BaseViewModel
import com.hash.home.data.model.NewsListBean
import com.hash.home.data.repository.FeaturedTabRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** 管理单个精选分类的首次加载、刷新与分页状态。 */
class FeaturedTabViewModel : BaseViewModel() {

    private val repository = FeaturedTabRepository()
    private val news = NewsListBean()

    lateinit var typeId: String
        private set

    private var currentPage = 0
    var hasMore = true
        private set
    /** 防止首次加载、下拉刷新和分页请求并发执行。 */
    private var isRequesting = false
    /** 是否已经完成过首次内容加载，用于区分首次进入与后续手动刷新。 */
    private var hasLoadedInitial = false
    /** 首次未命中缓存后，等待 RefreshLayout 回调以发起首次网络请求。 */
    private var awaitingInitialRefresh = false
    private var currentLoadMode = FeaturedLoadMode.INITIAL

    val isLoadingMore: Boolean
        get() = currentLoadMode.append

    private val _newsList = MutableLiveData<NewsListBean>()
    val newsList: LiveData<NewsListBean> = _newsList

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    /** 通知页面显示顶部刷新动画；动画触发刷新回调后才开始首次网络请求。 */
    private val _initialRefreshRequested = MutableLiveData<Unit>()
    val initialRefreshRequested: LiveData<Unit> = _initialRefreshRequested

    fun initTypeId(typeId: String) {
        this.typeId = typeId
    }

    fun loadInitial(): Boolean {
        if (isRequesting || !::typeId.isInitialized) return false

        isRequesting = true
        currentLoadMode = FeaturedLoadMode.INITIAL

        viewModelScope.launch {
            var hasCachedPage = false
            try {
                val cachedNews = repository.findCachedPage(
                    typeId = typeId,
                    page = FIRST_PAGE,
                    allowExpired = !hasLoadedInitial,
                )
                hasCachedPage = cachedNews != null
                if (cachedNews != null) {
                    publishPage(FIRST_PAGE, cachedNews, append = false)
                    // 缓存只用于快速展示，随后静默以网络数据校准首页内容。
                    publishInitialPageFromNetwork()
                    hasLoadedInitial = true
                    isRequesting = false
                } else {
                    isRequesting = false
                    awaitingInitialRefresh = true
                    _initialRefreshRequested.value = Unit
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                hasLoadedInitial = true
                isRequesting = false
                if (!hasCachedPage) {
                    _errorMessage.value = error.message ?: "加载失败，请稍后重试"
                }
            }
        }
        return true
    }

    fun refresh(): Boolean {
        if (awaitingInitialRefresh) return requestInitialPageFromNetwork()
        if (!hasLoadedInitial) return loadInitial()
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
                if (mode == FeaturedLoadMode.REFRESH) {
                    repository.clearCachedNextPages(typeId)
                }
                publishPage(page, result, append = mode.append)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _errorMessage.value = error.message ?: "Error..."
            } finally {
                isRequesting = false
            }
        }
        return true
    }

    private fun requestInitialPageFromNetwork(): Boolean {
        if (isRequesting || !::typeId.isInitialized) return false

        awaitingInitialRefresh = false
        isRequesting = true
        currentLoadMode = FeaturedLoadMode.INITIAL
        viewModelScope.launch {
            try {
                publishInitialPageFromNetwork()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _errorMessage.value = error.message ?: "加载失败，请稍后重试"
            } finally {
                hasLoadedInitial = true
                isRequesting = false
            }
        }
        return true
    }

    private suspend fun publishInitialPageFromNetwork() {
        publishPage(
            FIRST_PAGE,
            repository.loadPage(typeId, FIRST_PAGE, bypassCache = true),
            append = false,
        )
    }

    private fun publishPage(page: Int, pageNews: NewsListBean, append: Boolean) {
        currentPage = page
        hasMore = pageNews.size >= PAGE_SIZE
        if (append) {
            news.addAll(pageNews)
        } else {
            news.clear()
            news.addAll(pageNews)
        }
        // 提交新的 List 容器，避免 AsyncListDiffer 持有的旧快照被后续分页修改。
        _newsList.value = NewsListBean().apply { addAll(news) }
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
