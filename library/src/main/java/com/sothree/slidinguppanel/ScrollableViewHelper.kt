package com.sothree.slidinguppanel

import android.view.View
import com.sothree.slidinguppanel.positionhelper.ScrollPositionHelper
import com.sothree.slidinguppanel.positionhelper.impl.ListViewScrollPositionHelper
import com.sothree.slidinguppanel.positionhelper.impl.RecyclerViewScrollPositionHelper
import com.sothree.slidinguppanel.positionhelper.impl.ScrollViewScrollPositionHelper

/**
 * Helper class for determining the current scroll positions for scrollable views. Currently works
 * for ListView, ScrollView and RecyclerView, but the library users can override it to add support
 * for other views.
 */
class ScrollableViewHelper {

    private var positionHelpers: MutableList<ScrollPositionHelper> = mutableListOf(
        ListViewScrollPositionHelper(),
        ScrollViewScrollPositionHelper(),
        RecyclerViewScrollPositionHelper()
    )

    /**
     * Returns the current scroll position of the scrollable view. If this method returns zero or
     * less, it means at the scrollable view is in a position such as the panel should handle
     * scrolling. If the method returns anything above zero, then the panel will let the scrollable
     * view handle the scrolling
     *
     * @param scrollableView the scrollable view
     * @param isSlidingUp    whether or not the panel is sliding up or down
     * @return the scroll position
     */
    fun getScrollableViewScrollPosition(scrollableView: View?, isSlidingUp: Boolean): Int {
        scrollableView?.let {
            for (helper in positionHelpers) {
                if (helper.isSupport(scrollableView)) {
                    helper.getPosition(scrollableView, isSlidingUp)
                }
            }
        }
        return 0
    }

    fun setPositionHelpers(helpers: MutableList<ScrollPositionHelper>) {
        this.positionHelpers = helpers
    }

    fun addPositionHelpers(helper: ScrollPositionHelper) {
        this.positionHelpers.add(helper)
    }
}
