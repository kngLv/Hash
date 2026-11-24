package com.hash.video.api

/**
 * 一次播放会话的媒体顺序快照。
 *
 * 队列只描述播放顺序；运行中的当前索引由播放器引擎维护。构造时复制输入列表，避免业务层
 * 后续修改可变列表而改变正在播放的会话。
 */
class VideoPlaylist(
    items: List<VideoMedia>,
) {

    val items: List<VideoMedia> = items.toList()

    fun indexOf(media: VideoMedia): Int = items.indexOfFirst { it.isSameSourceAs(media) }
}
