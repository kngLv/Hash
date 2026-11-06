package com.hash.home.ui.featured.detail

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.TypedValue
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatTextView
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
import com.hash.home.ui.featured.detail.adapter.NewsDetailContentFormatter
import com.hash.home.ui.featured.detail.adapter.NewsDetailPageItem
import com.hash.home.ui.featured.detail.adapter.NewsDetailPageItemFactory
import com.hash.home.ui.featured.detail.vm.NewsDetailViewModel
import com.hash.common.ui.toolbar.HashToolbar.TitleGravity
import com.hash.router.RouterActivityPath
import com.hash.video.player.HashVideo
import java.util.Locale

@Route(path = RouterActivityPath.Home.NEWS_DETAIL)
class NewsDetailActivity : BaseBindingActivity<ActivityNewsDetailBinding>() {

    private val viewModel by viewModels<NewsDetailViewModel>()
    private val newsTitle get() = intent.getStringExtra(EXTRA_TITLE).orEmpty()
    private val newsSource get() = intent.getStringExtra(EXTRA_SOURCE).orEmpty()
    private val detailAdapter by lazy(::NewsDetailAdapter)
    private var narrationText = ""
    private var textToSpeech: TextToSpeech? = null
    private var isTextToSpeechReady = false
    private var isReading = false
    private var isSourceVisible = false
    private var hasVideoContent = false
    private lateinit var listenAction: AppCompatTextView

    override val loadingStateOwner: LoadingStateOwner
        get() = viewModel

    override fun layoutId(): Int = R.layout.activity_news_detail

    override fun initView() = with(binding) {
        toolbar.applyTopBarInsets()
        contentRecycler.applyScrollContentInsets()
        contentRecycler.layoutManager = LinearLayoutManager(this@NewsDetailActivity)
        contentRecycler.adapter = detailAdapter
        toolbar.setDefaultActionClickListener { shareNews() }
        listenAction = toolbar.createTextAction(
            text = getString(R.string.news_detail_listen),
            onClick = View.OnClickListener { toggleReading() },
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
        contentRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateToolbarSource()
            }
        })
    }

    override fun initParam() {
        viewModel.load(intent.getStringExtra(EXTRA_NEWS_ID).orEmpty())
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
        hasVideoContent = pageItems.any { it is NewsDetailPageItem.Video }
        narrationText = pageItems
            .filterIsInstance<NewsDetailPageItem.Text>()
            .joinToString("\n") { NewsDetailContentFormatter.toPlainText(it.content) }
            .trim()
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

    private fun toggleReading() {
        if (isReading) {
            textToSpeech?.stop()
            updateReadingState(false)
            return
        }
        if (narrationText.isBlank()) {
            getString(R.string.news_detail_no_readable_content).showToast()
            return
        }
        if (textToSpeech == null) {
            initializeTextToSpeech()
        } else if (isTextToSpeechReady) {
            startReading()
        }
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this) { status ->
            val speech = textToSpeech
            if (status != TextToSpeech.SUCCESS || speech == null || speech.setLanguage(Locale.CHINESE) in
                setOf(TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED)
            ) {
                releaseTextToSpeech()
                getString(R.string.news_detail_tts_unavailable).showToast()
                return@TextToSpeech
            }
            isTextToSpeechReady = true
            speech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) = Unit

                override fun onDone(utteranceId: String) {
                    runOnUiThread { updateReadingState(false) }
                }

                @Suppress("OVERRIDE_DEPRECATION")
                override fun onError(utteranceId: String) {
                    runOnUiThread { updateReadingState(false) }
                }

                override fun onError(utteranceId: String, errorCode: Int) {
                    runOnUiThread { updateReadingState(false) }
                }
            })
            startReading()
        }
    }

    private fun startReading() {
        val result = textToSpeech?.speak(
            narrationText,
            TextToSpeech.QUEUE_FLUSH,
            Bundle(),
            TTS_UTTERANCE_ID,
        )
        if (result == TextToSpeech.SUCCESS) {
            updateReadingState(true)
        } else {
            getString(R.string.news_detail_tts_unavailable).showToast()
        }
    }

    private fun updateReadingState(reading: Boolean) {
        isReading = reading
        listenAction.contentDescription = getString(
            if (reading) R.string.news_detail_stop_listening else R.string.news_detail_listen,
        )
        listenAction.isSelected = reading
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

    override fun onDestroy() {
        if (isFinishing && hasVideoContent) {
            HashVideo.controller(this).removeCurrentMedia()
        }
        releaseTextToSpeech()
        super.onDestroy()
    }

    private fun releaseTextToSpeech() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        isTextToSpeechReady = false
        isReading = false
    }

    companion object {
        const val EXTRA_NEWS_ID = "newsId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_SOURCE = "source"
        const val EXTRA_POST_TIME = "postTime"
        const val TTS_UTTERANCE_ID = "news_detail"
    }
}
