package com.hash.home.ui.featured.detail

import android.content.Intent
import android.app.PendingIntent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Route
import com.hash.common.R as CommonR
import com.hash.common.base.activity.BaseBindingActivity
import com.hash.common.ext.showToast
import com.hash.common.ext.toAny
import com.hash.common.ext.mmkvGetBoolean
import com.hash.common.ext.mmkvPut
import com.hash.common.ui.loading.LoadingStateOwner
import com.hash.common.ui.insets.applyScrollContentInsets
import com.hash.common.ui.insets.applyTopBarInsets
import com.hash.common.utils.image.ImageLoader
import com.hash.home.R
import com.hash.home.data.model.NewsDetailBean
import com.hash.home.databinding.ActivityNewsDetailBinding
import com.hash.home.ui.featured.detail.adapter.NewsDetailAdapter
import com.hash.home.ui.featured.detail.adapter.NewsDetailPageItem
import com.hash.home.ui.featured.detail.adapter.NewsDetailPageItemFactory
import com.hash.home.ui.featured.detail.vm.NewsDetailViewModel
import com.hash.common.ui.toolbar.HashToolbar.TitleGravity
import com.hash.common.shared.storage.MMKVConst
import com.hash.router.RouterActivityPath
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoOverlayResult
import com.hash.video.api.VideoPlaybackControlToken
import com.hash.video.api.VideoPlaybackNotification
import com.hash.video.api.VideoPlaybackQueueManager
import com.hash.video.api.VideoSystemOverlay
import com.hash.video.api.isPlaying
import com.hash.home.ui.featured.detail.video.NewsDetailVideoPresentationCoordinator
import com.hash.video.ui.player.EmbeddedVideoPlayerView
import com.hash.video.ui.player.BaseHashVideoPlayerView
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Route(path = RouterActivityPath.Home.NEWS_DETAIL)
open class NewsDetailActivity : BaseBindingActivity<ActivityNewsDetailBinding>() {

    private val viewModel by viewModels<NewsDetailViewModel>()
    private val newsId get() = intent.getStringExtra(EXTRA_NEWS_ID).orEmpty()
    private val newsTitle get() = intent.getStringExtra(EXTRA_TITLE).orEmpty()
    private val newsSource get() = intent.getStringExtra(EXTRA_SOURCE).orEmpty()
    private val videoPlaybackToken = VideoPlaybackControlToken()
    private val videoPresentationCoordinator by lazy {
        NewsDetailVideoPresentationCoordinator(
            lifecycleOwner = this,
            recyclerView = binding.contentRecycler,
            adapter = detailAdapter,
        )
    }
    private val detailAdapter by lazy {
        NewsDetailAdapter(
            playbackToken = videoPlaybackToken,
            showLandscape = ::showLandscapeVideo,
            showPortrait = ::showPortraitVideo,
            showVideoNotification = true,
            showVideoNotificationArtwork = true,
        )
    }
    private var isSourceVisible = false
    private var sourceVideoPlayer: EmbeddedVideoPlayerView? = null
    private var activePresentationPlayer: BaseHashVideoPlayerView? = null
    private var presentationMode: VideoPresentationMode? = null
    private var originalRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private var detailRenderRequestId = 0L
    /** 当前详情与悬浮窗播放内容相同，待列表提交后将画面交回页面。 */
    private var shouldRestoreOverlay = false
    private val videoPresentationBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = closeVideoPresentation()
    }
    private val detailExitBackCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() = requestExitDetail()
    }

    override val loadingStateOwner: LoadingStateOwner
        get() = viewModel

    override fun layoutId(): Int = R.layout.activity_news_detail

    override fun initView() = with(binding) {
        resolveOverlayForDetail()
        toolbar.applyTopBarInsets()
        contentRecycler.applyScrollContentInsets()
        contentRecycler.layoutManager = LinearLayoutManager(this@NewsDetailActivity)
        contentRecycler.adapter = detailAdapter
        videoPresentationCoordinator.start()
        toolbar.setDefaultActionClickListener { shareNews() }
        toolbar.setBackClickListener { requestExitDetail() }
        val listenAction = toolbar.createTextAction(
            text = getString(R.string.news_detail_listen),
            onClick = View.OnClickListener {
                getString(R.string.news_detail_no_readable_content).showToast()
            },
        ).apply {
            typeface = Typeface.create("serif", Typeface.NORMAL)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            setTextColor(
                ContextCompat.getColorStateList(
                    this@NewsDetailActivity,
                    R.color.news_detail_listen_text,
                ) ?: ColorStateList.valueOf(
                    ContextCompat.getColor(this@NewsDetailActivity, CommonR.color.textTitle),
                ),
            )
        }
        toolbar.addAction(listenAction)
        toolbar.addAction(toolbar.createIconAction(
            R.drawable.ic_news_detail_more,
            getString(R.string.news_detail_more),
        ) { anchor -> showMoreMenu(anchor) })
        fullscreenVideoPlayer.panel.exitAction.setOnClickListener { closeVideoPresentation() }
        portraitVideoPlayer.panel.exitAction.setOnClickListener { closeVideoPresentation() }
        fullscreenVideoPlayer.playbackToken = videoPlaybackToken
        portraitVideoPlayer.playbackToken = videoPlaybackToken
        setVideoNotificationActivity()
        contentRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateToolbarSource()
            }
        })
        onBackPressedDispatcher.addCallback(this@NewsDetailActivity, detailExitBackCallback)
        onBackPressedDispatcher.addCallback(this@NewsDetailActivity, videoPresentationBackCallback)
    }

    override fun loadData() {
        intent.getStringExtra(EXTRA_DETAIL_JSON)
            .toAny(NewsDetailBean::class.java)
            ?.let(::renderDetail)
            ?: viewModel.load(newsId)
    }

    override fun onNewIntent(intent: Intent) {
        val previousNewsId = newsId
        super.onNewIntent(intent)
        // singleTop 复用页面时，通知点击目标也必须切换到新的详情内容。
        setVideoNotificationActivity()
        resolveOverlayForDetail()
        if (newsId != previousNewsId) {
            viewModel.load(newsId)
        }
    }

    override fun onRestoreData(savedInstanceState: Bundle) {
        if (viewModel.detail.value == null) {
            loadData()
        }
    }

    override fun observer() {
        viewModel.detail.observe(this, ::renderDetail)
        viewModel.errorMessage.observe(this) { message ->
            binding.contentRecycler.visibility = View.GONE
            binding.emptyView.apply {
                text = message
                visibility = View.VISIBLE
            }
        }
    }

    private fun renderDetail(detail: NewsDetailBean) = with(binding) {
        contentRecycler.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        val pageItems = transformPageItems(NewsDetailPageItemFactory.create(
                title = newsTitle,
                source = newsSource,
                postTime = intent.getStringExtra(EXTRA_POST_TIME).orEmpty(),
                detailItems = detail.items,
            ))
        //去重
        val deduplicatedItems = pageItems.deduplicateVideos()
        val requestId = ++detailRenderRequestId
        lifecycleScope.launch {
            val sizedItems = deduplicatedItems.withResolvedVideoCoverSizes()
            if (requestId != detailRenderRequestId) return@launch
            //给视频item分配索引
            val playlistItems = sizedItems.withPlaylistIndices()
            val playbackQueue = playlistItems.mapNotNull { item ->
                (item as? NewsDetailPageItem.Video)?.toVideoMedia()
            }
            VideoPlaybackQueueManager.setPlaybackQueue(videoPlaybackToken, playbackQueue)
            videoPresentationCoordinator.updatePlaybackMapping(
                positions = playlistItems.mapIndexedNotNull { adapterPosition, item ->
                    (item as? NewsDetailPageItem.Video)?.playlistIndex?.let { index ->
                        index to adapterPosition
                    }
                }.toMap(),
            )
            detailAdapter.submitList(playlistItems) {
                videoPresentationCoordinator.onAdapterListCommitted()
                if (
                    shouldRestoreOverlay && VideoSystemOverlay.restoreIfMatches(videoPlaybackToken) {
                        it.contentKey == newsId
                    }
                ) {
                    videoPresentationCoordinator.presentCurrentPlaybackHost()
                }
            }
        }
    }

    /** 右上角更多菜单；始终保留手动开启小窗的入口。 */
    private fun showMoreMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(R.string.news_detail_float_play)
            setOnMenuItemClickListener {
                startOverlayPlayback { finish() }
                true
            }
        }.show()
    }

    /**
     * 处理详情页返回：仅在视频播放中、已有悬浮窗权限且首次退出时询问是否转入小窗。
     * 其他情况按普通详情页退出处理。
     */
    private fun requestExitDetail() {
        if (!VideoPlaybackQueueManager.playbackState.value.status.isPlaying ||
            !VideoSystemOverlay.canDraw(this) ||
            MMKVConst.KEY_FEATURED_VIDEO_OVERLAY_EXIT_PROMPT_HANDLED.mmkvGetBoolean()
        ) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setMessage(R.string.news_detail_float_exit_message)
            .setNegativeButton(R.string.news_detail_exit) { _, _ ->
                MMKVConst.KEY_FEATURED_VIDEO_OVERLAY_EXIT_PROMPT_HANDLED.mmkvPut(true)
                finish()
            }
            .setPositiveButton(R.string.news_detail_float_play) { _, _ ->
                MMKVConst.KEY_FEATURED_VIDEO_OVERLAY_EXIT_PROMPT_HANDLED.mmkvPut(true)
                startOverlayPlayback { finish() }
            }
            .show()
    }

    /**
     * 请求悬浮窗接管当前播放会话；只有窗口成功显示后才执行 [onShown] 并关闭详情页。
     * 未授权时仅打开授权页，当前页面和播放保持不变。
     */
    private fun startOverlayPlayback(onShown: () -> Unit) {
        when (VideoSystemOverlay.enter(this, videoPlaybackToken, createNotificationPendingIntent())) {
            VideoOverlayResult.SHOWN -> onShown()
            VideoOverlayResult.ALREADY_SHOWN -> Unit
            VideoOverlayResult.PERMISSION_REQUIRED -> startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            else -> getString(R.string.news_detail_float_unavailable).showToast()
        }
    }

    /** 在提交列表前并行读取封面原始尺寸，避免播放器模块在绑定后再触发布局变化。 */
    private suspend fun List<NewsDetailPageItem>.withResolvedVideoCoverSizes(): List<NewsDetailPageItem> =
        coroutineScope {
            map { item ->
                async {
                    val video = item as? NewsDetailPageItem.Video ?: return@async item
                    if ((video.coverWidth ?: 0) > 0 && (video.coverHeight ?: 0) > 0) {
                        return@async video
                    }
                    val size = video.coverUrl.takeIf { it.isNotBlank() }?.let { loadImageSize(it) }
                    video.copy(
                        coverWidth = size?.width,
                        coverHeight = size?.height,
                    )
                }
            }.awaitAll()
        }

    private suspend fun loadImageSize(url: String): ImageLoader.ImageSize? =
        suspendCancellableCoroutine { continuation ->
            val request = ImageLoader.loadImageSize(this, url) { size ->
                if (continuation.isActive) continuation.resume(size)
            }
            continuation.invokeOnCancellation { request.cancel() }
        }

    /**
     * 按视频 URL 保留详情页中首次出现的视频条目。
     *
     * 同一 URL 若重复展示，会同时形成重复卡片和重复播放队列项；播放器切换时也无法唯一
     * 确定应由哪张卡片承接画面，因此在建立队列索引前先完成去重。
     */
    private fun List<NewsDetailPageItem>.deduplicateVideos(): List<NewsDetailPageItem> {
        val displayedVideoUrls = mutableSetOf<String>()
        return filter { item ->
            item !is NewsDetailPageItem.Video || displayedVideoUrls.add(item.videoUrl)
        }
    }

    /**
     * 为最终展示的每个视频分配从 0 开始、连续的播放队列索引。
     *
     * 该索引对应 ExoPlayer 队列位置，而不是 RecyclerView 的 Adapter 位置：详情里夹杂的
     * 标题、文字和图片不会计入队列。协调器再通过“队列索引 -> Adapter 位置”映射定位卡片。
     */
    private fun List<NewsDetailPageItem>.withPlaylistIndices(): List<NewsDetailPageItem> {
        var nextIndex = 0
        return map { item ->
            if (item is NewsDetailPageItem.Video) {
                item.copy(
                    playlistIndex = nextIndex++,
                    contentKey = newsId,
                    title = newsTitle,
                    subtitle = newsSource,
                )
            } else {
                item
            }
        }
    }

    private fun NewsDetailPageItem.Video.toVideoMedia(): VideoMedia = VideoMedia(
        id = videoUrl,
        url = videoUrl,
        title = this.title,
        coverUrl = coverUrl.ifBlank { null },
        coverWidth = coverWidth,
        coverHeight = coverHeight,
        contentKey = this.contentKey,
        subtitle = this.subtitle,
    )

    /** 打开详情时按业务内容标识决定接管当前悬浮窗，或结束旧的播放。 */
    private fun resolveOverlayForDetail() {
        shouldRestoreOverlay = false
        val overlayMedia = VideoSystemOverlay.currentMediaOrNull() ?: return
        shouldRestoreOverlay = overlayMedia.contentKey == newsId
        if (!shouldRestoreOverlay) VideoSystemOverlay.close()
    }

    /** 测试详情页可在不改变真实详情构建逻辑的前提下追加或替换页面视频。 */
    protected open fun transformPageItems(items: List<NewsDetailPageItem>): List<NewsDetailPageItem> = items

    private fun updateToolbarSource() {
        val shouldShow = newsSource.isNotBlank() &&
            binding.contentRecycler.computeVerticalScrollOffset() >= binding.toolbar.height
        if (shouldShow == isSourceVisible) return

        isSourceVisible = shouldShow
        if (shouldShow) {
            binding.toolbar.setTitleGravity(TitleGravity.START)
            binding.toolbar.setTitle(newsSource)
        } else {
            binding.toolbar.setTitle(null)
            binding.toolbar.setTitleGravity(TitleGravity.CENTER)
        }
    }

    private fun shareNews() {
        val content = listOf(newsTitle, newsSource).filter(String::isNotBlank).joinToString("\n")
        startActivity(
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, content)
                .let { Intent.createChooser(it, getString(R.string.news_detail_share)) },
        )
    }

    /** 提前保存通知点击目标；服务尚未启动时只暂存，不会创建空通知。 */
    private fun setVideoNotificationActivity() {
        if (newsId.isBlank()) return
        VideoPlaybackNotification.setActivity(this, createNotificationPendingIntent())
    }

    private fun createNotificationPendingIntent(): PendingIntent {
        val intent = Intent(this, this::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_NEWS_ID, newsId)
            .putExtra(EXTRA_TITLE, newsTitle)
            .putExtra(EXTRA_SOURCE, newsSource)
            .putExtra(EXTRA_POST_TIME, intent.getStringExtra(EXTRA_POST_TIME).orEmpty())
        // 复用后台已有的详情页。TaskStackBuilder 会清空并重建任务栈，使旧详情页销毁并释放
        // 正在播放的全局 Owner，进而让通知和播放一同消失。
        return PendingIntent.getActivity(
            this,
            newsId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }



    /** 详情页负责横屏覆盖层和方向；video 模块只迁移唯一画面。 */
    private fun showLandscapeVideo(source: EmbeddedVideoPlayerView, media: VideoMedia) {
        showVideoPresentation(source, media, VideoPresentationMode.LANDSCAPE)
    }

    /** 详情页负责竖屏沉浸覆盖层；本次仅播放发起请求的当前视频。 */
    private fun showPortraitVideo(source: EmbeddedVideoPlayerView, media: VideoMedia) {
        showVideoPresentation(source, media, VideoPresentationMode.PORTRAIT)
    }

    private fun showVideoPresentation(
        source: EmbeddedVideoPlayerView,
        media: VideoMedia,
        mode: VideoPresentationMode,
    ) {
        if (presentationMode != null) return

        sourceVideoPlayer = source
        presentationMode = mode
        originalRequestedOrientation = requestedOrientation
        val player = when (mode) {
            VideoPresentationMode.LANDSCAPE -> binding.fullscreenVideoPlayer
            VideoPresentationMode.PORTRAIT -> binding.portraitVideoPlayer
        }
        activePresentationPlayer = player
        player.setMedia(media)
        player.visibility = View.VISIBLE
        binding.fullscreenVideoPlayer.panel.exitAction.visibility =
            if (mode == VideoPresentationMode.LANDSCAPE) View.VISIBLE else View.GONE
        binding.portraitVideoPlayer.panel.exitAction.visibility =
            if (mode == VideoPresentationMode.PORTRAIT) View.VISIBLE else View.GONE
        videoPresentationBackCallback.isEnabled = true
        player.attachRender()
        requestedOrientation = when (mode) {
            VideoPresentationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            VideoPresentationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    /** 将画面迁回仍在窗口内的内嵌播放器，再清理详情页自己的覆盖层。 */
    private fun closeVideoPresentation() {
        if (presentationMode == null) return
        if (sourceVideoPlayer?.isAttachedToWindow == true) {
            sourceVideoPlayer?.attachRender()
        } else {
            activePresentationPlayer?.detachRender()
        }
        binding.fullscreenVideoPlayer.visibility = View.GONE
        binding.portraitVideoPlayer.visibility = View.GONE
        binding.fullscreenVideoPlayer.panel.exitAction.visibility = View.GONE
        binding.portraitVideoPlayer.panel.exitAction.visibility = View.GONE
        sourceVideoPlayer = null
        activePresentationPlayer = null
        presentationMode = null
        videoPresentationBackCallback.isEnabled = false
        requestedOrientation = originalRequestedOrientation
    }

    override fun onDestroy() {
        videoPresentationCoordinator.release()
        closeVideoPresentation()
        VideoPlaybackQueueManager.release(this, videoPlaybackToken)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_NEWS_ID = "newsId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_SOURCE = "source"
        const val EXTRA_POST_TIME = "postTime"
        /** 视频类型选择页已加载的详情；为空时仍按 [EXTRA_NEWS_ID] 请求。 */
        const val EXTRA_DETAIL_JSON = "detailJson"

    }

    private enum class VideoPresentationMode {
        LANDSCAPE,
        PORTRAIT,
    }
}
