package com.hash.video.api

import kotlin.math.abs

/** 当前媒体的播放与缓冲进度，单位均为毫秒。 */
data class VideoPlaybackProgress(
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val durationMs: Long = 0L,
) {
    /** 时长有效时才允许界面展示确定进度。 */
    val isAvailable: Boolean
        get() = durationMs > 0L

    /** 已播放比例，始终限制在 0 到 1。 */
    val fraction: Float
        get() = positionMs.toFraction()

    /** 已缓冲比例，始终限制在 0 到 1。 */
    val bufferedFraction: Float
        get() = bufferedPositionMs.toFraction()

    /** 是否已进入指定位置附近的允许误差范围。 */
    fun isNear(positionMs: Long, toleranceMs: Long): Boolean =
        toleranceMs >= 0L && abs(this.positionMs - positionMs) <= toleranceMs

    /** 用于继续播放的进度；已播完时从头重新开始。 */
    fun forResume(): VideoPlaybackProgress =
        if (durationMs in 1L..positionMs) {
            copy(positionMs = 0L, bufferedPositionMs = 0L)
        } else {
            this
        }

    /** 用于播放器 seek 的位置，确保不为负数。 */
    fun resumePositionMs(): Long = positionMs.coerceAtLeast(0L)

    private fun Long.toFraction(): Float = if (isAvailable) {
        (toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
}
