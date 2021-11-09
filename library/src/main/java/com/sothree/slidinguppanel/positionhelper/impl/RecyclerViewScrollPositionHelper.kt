package com.sothree.slidinguppanel.positionhelper.impl

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.sothree.slidinguppanel.positionhelper.ScrollPositionHelper

open class RecyclerViewScrollPositionHelper: AbstractScrollPositionHelper<RecyclerView>() {
    override fun isSupport(view: View): Boolean {
        return view is RecyclerView && view.childCount > 0
    }

    override fun getPosition(view: RecyclerView, isSlidingUp: Boolean): Int {
        val lm = view.layoutManager
        return when {
            view.adapter == null -> return 0
            isSlidingUp -> {
                val firstChild = view.getChildAt(0)
                // Approximate the scroll position based on the top child and the first visible item
                view.getChildLayoutPosition(firstChild) * lm!!.getDecoratedMeasuredHeight(firstChild) - lm.getDecoratedTop(firstChild)
            }
            else -> {
                val lastChild = view.getChildAt(view.childCount - 1)
                // Approximate the scroll position based on the bottom child and the last visible item
                (view.adapter!!.itemCount - 1) * lm!!.getDecoratedMeasuredHeight(lastChild) + lm.getDecoratedBottom(lastChild) - view.bottom
            }
        }
    }
}