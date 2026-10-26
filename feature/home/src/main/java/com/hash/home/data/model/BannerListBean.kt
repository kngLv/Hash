package com.hash.home.data.model

/**
 * @name BannerListBean
 * @package com.hash.home.data.model
 * @author 345 QQ:1831712732
 * @time 2025/01/03 23:41
 * @description
 */
class BannerListBean : ArrayList<BannerListBean.BannerListBeanItem>() {
    data class BannerListBeanItem(
        val desc: String, // 我们支持订阅啦~
        val id: Int, // 30
        val imagePath: String, // https://www.wanandroid.com/blogimgs/42da12d8-de56-4439-b40c-eab66c227a4b.png
        val isVisible: Int, // 1
        val order: Int, // 2
        val title: String, // 我们支持订阅啦~
        val type: Int, // 0
        val url: String // https://www.wanandroid.com/blog/show/3352
    )
}
