package com.sothree.slidinguppanel.canvasSaveProxy

import android.graphics.Canvas
import android.os.Build

class CanvasSaveProxyFactory {
    fun create(canvas: Canvas): CanvasSaveProxy {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            AndroidPCanvasSaveProxy(canvas)
        } else {
            LegacyCanvasSaveProxy(canvas)
        }
    }
}
