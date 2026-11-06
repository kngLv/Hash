package com.hash.video.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.hash.video.api.VideoMedia
import com.hash.video.cache.VideoCacheKeyFactory

/** 将业务无关的视频描述转换为 Media3 播放项。 */
@OptIn(UnstableApi::class)
object VideoMediaItemFactory {

    fun create(media: VideoMedia): MediaItem = MediaItem.Builder()
        .setMediaId(media.id)
        .setUri(media.url)
        .setCustomCacheKey(VideoCacheKeyFactory.create(media))
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(media.title)
                .setArtworkUri(media.coverUrl?.let(Uri::parse))
                .build(),
        )
        .build()
}
