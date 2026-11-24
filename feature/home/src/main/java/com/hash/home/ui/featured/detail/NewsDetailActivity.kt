package com.hash.home.ui.featured.detail

import android.content.Intent
import android.app.PendingIntent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Route
import com.hash.home.BuildConfig
import com.hash.common.R as CommonR
import com.hash.common.base.activity.BaseBindingActivity
import com.hash.common.ext.showToast
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
import com.hash.router.RouterActivityPath
import com.hash.video.api.VideoMedia
import com.hash.video.player.VideoPlaybackOwner
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
class NewsDetailActivity : BaseBindingActivity<ActivityNewsDetailBinding>() {

    private val viewModel by viewModels<NewsDetailViewModel>()
    private val newsId get() = intent.getStringExtra(EXTRA_NEWS_ID).orEmpty()
    private val newsTitle get() = intent.getStringExtra(EXTRA_TITLE).orEmpty()
    private val newsSource get() = intent.getStringExtra(EXTRA_SOURCE).orEmpty()
    private val videoPlaybackOwner = VideoPlaybackOwner()
    private val videoPresentationCoordinator by lazy {
        NewsDetailVideoPresentationCoordinator(
            context = this,
            lifecycleOwner = this,
            recyclerView = binding.contentRecycler,
            adapter = detailAdapter,
            playbackOwner = videoPlaybackOwner,
        )
    }
    private val detailAdapter by lazy {
        NewsDetailAdapter(
            videoPlaybackOwner,
            ::showLandscapeVideo,
            ::showPortraitVideo,
            showVideoNotification = !BuildConfig.DEBUG || DEBUG_ENABLE_VIDEO_NOTIFICATION,
            showVideoNotificationArtwork = !BuildConfig.DEBUG || DEBUG_ENABLE_VIDEO_NOTIFICATION_ARTWORK,
        )
    }
    private var isSourceVisible = false
    private var sourceVideoPlayer: EmbeddedVideoPlayerView? = null
    private var activePresentationPlayer: BaseHashVideoPlayerView? = null
    private var presentationMode: VideoPresentationMode? = null
    private var originalRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    private var detailRenderRequestId = 0L
    private val videoPresentationBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = closeVideoPresentation()
    }

    override val loadingStateOwner: LoadingStateOwner
        get() = viewModel

    override fun layoutId(): Int = R.layout.activity_news_detail

    override fun initView() = with(binding) {
        toolbar.applyTopBarInsets()
        contentRecycler.applyScrollContentInsets()
        contentRecycler.layoutManager = LinearLayoutManager(this@NewsDetailActivity)
        contentRecycler.adapter = detailAdapter
        videoPresentationCoordinator.start()
        toolbar.setDefaultActionClickListener { shareNews() }
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
        fullscreenVideoPlayer.panel.exitAction.setOnClickListener { closeVideoPresentation() }
        portraitVideoPlayer.panel.exitAction.setOnClickListener { closeVideoPresentation() }
        fullscreenVideoPlayer.playbackOwner = videoPlaybackOwner
        portraitVideoPlayer.playbackOwner = videoPlaybackOwner
        setVideoNotificationActivity()
        contentRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateToolbarSource()
            }
        })
        onBackPressedDispatcher.addCallback(this@NewsDetailActivity, videoPresentationBackCallback)
    }

    override fun loadData() {
        viewModel.load(newsId)
    }

    override fun onNewIntent(intent: Intent) {
        val previousNewsId = newsId
        super.onNewIntent(intent)
        // Activity 被通知 PendingIntent 复用时，后续读取的新闻参数必须来自最新 Intent。
        setIntent(intent)
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
        val pageItems = NewsDetailPageItemFactory.create(
                title = newsTitle,
                source = newsSource,
                postTime = intent.getStringExtra(EXTRA_POST_TIME).orEmpty(),
                detailItems = detail.items,
            )
            .toMutableList()
            .apply {
                if (BuildConfig.DEBUG) addAll(DEBUG_TEST_VIDEO_ITEMS)
            }
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
            videoPresentationCoordinator.updatePlaybackQueue(
                items = playbackQueue,
                positions = playlistItems.mapIndexedNotNull { adapterPosition, item ->
                    (item as? NewsDetailPageItem.Video)?.playlistIndex?.let { index ->
                        index to adapterPosition
                    }
                }.toMap(),
            )
            detailAdapter.submitList(playlistItems) {
                videoPresentationCoordinator.onAdapterListCommitted()
            }
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
            if (item is NewsDetailPageItem.Video) item.copy(playlistIndex = nextIndex++) else item
        }
    }

    private fun NewsDetailPageItem.Video.toVideoMedia(): VideoMedia = VideoMedia(
        id = videoUrl,
        url = videoUrl,
        coverUrl = coverUrl.ifBlank { null },
        coverWidth = coverWidth,
        coverHeight = coverHeight,
    )

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
        videoPresentationCoordinator.setNotificationActivity(createNotificationPendingIntent())
    }

    private fun createNotificationPendingIntent(): PendingIntent {
        val intent = Intent(this, NewsDetailActivity::class.java)
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
        videoPlaybackOwner.release(this)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_NEWS_ID = "newsId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_SOURCE = "source"
        const val EXTRA_POST_TIME = "postTime"

        /** 临时用于排查播放后列表滑动卡顿；本轮保留通知，移除通知中的远程封面。 */
        private const val DEBUG_ENABLE_VIDEO_NOTIFICATION = true
        private const val DEBUG_ENABLE_VIDEO_NOTIFICATION_ARTWORK = false

        /** 仅调试包追加，用于验证详情页多个视频的切换、迁移与释放。 */
        private val DEBUG_TEST_VIDEO_ITEMS = listOf(
            NewsDetailPageItem.Video(
                coverUrl = "http://videoimg.ws.126.net/cover/20261112/yrgIujvcs_cover.jpg",
                videoUrl = "http://flv0.bn.netease.com/2918f670d37bc18170259a7ffdf260df7aeb1419b39ff224873eb5ee382ec02443d6d398ef0d8356ef56fa26898794882592a1f72486c096ee086ab35e0e24b0283a873b7a2ed5103bdeba57ff0388307a7d4578c49f3d2da8296d6ba7c1acd51b06e1a5f5a0ee23938f321f786a6624047eacd1f3580e14.mp4",
            ),
            NewsDetailPageItem.Video(
                coverUrl = "http://videoimg.ws.126.net/cover/20261111/Tn63ejVcF_cover.jpg",
                videoUrl = "http://flv0.bn.netease.com/2918f670d37bc181a0ccda658d0cd6357323580545d52b9db7250001559819ed92bcf5433383346905ab95ca0491a6cd3f2b3d7a787ecf3f9dd8da146fa439623d45803b461887fb4f37ca8937976c831b56667bfd272b3e881b35dfb0580c9e0d309970bb774f09f2794b4fd9c878ddf7e9a3abade3d3dd.mp4",
            ),
            NewsDetailPageItem.Video(
                coverUrl = "http://videoimg.ws.126.net/cover/20261111/bkryRlOPS_cover.jpg",
                videoUrl = "http://flv0.bn.netease.com/2918f670d37bc181fbb95c830762ec279c05742363e88a90136516667c195d7add054d609c2fa5de7111dc0bbe1e492f2693516ee3fc06b5581817b7fdfd4dc1a61ea9c24b62d5b8c34ea3d65d9bf8dcd4cb7d453ebeafcd7bca66095ddbb96a5d8c701c6ffe95ef0cef7d4d05b0261e53258a22816356e7.mp4",
            ),
            NewsDetailPageItem.Video(
                coverUrl = "http://videoimg.ws.126.net/cover/20261110/AcGXIWe1f_cover.jpg",
                videoUrl = "http://flv0.bn.netease.com/2918f670d37bc181c73bee7a77100e52d2518a3e8d93f85ec21fe421bf07b4788c28a779eccbdef2b762bd43068924334f44976f9c1a29c9bdedcfa0ab7b49d7c99b38d45f76249eec949a8287ec7c0b6a6c552102c2b572ba112e99f0f95f2658007f0b189fcb1d9267a8893331603f99c0a35328af51cf.mp4",
            ),
            NewsDetailPageItem.Video(
                coverUrl = "http://videoimg.ws.126.net/cover/20261110/sjKXkI3Pm_cover.jpg",
                videoUrl = "http://flv0.bn.netease.com/8c084e400104322edb93a34bea79e0b373d53af841294ed9e7503474b9adff259f4ff5b78b476498462d0b72ffaade3dcdc15ba722ec686a3f3944369eb730465ead976b454de9aeb2977da25c3f59dd8480f9c59498c1f72e70dcd722e1694c6831404f2c03337314585c9265b87416b809b3ed370b056f.mp4",
            ),
            NewsDetailPageItem.Video(
                coverUrl = "http://videoimg.ws.126.net/cover/20261109/oteqfWPi2_cover.jpg",
                videoUrl = "http://flv0.bn.netease.com/11e0ef4101124a0ce513ea99f83ea7793bd3579b975aaaaad7d53a20e5c220ae83faa730aff83e54bdf2ed12b02eaac35dd3d1f408f83792da2ab16a64eb3b5e9feb3f3e7dcdc35de96854d4c9a2bec89cab84eb151f53c74315ed637898321b03f43a587506d692d12d8eb2fd5ee8599d569a48fb173121.mp4",
            ),
        )
    }

    private enum class VideoPresentationMode {
        LANDSCAPE,
        PORTRAIT,
    }
}
