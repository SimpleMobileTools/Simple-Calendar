package com.simplemobiletools.calendar.pro.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ScrollView

class MyScrollView : ScrollView {
    var isScrollable = true

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    private var scrollViewListener: ScrollViewListener? = null

    fun setOnScrollviewListener(scrollViewListener: ScrollViewListener) {
        this.scrollViewListener = scrollViewListener
    }

    override fun onScrollChanged(x: Int, y: Int, oldx: Int, oldy: Int) {
        super.onScrollChanged(x, y, oldx, oldy)
        scrollViewListener?.onScrollChanged(this, x, y, oldx, oldy)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (isScrollable) {
            super.onTouchEvent(event)
        } else {
            true
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return if (isScrollable) {
            super.onInterceptTouchEvent(event)
        } else {
            false
        }
    }

    interface ScrollViewListener {
        fun onScrollChanged(scrollView: MyScrollView, x: Int, y: Int, oldx: Int, oldy: Int)
    }
}
