package com.hash.home.data.model

import com.google.gson.annotations.SerializedName

/**
 * @name BannerListBean
 * @package com.hash.home.data.model
 * @author 345 QQ:1831712732
 * @time 2025/01/03 23:41
 * @description
 */
class BannerListBean : ArrayList<BannerListBean.BannerListBeanItem>() {
    data class BannerListBeanItem(
        @field:SerializedName("desc") val desc: String, // 我们支持订阅啦~
        @field:SerializedName("id") val id: Int, // 30
        @field:SerializedName("imagePath") val imagePath: String, // https://www.wanandroid.com/blogimgs/42da12d8-de56-4439-b40c-eab66c227a4b.png
        @field:SerializedName("isVisible") val isVisible: Int, // 1
        @field:SerializedName("order") val order: Int, // 2
        @field:SerializedName("title") val title: String, // 我们支持订阅啦~
        @field:SerializedName("type") val type: Int, // 0
        @field:SerializedName("url") val url: String // https://www.wanandroid.com/blog/show/3352
    )
}
