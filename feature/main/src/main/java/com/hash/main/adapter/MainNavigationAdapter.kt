package com.hash.main.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.hash.main.R

class MainNavigationAdapter(private val menuItem: List<MenuItem>) :
    RecyclerView.Adapter<MainNavigationAdapter.MainNavigationHolder>() {

    interface MainNavigationItemClickListener {
        fun onTabClickPositionListener(position: Int)
    }

    var onTabClickPosition: MainNavigationItemClickListener? = null

    var selectPosition: Int = 0


    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainNavigationHolder {
        val holder = MainNavigationHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.main_navigation_item, parent, false)
        )
        holder.itemView.setOnClickListener {
            selectPosition = holder.layoutPosition
            notifyDataSetChanged()
            onTabClickPosition?.onTabClickPositionListener(selectPosition)
        }
        return holder
    }

    override fun onBindViewHolder(holder: MainNavigationHolder, position: Int) {

        holder.setData(menuItem[position], position)
    }

    override fun getItemCount(): Int = menuItem.size

    inner class MainNavigationHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val iconView by lazy { view.findViewById<AppCompatImageView>(R.id.iv_main_tab_icon) }
        private val release by lazy { view.findViewById<AppCompatImageView>(R.id.iv_main_tab_release) }
        private val titleView by lazy { view.findViewById<AppCompatTextView>(R.id.tv_main_tab_title) }
        fun setData(item: MenuItem, pos: Int) {
            iconView.setImageResource(item.res)
            titleView.text = item.text
            val flag = selectPosition == pos
            iconView.isSelected = flag
            if (pos == 2) {
                release.isVisible = true
                release.setImageResource(R.mipmap.main_tab_release)
            } else {
                release.isVisible = false
            }
            if (flag) {
                setTextColor(titleView, R.color.main_nav_on)
            } else {
                setTextColor(titleView, R.color.main_nav_off)
            }
        }
    }

    private fun setTextColor(textView: AppCompatTextView, colorRes: Int) {
        textView.setTextColor(ResourcesCompat.getColor(textView.resources, colorRes, null))
    }

    class MenuItem(val text: String, val res: Int)
}

