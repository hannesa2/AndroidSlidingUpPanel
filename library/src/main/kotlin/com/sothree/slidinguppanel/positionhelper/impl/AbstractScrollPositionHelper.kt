package com.sothree.slidinguppanel.positionhelper.impl

import android.view.View
import com.sothree.slidinguppanel.positionhelper.ScrollPositionHelper

/**
 * help to cast view to supported type
 */
abstract class AbstractScrollPositionHelper<V> : ScrollPositionHelper {

    @Suppress("UNCHECKED_CAST")
    override fun getPosition(view: View, isSlidingUp: Boolean): Int {
        return getPosition(view as V, isSlidingUp)
    }

    abstract fun getPosition(view: V, isSlidingUp: Boolean): Int
}