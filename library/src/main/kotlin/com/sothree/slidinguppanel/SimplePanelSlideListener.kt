package com.sothree.slidinguppanel

import android.view.View

/**
 * No-op stubs for [PanelSlideListener]. If you only want to implement a subset
 * of the listener methods you can extend this instead of implement the full interface.
 */
class SimplePanelSlideListener : PanelSlideListener {
    override fun onPanelSlide(panel: View, slideOffset: Float) = Unit
    override fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) = Unit
}
