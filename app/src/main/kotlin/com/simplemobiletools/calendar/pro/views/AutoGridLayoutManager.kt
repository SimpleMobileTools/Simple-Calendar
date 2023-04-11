package com.simplemobiletools.calendar.pro.views

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max

/**
 * RecyclerView GridLayoutManager but with automatic spanCount calculation
 * @param itemWidth: Grid item width in pixels. Will be used to calculate span count.
 */
class AutoGridLayoutManager(
    context: Context,
    private var itemWidth: Int
) : GridLayoutManager(context, 1) {

    init {
        require(itemWidth >= 0)
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        val width = width
        val height = height
        if (itemWidth > 0 && width > 0 && height > 0) {
            val totalSpace = if (orientation == VERTICAL) {
                width - paddingRight - paddingLeft
            } else {
                height - paddingTop - paddingBottom
            }
            spanCount = max(1, totalSpace / itemWidth)
        }
        super.onLayoutChildren(recycler, state)
    }
}
