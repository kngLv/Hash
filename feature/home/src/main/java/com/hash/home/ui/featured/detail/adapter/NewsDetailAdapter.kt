package com.hash.home.ui.featured.detail.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter4.BaseMultiItemAdapter
import com.hash.common.utils.image.ImageLoader
import com.hash.common.utils.image.LoadOptions
import com.hash.video.api.VideoMedia
import com.hash.home.databinding.ItemNewsDetailEmptyBinding
import com.hash.home.databinding.ItemNewsDetailHeaderBinding
import com.hash.home.databinding.ItemNewsDetailImageBinding
import com.hash.home.databinding.ItemNewsDetailTextBinding
import com.hash.home.databinding.ItemNewsDetailVideoBinding

/** 展示新闻详情的头部、文字、图片、视频与空内容条目。 */
class NewsDetailAdapter : BaseMultiItemAdapter<NewsDetailPageItem>() {

    init {
        addItemType(TYPE_HEADER, HeaderItemListener())
            .addItemType(TYPE_TEXT, TextItemListener())
            .addItemType(TYPE_IMAGE, ImageItemListener())
            .addItemType(TYPE_VIDEO, VideoItemListener())
            .addItemType(TYPE_EMPTY, EmptyItemListener())
            .onItemViewType { position, list -> itemTypeOf(list[position]) }
    }

    private fun itemTypeOf(item: NewsDetailPageItem): Int = when (item) {
        is NewsDetailPageItem.Header -> TYPE_HEADER
        is NewsDetailPageItem.Text -> TYPE_TEXT
        is NewsDetailPageItem.Image -> TYPE_IMAGE
        is NewsDetailPageItem.Video -> TYPE_VIDEO
        NewsDetailPageItem.Empty -> TYPE_EMPTY
    }

    private class HeaderItemListener : OnMultiItemAdapterListener<NewsDetailPageItem, HeaderHolder> {
        override fun onCreate(context: Context, parent: ViewGroup, viewType: Int) = HeaderHolder(
            ItemNewsDetailHeaderBinding.inflate(LayoutInflater.from(context), parent, false),
        )

        override fun onBind(holder: HeaderHolder, position: Int, item: NewsDetailPageItem?) {
            (item as? NewsDetailPageItem.Header)?.let(holder::bind)
        }
    }

    private class TextItemListener : OnMultiItemAdapterListener<NewsDetailPageItem, TextHolder> {
        override fun onCreate(context: Context, parent: ViewGroup, viewType: Int) = TextHolder(
            ItemNewsDetailTextBinding.inflate(LayoutInflater.from(context), parent, false),
        )

        override fun onBind(holder: TextHolder, position: Int, item: NewsDetailPageItem?) {
            (item as? NewsDetailPageItem.Text)?.let(holder::bind)
        }
    }

    private class ImageItemListener : OnMultiItemAdapterListener<NewsDetailPageItem, ImageHolder> {
        override fun onCreate(context: Context, parent: ViewGroup, viewType: Int) = ImageHolder(
            ItemNewsDetailImageBinding.inflate(LayoutInflater.from(context), parent, false),
        )

        override fun onBind(holder: ImageHolder, position: Int, item: NewsDetailPageItem?) {
            (item as? NewsDetailPageItem.Image)?.let(holder::bind)
        }
    }

    private class VideoItemListener : OnMultiItemAdapterListener<NewsDetailPageItem, VideoHolder> {
        override fun onCreate(context: Context, parent: ViewGroup, viewType: Int) = VideoHolder(
            ItemNewsDetailVideoBinding.inflate(LayoutInflater.from(context), parent, false),
        )

        override fun onBind(holder: VideoHolder, position: Int, item: NewsDetailPageItem?) {
            (item as? NewsDetailPageItem.Video)?.let(holder::bind)
        }
    }

    private class EmptyItemListener : OnMultiItemAdapterListener<NewsDetailPageItem, EmptyHolder> {
        override fun onCreate(context: Context, parent: ViewGroup, viewType: Int) = EmptyHolder(
            ItemNewsDetailEmptyBinding.inflate(LayoutInflater.from(context), parent, false),
        )

        override fun onBind(holder: EmptyHolder, position: Int, item: NewsDetailPageItem?) = Unit
    }

    private class HeaderHolder(
        private val binding: ItemNewsDetailHeaderBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: NewsDetailPageItem.Header) = with(binding) {
            titleView.text = item.title
            metaView.text = item.meta
            metaView.visibility = if (item.meta.isBlank()) View.GONE else View.VISIBLE
        }
    }

    private class TextHolder(
        private val binding: ItemNewsDetailTextBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: NewsDetailPageItem.Text) {
            binding.contentView.text = NewsDetailContentFormatter.format(item.content)
        }
    }

    private class ImageHolder(
        private val binding: ItemNewsDetailImageBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: NewsDetailPageItem.Image) {
            ImageLoader.load(
                binding.imageView,
                item.imageUrl,
                LoadOptions.Builder().roundedRadiusDp(12).build(),
            )
        }
    }

    private class VideoHolder(
        private val binding: ItemNewsDetailVideoBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: NewsDetailPageItem.Video) = with(binding) {
            videoPlayer.setMedia(
                VideoMedia(
                    id = item.videoUrl,
                    url = item.videoUrl,
                    coverUrl = item.coverUrl.ifBlank { null },
                ),
            )
        }
    }

    private class EmptyHolder(binding: ItemNewsDetailEmptyBinding) : RecyclerView.ViewHolder(binding.root)

    private companion object {
        const val TYPE_HEADER = 0
        const val TYPE_TEXT = 1
        const val TYPE_IMAGE = 2
        const val TYPE_VIDEO = 3
        const val TYPE_EMPTY = 4
    }
}
