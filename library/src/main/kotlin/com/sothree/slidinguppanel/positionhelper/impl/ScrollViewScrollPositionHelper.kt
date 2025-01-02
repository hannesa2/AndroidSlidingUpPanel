package com.sothree.slidinguppanel.positionhelper.impl

import android.view.View
import android.widget.ScrollView

open class ScrollViewScrollPositionHelper : AbstractScrollPositionHelper<ScrollView>() {
    override fun isSupport(view: View): Boolean {
        return view is ScrollView
    }

    override fun getPosition(view: ScrollView, isSlidingUp: Boolean): Int {
        return if (isSlidingUp) {
            view.scrollY
        } else {
            val child = view.getChildAt(0)
            child.bottom - (view.height + view.scrollY)
        }
    }
}