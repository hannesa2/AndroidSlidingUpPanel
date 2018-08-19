package com.sothree.slidinguppanel.canvassaveproxy;

import android.graphics.Canvas;
import android.os.Build;

public class CanvasSaveProxyFactory {

    @SuppressWarnings("deprecation")
    public CanvasSaveProxy create(final Canvas canvas) {

        if (Build.VERSION.SDK_INT >= 28) {
            return new AndroidPCanvasSaveProxy(canvas);
        } else {
            return new LegacyCanvasSaveProxy(canvas);
        }
    }
}
