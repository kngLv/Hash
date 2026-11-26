package com.hash.video.api

/** 与业务模型解耦的视频播放输入。 */
data class VideoMedia(
    val id: String,
    val url: String,
    val title: String = "",
    val coverUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
    /** 封面的原始像素尺寸；存在时直接决定内嵌播放器比例，无需等待图片加载。 */
    val coverWidth: Int? = null,
    val coverHeight: Int? = null,
    /** 所属业务内容的透传标识；视频模块不参与解释或使用。 */
    val contentKey: String? = null,
    /** 媒体的补充描述，可用于系统媒体通知的第二行。 */
    val subtitle: String = "",
) {
    /** 宽高均为有效正数时，表示业务已明确指定封面比例。 */
    fun hasValidCoverSize(): Boolean = (coverWidth ?: 0) > 0 && (coverHeight ?: 0) > 0

    /** 业务传入的封面比例；未传或尺寸非法时返回空。 */
    fun coverAspectRatioOrNull(): Float? = if (hasValidCoverSize()) {
        coverWidth!!.toFloat() / coverHeight!!
    } else {
        null
    }
}

/** 标题和封面只影响展示；播放源由业务标识、地址和请求头共同确定。 */
internal fun VideoMedia.isSameSourceAs(other: VideoMedia?): Boolean =
    other != null && id == other.id && url == other.url && headers == other.headers
