package com.hash.common.net.api.model.home

/**
 * @name HomeListBean
 * @package com.hash.common.net.api.model.home
 * @author 345 QQ:1831712732
 * @time 2025/01/01 00:51
 * @description
 */
data class HomeListBean(
    val curPage: Int, // 2
    val datas: List<HomeListItem>,
    val offset: Int, // 20
    val over: Boolean, // false
    val pageCount: Int, // 797
    val size: Int, // 20
    val total: Int // 15925
) {
    data class HomeListItem(
        val adminAdd: Boolean, // false
        val apkLink: String,
        val audit: Int, // 1
        val author: String, // 郭霖
        val canEdit: Boolean, // false
        val chapterId: Int, // 409
        val chapterName: String, // 郭霖
        val collect: Boolean, // false
        val courseId: Int, // 13
        val desc: String,
        val descMd: String,
        val envelopePic: String,
        val fresh: Boolean, // false
        val host: String,
        val id: Int, // 29333
        val isAdminAdd: Boolean, // false
        val link: String, // https://mp.weixin.qq.com/s/lWvXKj2BFnDRScU4JbPrhg
        val niceDate: String, // 2024-12-20 00:00
        val niceShareDate: String, // 2024-12-22 15:33
        val origin: String,
        val prefix: String,
        val projectLink: String,
        val publishTime: Long, // 1734624000000
        val realSuperChapterId: Int, // 407
        val selfVisible: Int, // 0
        val shareDate: Long, // 1734852817000
        val shareUser: String,
        val superChapterId: Int, // 408
        val superChapterName: String, // 公众号
        val tags: List<Tag>,
        val title: String, // 从源码到定制：全面解析 Android 开机动画机制
        val type: Int, // 0
        val userId: Int, // -1
        val visible: Int, // 1
        val zan: Int // 0
    ) {
        data class Tag(
            val name: String, // 公众号
            val url: String // /wxarticle/list/409/1
        )
    }
}
