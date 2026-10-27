package com.hash.home.data.model

import com.google.gson.annotations.SerializedName

/**
 * @name HomeListBean
 * @package com.hash.home.data.model
 * @author 345 QQ:1831712732
 * @time 2025/01/01 00:51
 * @description
 */
data class HomeListBean(
    @field:SerializedName("curPage") val curPage: Int, // 2
    @field:SerializedName("datas") val datas: List<HomeListItem>,
    @field:SerializedName("offset") val offset: Int, // 20
    @field:SerializedName("over") val over: Boolean, // false
    @field:SerializedName("pageCount") val pageCount: Int, // 797
    @field:SerializedName("size") val size: Int, // 20
    @field:SerializedName("total") val total: Int // 15925
) {
    data class HomeListItem(
        @field:SerializedName("adminAdd") val adminAdd: Boolean, // false
        @field:SerializedName("apkLink") val apkLink: String,
        @field:SerializedName("audit") val audit: Int, // 1
        @field:SerializedName("author") val author: String, // 郭霖
        @field:SerializedName("canEdit") val canEdit: Boolean, // false
        @field:SerializedName("chapterId") val chapterId: Int, // 409
        @field:SerializedName("chapterName") val chapterName: String, // 郭霖
        @field:SerializedName("collect") val collect: Boolean, // false
        @field:SerializedName("courseId") val courseId: Int, // 13
        @field:SerializedName("desc") val desc: String,
        @field:SerializedName("descMd") val descMd: String,
        @field:SerializedName("envelopePic") val envelopePic: String,
        @field:SerializedName("fresh") val fresh: Boolean, // false
        @field:SerializedName("host") val host: String,
        @field:SerializedName("id") val id: Int, // 29333
        @field:SerializedName("isAdminAdd") val isAdminAdd: Boolean, // false
        @field:SerializedName("link") val link: String, // https://mp.weixin.qq.com/s/lWvXKj2BFnDRScU4JbPrhg
        @field:SerializedName("niceDate") val niceDate: String, // 2024-12-20 00:00
        @field:SerializedName("niceShareDate") val niceShareDate: String, // 2024-12-22 15:33
        @field:SerializedName("origin") val origin: String,
        @field:SerializedName("prefix") val prefix: String,
        @field:SerializedName("projectLink") val projectLink: String,
        @field:SerializedName("publishTime") val publishTime: Long, // 1734624000000
        @field:SerializedName("realSuperChapterId") val realSuperChapterId: Int, // 407
        @field:SerializedName("selfVisible") val selfVisible: Int, // 0
        @field:SerializedName("shareDate") val shareDate: Long, // 1734852817000
        @field:SerializedName("shareUser") val shareUser: String,
        @field:SerializedName("superChapterId") val superChapterId: Int, // 408
        @field:SerializedName("superChapterName") val superChapterName: String, // 公众号
        @field:SerializedName("tags") val tags: List<Tag>,
        @field:SerializedName("title") val title: String, // 从源码到定制：全面解析 Android 开机动画机制
        @field:SerializedName("type") val type: Int, // 0
        @field:SerializedName("userId") val userId: Int, // -1
        @field:SerializedName("visible") val visible: Int, // 1
        @field:SerializedName("zan") val zan: Int // 0
    ) {
        data class Tag(
            @field:SerializedName("name") val name: String, // 公众号
            @field:SerializedName("url") val url: String // /wxarticle/list/409/1
        )
    }
}
