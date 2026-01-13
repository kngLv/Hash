package com.hash.home.home.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter4.BaseQuickAdapter
import com.hash.bean.home.NewsListBean
import com.hash.common.utils.image.ImageLoader
import com.hash.common.utils.image.LoadOptions
import com.hash.home.databinding.ItemFeatureTabBinding

/**
 * 创建者: KngLv
 * 时间: 2026/1/8 09:40
 * 描述: Featured 列表的 Adapter
 */

class FeaturedTabAdapter :
    BaseQuickAdapter<NewsListBean.NewsListBeanItem, FeaturedTabAdapter.NewsListVH>(
        config = ASYNC_DIFF_CONFIG
    ) {

    // Add Diff callback and AsyncDifferConfig for BaseQuickAdapter
    companion object {
        private val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<NewsListBean.NewsListBeanItem>() {
                // 判断两条数据是否表示同一条新闻：使用唯一标识 newsId 进行比较
                override fun areItemsTheSame(
                    oldItem: NewsListBean.NewsListBeanItem,
                    newItem: NewsListBean.NewsListBeanItem
                ): Boolean = oldItem.newsId == newItem.newsId

                // 判断两条数据的内容是否完全相同：比较整个数据类（所有字段相等则视为内容相同）
                override fun areContentsTheSame(
                    oldItem: NewsListBean.NewsListBeanItem,
                    newItem: NewsListBean.NewsListBeanItem
                ): Boolean = oldItem == newItem
            }

        private val ASYNC_DIFF_CONFIG: AsyncDifferConfig<NewsListBean.NewsListBeanItem> =
            AsyncDifferConfig.Builder<NewsListBean.NewsListBeanItem>(DIFF_CALLBACK).build()
    }

    class NewsListVH(
        parent: ViewGroup,
        val binding: ItemFeatureTabBinding = ItemFeatureTabBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ),
    ) : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(
        context: android.content.Context,
        parent: ViewGroup,
        viewType: Int
    ): NewsListVH = NewsListVH(parent)

    override fun onBindViewHolder(
        holder: NewsListVH,
        position: Int,
        item: NewsListBean.NewsListBeanItem?
    ) {
        item ?: return
        with(holder.binding) {
            this.item = item
            // 图片处理：最多显示前 3 张图片
            val imgs = item.imgList
            val size = imgs.size

            // 重置可见性
            ivImg1.visibility = if (size >= 1) android.view.View.VISIBLE else android.view.View.GONE
            ivImg2.visibility = if (size >= 2) android.view.View.VISIBLE else android.view.View.GONE
            ivImg3.visibility = if (size >= 3) android.view.View.VISIBLE else android.view.View.GONE

            // 布局中已将前 3 张图的 tag 预设为 url，可直接用项目的图片加载器按 tag 加载
            ivImg1.tag = if (size >= 1) imgs[0] else null
            ivImg2.tag = if (size >= 2) imgs[1] else null
            ivImg3.tag = if (size >= 3) imgs[2] else null

            // 调整尺寸：若只有 1 张图片，则使用全宽更高的样式
            if (size == 1) {
                // iv_img1 全宽：将其 LayoutParams 设置为 match_parent
                val params = ivImg1.layoutParams
                params.width = ViewGroup.LayoutParams.MATCH_PARENT
                params.height =
                    root.resources.getDimensionPixelSize(com.hash.home.R.dimen.img_single_height)
                ivImg1.layoutParams = params

                // 隐藏其余图片视图
                ivImg2.visibility = android.view.View.GONE
                ivImg3.visibility = android.view.View.GONE

                // 使用统一 14dp 圆角加载单张图片
                loadImage(ivImg1, imgs.getOrNull(0), uniformRadiusDp = 14, centerCrop = true)
            } else {
                // 多图：等分宽度并使用 multiHeight
                val multiHeight =
                    root.resources.getDimensionPixelSize(com.hash.home.R.dimen.img_multi_height)

                // iv_img1
                val p1 = ivImg1.layoutParams
                p1.width = 0
                p1.height = multiHeight
                ivImg1.layoutParams = p1

                // iv_img2
                val p2 = ivImg2.layoutParams
                p2.width = 0
                p2.height = multiHeight
                ivImg2.layoutParams = p2

                // iv_img3
                val p3 = ivImg3.layoutParams
                p3.width = 0
                p3.height = multiHeight
                ivImg3.layoutParams = p3

                // 按图片数量应用不同的圆角规则并加载图片
                when (size) {
                    2 -> {
                        // 第一张：左侧圆角
                        loadImage(
                            ivImg1, imgs.getOrNull(0), tlDp = 14, blDp = 14, centerCrop = true
                        )
                        // 第二张：右侧圆角
                        loadImage(
                            ivImg2, imgs.getOrNull(1), trDp = 14, brDp = 14, centerCrop = true
                        )
                    }

                    else -> {
                        // size >= 3
                        // 第一张：左侧圆角
                        loadImage(
                            ivImg1, imgs.getOrNull(0), tlDp = 14, blDp = 14, centerCrop = true
                        )
                        // 第二张：无圆角
                        loadImage(ivImg2, imgs.getOrNull(1), centerCrop = true)
                        // 第三张：右侧圆角
                        loadImage(
                            ivImg3, imgs.getOrNull(2), trDp = 14, brDp = 14, centerCrop = true
                        )
                    }
                }
            }


            executePendingBindings()
        }
    }

    // 辅助方法：统一构建 LoadOptions 并调用 ImageLoader.load
    private fun loadImage(
        imageView: ImageView,
        url: String?,
        tlDp: Int = 0,
        trDp: Int = 0,
        brDp: Int = 0,
        blDp: Int = 0,
        uniformRadiusDp: Int? = null,
        centerCrop: Boolean = false
    ) {
        val builder = LoadOptions.Builder()
        uniformRadiusDp?.let { builder.roundedRadiusDp(it) }
        // 若未提供统一圆角但任一角有值，则使用分角圆角配置
        if (uniformRadiusDp == null && (tlDp != 0 || trDp != 0 || brDp != 0 || blDp != 0)) {
            builder.roundedCorners(tlDp, trDp, brDp, blDp)
        }
        if (centerCrop) builder.centerCrop()
        val opts = builder.build()
        ImageLoader.load(imageView, url, opts)
    }


}