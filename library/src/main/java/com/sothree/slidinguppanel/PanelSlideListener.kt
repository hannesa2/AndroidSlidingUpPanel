package com.sothree.slidinguppanel

import android.view.View

interface PanelSlideListener {
    /**
     * Called when a sliding pane's position changes.
     *
     * @param panel       The child view that was moved
     * @param slideOffset The new offset of this sliding pane within its range, from 0-1
     */
    fun onPanelSlide(panel: View, slideOffset: Float)

    /**
     * Called when a sliding panel state changes
     *
     * @param panel The child view that was slid to an collapsed position
     */
    fun onPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState)
}