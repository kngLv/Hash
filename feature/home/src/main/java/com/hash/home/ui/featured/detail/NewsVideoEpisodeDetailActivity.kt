package com.hash.home.ui.featured.detail

import android.app.PendingIntent
import android.content.Intent
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.hash.common.base.activity.BaseBindingActivity
import com.hash.common.ui.insets.applyTopBarInsets
import com.hash.home.R
import com.hash.home.databinding.ActivityNewsVideoEpisodeDetailBinding
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackNotification
import com.hash.video.api.VideoPlaybackQueueManager
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** 单播放器选集测试页：队列切换只更新同一个播放器和选集高亮。 */
class NewsVideoEpisodeDetailActivity :
    BaseBindingActivity<ActivityNewsVideoEpisodeDetailBinding>() {
    private val newsId get() = intent.getStringExtra(NewsDetailActivity.EXTRA_NEWS_ID).orEmpty()
    private val episodes by lazy {
        NewsVideoTestMedia.items.map {
            VideoMedia(
                it.videoUrl,
                it.videoUrl,
                it.coverUrl
            )
        }
    }
    private val buttons = mutableListOf<Button>()

    override fun layoutId(): Int = R.layout.activity_news_video_episode_detail

    override fun initView() = with(binding) {
        toolbar.applyTopBarInsets()
        toolbar.setTitle("选集详情")
        videoPlayer.setMedia(episodes.first())
        VideoPlaybackQueueManager.setPlaybackQueue(videoPlayer.playbackToken, episodes)
        VideoPlaybackNotification.setActivity(
            this@NewsVideoEpisodeDetailActivity,
            createNotificationIntent()
        )
        episodes.forEachIndexed { index, _ ->
            Button(this@NewsVideoEpisodeDetailActivity).apply {
                text = "第 ${index + 1} 集"
                layoutParams = ViewGroup.MarginLayoutParams(-2, -2).also { it.marginEnd = 8.dp }
                setOnClickListener {
                    videoPlayer.setMedia(episodes[index])
                    videoPlayer.attachRender()
                    VideoPlaybackQueueManager.playQueueAt(
                        context = this@NewsVideoEpisodeDetailActivity,
                        token = videoPlayer.playbackToken,
                        index = index,
                    )
                }
                buttons += this
                episodeContainer.addView(this)
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                videoPlayer.playbackState
                    .map { it.playlistIndex }
                    .distinctUntilChanged()
                    .collect { index ->
                        index ?: return@collect
                        val media = episodes.getOrNull(index) ?: return@collect
                        videoPlayer.setMedia(media)
                        currentEpisodeView.text = "正在播放：第 ${index + 1} 集"
                        buttons.forEachIndexed { i, button -> button.isEnabled = i != index }
                    }
            }
        }
        Unit
    }

    override fun onDestroy() {
        VideoPlaybackQueueManager.release(this, binding.videoPlayer.playbackToken)
        super.onDestroy()
    }

    private fun createNotificationIntent() = PendingIntent.getActivity(
        this, newsId.hashCode(),
        Intent(this, NewsVideoEpisodeDetailActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtras(intent.extras ?: android.os.Bundle()),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()
}
