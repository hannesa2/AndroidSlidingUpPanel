package com.sothree.slidinguppanel.positionhelper

import android.view.View

interface ScrollPositionHelper {

    /**
     * is support to calculate scroll position on this view?
     */
    fun isSupport(view: View): Boolean

    /**
     * calculate scroll position
     */
    fun getPosition(view: View, isSlidingUp: Boolean): Int
}