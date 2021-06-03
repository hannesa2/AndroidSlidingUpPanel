package com.sothree.slidinguppanel.canvasSaveProxy

import android.graphics.Canvas

interface CanvasSaveProxy {
    fun save(): Int
    fun isFor(canvas: Canvas): Boolean
}
