package com.sothree.slidinguppanel.canvasSaveProxy

import android.graphics.Canvas
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(api = Build.VERSION_CODES.P)
internal class AndroidPCanvasSaveProxy(private var canvas: Canvas) : CanvasSaveProxy {

    init {
        Log.d(TAG, "New AndroidPCanvasSaveProxy")
    }

    override fun save(): Int {
        return canvas.save()
    }

    override fun isFor(canvas: Canvas): Boolean {
        return canvas === this.canvas
    }

    companion object {
        private val TAG = CanvasSaveProxy::class.java.simpleName
    }

}
