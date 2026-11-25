package com.hash.home.ui.featured.detail

import android.content.Intent
import androidx.activity.viewModels
import com.alibaba.android.arouter.facade.annotation.Route
import com.hash.common.base.activity.BaseBindingActivity
import com.hash.common.ext.toJson
import com.hash.common.ui.insets.applyTopBarInsets
import com.hash.home.R
import com.hash.home.data.model.NewsDetailBean
import com.hash.home.databinding.ActivityNewsVideoTypeBinding
import com.hash.home.ui.featured.detail.vm.NewsDetailViewModel
import com.hash.router.RouterActivityPath

/** 视频分类新闻的测试场景入口；只负责加载一次详情并把数据交给目标页。 */
@Route(path = RouterActivityPath.Home.NEWS_VIDEO_TYPE)
class NewsVideoTypeActivity : BaseBindingActivity<ActivityNewsVideoTypeBinding>() {

    private val viewModel by viewModels<NewsDetailViewModel>()
    private val newsId get() = intent.getStringExtra(NewsDetailActivity.EXTRA_NEWS_ID).orEmpty()
    private var loadedDetail: NewsDetailBean? = null

    override fun layoutId(): Int = R.layout.activity_news_video_type

    override fun initView() = with(binding) {
        toolbar.applyTopBarInsets()
        toolbar.setTitle("视频播放场景")
        descriptionView.text = "正在加载新闻详情…"
        defaultDetailButton.setOnClickListener { openDetail(NewsVideoScenario.DEFAULT) }
        singleVideoButton.setOnClickListener { openDetail(NewsVideoScenario.SINGLE) }
        episodeVideoButton.setOnClickListener { openDetail(NewsVideoScenario.EPISODE) }
        listVideoButton.setOnClickListener { openDetail(NewsVideoScenario.LIST) }
        problemVideoButton.setOnClickListener { openDetail(NewsVideoScenario.PROBLEM_LIST) }
    }

    override fun loadData() {
        viewModel.load(newsId)
    }

    override fun observer() {
        viewModel.detail.observe(this) { detail ->
            loadedDetail = detail
            binding.descriptionView.text = "已加载网络详情，请选择要验证的播放场景。"
            binding.defaultDetailButton.isEnabled = true
            binding.singleVideoButton.isEnabled = true
            binding.episodeVideoButton.isEnabled = true
            binding.listVideoButton.isEnabled = true
            binding.problemVideoButton.isEnabled = true
        }
        viewModel.errorMessage.observe(this) { message -> binding.descriptionView.text = message }
    }

    private fun openDetail(scenario: NewsVideoScenario) {
        val detailJson = loadedDetail?.toJson() ?: return
        startActivity(
            Intent(this, scenario.targetActivity).apply {
                intent.extras?.let(::putExtras)
                putExtra(NewsDetailActivity.EXTRA_DETAIL_JSON, detailJson)
                if (scenario.useProblemMedia) {
                    putExtra(NewsVideoListDetailActivity.EXTRA_USE_PROBLEM_MEDIA, true)
                }
            },
        )
    }
}

/** 场景入口与承载 Activity 的映射，避免选择页了解页面实现细节。 */
private enum class NewsVideoScenario(
    val targetActivity: Class<out android.app.Activity>,
    val useProblemMedia: Boolean = false,
) {
    DEFAULT(NewsDetailActivity::class.java),
    SINGLE(NewsVideoSingleDetailActivity::class.java),
    EPISODE(NewsVideoEpisodeDetailActivity::class.java),
    LIST(NewsVideoListDetailActivity::class.java),
    PROBLEM_LIST(NewsVideoListDetailActivity::class.java, useProblemMedia = true),
}
