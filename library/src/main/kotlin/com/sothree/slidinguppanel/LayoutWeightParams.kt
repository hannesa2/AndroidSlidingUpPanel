package com.sothree.slidinguppanel

import android.content.Context
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewGroup
import android.util.AttributeSet

class LayoutWeightParams : MarginLayoutParams {
    var weight = 0f

    constructor() : super(MATCH_PARENT, MATCH_PARENT)
    constructor(source: ViewGroup.LayoutParams?) : super(source)
    constructor(source: MarginLayoutParams?) : super(source)
    constructor(c: Context, attrs: AttributeSet?) : super(c, attrs) {
        val ta = c.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.layout_weight))
        try {
            weight = ta.getFloat(0, 0f)
        } finally {
            ta.recycle()
        }
    }
}
