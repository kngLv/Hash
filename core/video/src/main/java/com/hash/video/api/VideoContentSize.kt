package com.hash.video.api

/**
 * 视频内容的真实像素尺寸。
 *
 * 由播放器从媒体流解析得到，不依赖服务端提前下发宽高；全屏页、小窗和嵌入式播放器都可据此选择
 * 合适的展示比例。
 */
data class VideoContentSize(
    val width: Int,
    val height: Int,
    val pixelWidthHeightRatio: Float = 1f,
) {
    /** 视频显示区域的宽高比，未知或非法尺寸时为 0。 */
    val aspectRatio: Float
        get() = if (width > 0 && height > 0 && pixelWidthHeightRatio > 0f) {
            width.toFloat() * pixelWidthHeightRatio / height
        } else {
            0f
        }

    /** 是否为竖向内容，供全屏或小窗决定展示方向。 */
    val isPortrait: Boolean
        get() = aspectRatio > 0f && aspectRatio < 1f
}
