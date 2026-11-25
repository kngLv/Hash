package com.hash.home.ui.featured.detail

import com.hash.home.ui.featured.detail.adapter.NewsDetailPageItem

/** 多视频列表场景，在真实详情后追加稳定的调试视频。 */
class NewsVideoListDetailActivity : NewsDetailActivity() {

    override fun transformPageItems(items: List<NewsDetailPageItem>): List<NewsDetailPageItem> =
        items + if (intent.getBooleanExtra(EXTRA_USE_PROBLEM_MEDIA, false)) {
            NewsVideoProblemMedia.items
        } else {
            NewsVideoTestMedia.items
        }

    companion object {
        const val EXTRA_USE_PROBLEM_MEDIA = "useProblemMedia"
    }
}
