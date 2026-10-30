package com.hash.home.ui.featured.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.View
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.hash.common.base.activity.BaseBindingActivity
import com.hash.common.ext.showToast
import com.hash.common.ui.loading.LoadingStateOwner
import com.hash.home.R
import com.hash.home.data.model.NewsDetailBean
import com.hash.home.databinding.ActivityNewsDetailBinding
import com.hash.home.ui.featured.detail.adapter.NewsDetailAdapter
import com.hash.home.ui.featured.detail.adapter.NewsDetailPageItem
import com.hash.home.ui.featured.detail.adapter.NewsDetailPageItemFactory
import com.hash.home.ui.featured.detail.vm.NewsDetailViewModel
import com.hash.router.RouterActivityPath

@Route(path = RouterActivityPath.Home.NEWS_DETAIL)
class NewsDetailActivity : BaseBindingActivity<ActivityNewsDetailBinding>() {

    private val viewModel by viewModels<NewsDetailViewModel>()
    private val detailAdapter by lazy {
        NewsDetailAdapter().apply {
            setOnItemClickListener { adapter, _, position ->
                (adapter.getItem(position) as? NewsDetailPageItem.Video)?.let { openVideo(it.videoUrl) }
            }
        }
    }

    override val loadingStateOwner: LoadingStateOwner
        get() = viewModel

    override fun layoutId(): Int = R.layout.activity_news_detail

    override fun initView() = with(binding) {
        contentRecycler.layoutManager = LinearLayoutManager(this@NewsDetailActivity)
        contentRecycler.adapter = detailAdapter
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

    override fun getTitleBar(): View = binding.toolbar

    private fun renderDetail(detail: NewsDetailBean) = with(binding) {
        contentRecycler.visibility = View.VISIBLE
        emptyView.visibility = View.GONE
        detailAdapter.submitList(
            NewsDetailPageItemFactory.create(
                title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                source = intent.getStringExtra(EXTRA_SOURCE).orEmpty(),
                postTime = intent.getStringExtra(EXTRA_POST_TIME).orEmpty(),
                detailItems = detail.items,
            ),
        )
    }

    private fun openVideo(videoUrl: String) {
        val uri = Uri.parse(videoUrl)
        val intent = Intent(Intent.ACTION_VIEW, uri).setDataAndType(uri, "video/*")
        try {
            startActivity(Intent.createChooser(intent, getString(R.string.news_detail_play_video)))
        } catch (_: ActivityNotFoundException) {
            getString(R.string.news_detail_video_unavailable).showToast()
        }
    }

    companion object {
        const val EXTRA_NEWS_ID = "newsId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_SOURCE = "source"
        const val EXTRA_POST_TIME = "postTime"
    }
}
