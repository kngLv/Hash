package com.hash.common.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager


/**
 * Created by KngLv
 * @time 2026/1/4 09:15
 * @description Grid布局间距设置
 *
 * @param spacing     item 间距
 * @param includeEdge item 距屏幕周围是否也有间距
 *
 */
class GridSpaceItemDecoration @JvmOverloads constructor(spacing: Int, includeEdge: Boolean = true) :
    RecyclerView.ItemDecoration() {
    /**
     * 每行个数
     */
    private var mSpanCount = 0

    /**
     * 间距
     */
    private val mSpacing: Int

    /**
     * 距屏幕周围是否也有间距
     */
    private val mIncludeEdge: Boolean

    /**
     * 头部 不显示间距的item个数
     */
    private var mStartFromSize = 0

    /**
     * 尾部 不显示间距的item个数 默认不处理最后一个item的间距
     */
    private var mEndFromSize = 1

    /**
     * 瀑布流 头部第一个整行的position
     */
    private var fullPosition = -1

    /**
     * 横向还是纵向
     */
    private var mOrientation = RecyclerView.VERTICAL

    /**
     * @param spacing     item 间距
     * @param includeEdge item 距屏幕周围是否也有间距
     */
    init {
        this.mSpacing = spacing
        this.mIncludeEdge = includeEdge
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val lastPosition = state.itemCount - 1
        var position = parent.getChildAdapterPosition(view)
        if (mStartFromSize <= position && position <= lastPosition - mEndFromSize) {
            // 行，如果是横向就是列

            var spanGroupIndex = -1
            // 列，如果是横向就是行
            var column = 0
            // 瀑布流是否占满一行
            var fullSpan = false

            val layoutManager = parent.getLayoutManager()
            if (layoutManager is GridLayoutManager) {
                val gridLayoutManager = layoutManager
                val spanSizeLookup = gridLayoutManager.getSpanSizeLookup()
                val spanCount = gridLayoutManager.getSpanCount()
                // 当前position的spanSize
                val spanSize = spanSizeLookup.getSpanSize(position)
                // 横向还是纵向
                mOrientation = gridLayoutManager.getOrientation()
                // 一行几个，如果是横向就是一列几个
                mSpanCount = spanCount / spanSize
                // =0 表示是最左边 0 2 4
                val spanIndex = spanSizeLookup.getSpanIndex(position, spanCount)
                // 列
                column = spanIndex / spanSize
                // 行 减去mStartFromSize,得到从0开始的行
                spanGroupIndex =
                    spanSizeLookup.getSpanGroupIndex(position, spanCount) - mStartFromSize
            } else if (layoutManager is StaggeredGridLayoutManager) {
                // 瀑布流获取列方式不一样
                val staggeredGridLayoutManager = layoutManager
                val params = view.getLayoutParams() as StaggeredGridLayoutManager.LayoutParams
                // 横向还是纵向
                mOrientation = staggeredGridLayoutManager.getOrientation()
                // 列
                column = params.getSpanIndex()
                // 是否是全一行
                fullSpan = params.isFullSpan()
                mSpanCount = layoutManager.getSpanCount()
            }
            // 减掉不设置间距的position,得到从0开始的position
            position = position - mStartFromSize

            if (mIncludeEdge) {
                /*
                 *示例：
                 * spacing = 10 ；spanCount = 3
                 * ---------10--------
                 * 10   3+7   6+4    10
                 * ---------10--------
                 * 10   3+7   6+4    10
                 * ---------10--------
                 */
                if (fullSpan) {
                    outRect.left = 0
                    outRect.right = 0
                } else {
                    if (mOrientation == RecyclerView.VERTICAL) {
                        outRect.left = mSpacing - column * mSpacing / mSpanCount
                        outRect.right = (column + 1) * mSpacing / mSpanCount
                    } else {
                        // 如果是横向对应的是上下
                        outRect.top = mSpacing - column * mSpacing / mSpanCount
                        outRect.bottom = (column + 1) * mSpacing / mSpanCount
                    }
                }

                if (spanGroupIndex > -1) {
                    // grid 显示规则
                    if (spanGroupIndex < 1 && position < mSpanCount) {
                        if (mOrientation == RecyclerView.VERTICAL) {
                            // 第一行才有上间距
                            outRect.top = mSpacing
                        } else {
                            // 第一列才有左间距
                            outRect.left = mSpacing
                        }
                    }
                } else {
                    if (fullPosition == -1 && position < mSpanCount && fullSpan) {
                        // 找到头部第一个整行的position，后面的上间距都不显示
                        fullPosition = position
                    }
                    // Stagger显示规则 头部没有整行或者头部体验整行但是在之前的position显示上间距
                    val isFirstLineStagger =
                        (fullPosition == -1 || position < fullPosition) && (position < mSpanCount)
                    if (isFirstLineStagger) {
                        if (mOrientation == RecyclerView.VERTICAL) {
                            // 第一行才有上间距
                            outRect.top = mSpacing
                        } else {
                            outRect.left = mSpacing
                        }
                    }
                }
                if (mOrientation == RecyclerView.VERTICAL) {
                    outRect.bottom = mSpacing
                } else {
                    outRect.right = mSpacing
                }
            } else {
                /*
                 *示例：
                 * spacing = 10 ；spanCount = 3
                 * --------0--------
                 * 0   3+7   6+4    0
                 * -------10--------
                 * 0   3+7   6+4    0
                 * --------0--------
                 */
                if (fullSpan) {
                    outRect.left = 0
                    outRect.right = 0
                } else {
                    if (mOrientation == RecyclerView.VERTICAL) {
                        outRect.left = column * mSpacing / mSpanCount
                        outRect.right = mSpacing - (column + 1) * mSpacing / mSpanCount
                    } else {
                        outRect.top = column * mSpacing / mSpanCount
                        outRect.bottom = mSpacing - (column + 1) * mSpacing / mSpanCount
                    }
                }

                if (spanGroupIndex > -1) {
                    if (spanGroupIndex >= 1) {
                        if (mOrientation == RecyclerView.VERTICAL) {
                            // 超过第0行都显示上间距
                            outRect.top = mSpacing
                        } else {
                            // 超过第0列都显示左间距
                            outRect.left = mSpacing
                        }
                    }
                } else {
                    if (fullPosition == -1 && position < mSpanCount && fullSpan) {
                        // 找到头部第一个整行的position
                        fullPosition = position
                    }
                    // Stagger上间距显示规则
                    val isStaggerShowTop =
                        position >= mSpanCount || (fullSpan && position != 0) || (fullPosition != -1 && position != 0)

                    if (isStaggerShowTop) {
                        if (mOrientation == RecyclerView.VERTICAL) {
                            // 超过第0行都显示上间距
                            outRect.top = mSpacing
                        } else {
                            // 超过第0列都显示左间距
                            outRect.left = mSpacing
                        }
                    }
                }
            }
        }
    }

    /**
     * 设置从哪个位置 开始设置间距
     *
     * @param startFromSize 一般为HeaderView的个数 + 刷新布局(不一定设置)
     */
    fun setStartFrom(startFromSize: Int): GridSpaceItemDecoration {
        this.mStartFromSize = startFromSize
        return this
    }

    /**
     * 设置从哪个位置 结束设置间距。默认为1，默认用户设置了上拉加载
     *
     * @param endFromSize 一般为FooterView的个数 + 加载更多布局(不一定设置)
     */
    fun setEndFromSize(endFromSize: Int): GridSpaceItemDecoration {
        this.mEndFromSize = endFromSize
        return this
    }

    /**
     * 设置从哪个位置 结束设置间距
     *
     * @param startFromSize 一般为HeaderView的个数 + 刷新布局(不一定设置)
     * @param endFromSize   默认为1，一般为FooterView的个数 + 加载更多布局(不一定设置)
     */
    fun setNoShowSpace(startFromSize: Int, endFromSize: Int): GridSpaceItemDecoration {
        this.mStartFromSize = startFromSize
        this.mEndFromSize = endFromSize
        return this
    }
}