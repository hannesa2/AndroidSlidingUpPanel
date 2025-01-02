package com.sothree.slidinguppanel.positionhelper.impl

import android.view.View
import android.widget.ListView

open class ListViewScrollPositionHelper : AbstractScrollPositionHelper<ListView>() {
    override fun isSupport(view: View): Boolean {
        return view is ListView && view.childCount > 0
    }

    override fun getPosition(view: ListView, isSlidingUp: Boolean): Int {
        return when {
            view.adapter == null -> 0
            isSlidingUp -> {
                val firstChild = view.getChildAt(0)
                // Approximate the scroll position based on the top child and the first visible item
                view.firstVisiblePosition * firstChild.height - firstChild.top
            }
            else -> {
                val lastChild = view.getChildAt(view.childCount - 1)
                // Approximate the scroll position based on the bottom child and the last visible item
                (view.adapter.count - view.lastVisiblePosition - 1) * lastChild.height + lastChild.bottom - view.bottom
            }
        }
    }
}