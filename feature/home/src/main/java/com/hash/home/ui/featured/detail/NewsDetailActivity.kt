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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alibaba.android.arouter.facade.annotation.Route
import com.hash.common.R as CommonR
import com.hash.common.base.activity.BaseBindingActivity
import com.hash.common.ext.showToast
import com.hash.common.ui.loading.LoadingStateOwner
import com.hash.common.ui.insets.applyScrollContentInsets
import com.hash.common.ui.insets.applyTopBarInsets
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
import com.hash.video.ui.player.EmbeddedVideoPlayerView
import com.hash.video.ui.player.BaseHashVideoPlayerView

@Route(path = RouterActivityPath.Home.NEWS_DETAIL)
class NewsDetailActivity : BaseBindingActivity<ActivityNewsDetailBinding>() {

    private val viewModel by viewModels<NewsDetailViewModel>()
    private val newsId get() = intent.getStringExtra(EXTRA_NEWS_ID).orEmpty()
    private val newsTitle get() = intent.getStringExtra(EXTRA_TITLE).orEmpty()
    private val newsSource get() = intent.getStringExtra(EXTRA_SOURCE).orEmpty()
    private val videoPlaybackOwner = VideoPlaybackOwner()
    private val detailAdapter by lazy {
        NewsDetailAdapter(
            videoPlaybackOwner,
            ::showLandscapeVideo,
            ::showPortraitVideo,
            ::setVideoNotificationActivity,
        )
    }
    private var isSourceVisible = false
    private var sourceVideoPlayer: EmbeddedVideoPlayerView? = null
    private var activePresentationPlayer: BaseHashVideoPlayerView? = null
    private var presentationMode: VideoPresentationMode? = null
    private var originalRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
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
        fullscreenVideoPlayer.onExitFullscreenRequested = ::closeVideoPresentation
        portraitVideoPlayer.onExitPortraitRequested = ::closeVideoPresentation
        fullscreenVideoPlayer.playbackOwner = videoPlaybackOwner
        portraitVideoPlayer.playbackOwner = videoPlaybackOwner
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
        detailAdapter.submitList(pageItems)
    }

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

    /** 仅在用户实际播放详情视频前，注册可恢复当前新闻的通知点击目标。 */
    private fun setVideoNotificationActivity() {
        if (newsId.isBlank()) return
        binding.fullscreenVideoPlayer.setNotificationActivity(createNotificationPendingIntent())
    }

    private fun createNotificationPendingIntent(): PendingIntent {
        val intent = Intent(this, NewsDetailActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_NEWS_ID, newsId)
            .putExtra(EXTRA_TITLE, newsTitle)
            .putExtra(EXTRA_SOURCE, newsSource)
            .putExtra(EXTRA_POST_TIME, intent.getStringExtra(EXTRA_POST_TIME).orEmpty())
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
        sourceVideoPlayer = null
        activePresentationPlayer = null
        presentationMode = null
        videoPresentationBackCallback.isEnabled = false
        requestedOrientation = originalRequestedOrientation
    }

    override fun onDestroy() {
        closeVideoPresentation()
        videoPlaybackOwner.release(this)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_NEWS_ID = "newsId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_SOURCE = "source"
        const val EXTRA_POST_TIME = "postTime"
    }

    private enum class VideoPresentationMode {
        LANDSCAPE,
        PORTRAIT,
    }
}
