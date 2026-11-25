package com.hash.home.ui.featured.detail

import com.hash.home.ui.featured.detail.adapter.NewsDetailPageItem

/** 视频测试页专用的公开媒体源；真实详情页不引用这份数据。 */
internal object NewsVideoTestMedia {
    val items = listOf(
        testVideo("https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4"),
        testVideo("https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4"),
        testVideo("https://uploads.video-commander.com/sample/BigBuckBunny.mp4"),
        testVideo("https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8"),
        testVideo("https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8"),
        testVideo("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
    )

    private fun testVideo(url: String) = NewsDetailPageItem.Video(
        coverUrl = "",
        videoUrl = url,
    )
}
