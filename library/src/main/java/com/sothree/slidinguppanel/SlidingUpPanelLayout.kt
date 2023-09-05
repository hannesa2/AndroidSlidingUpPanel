package com.sothree.slidinguppanel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.*
import android.view.View.OnClickListener
import android.view.accessibility.AccessibilityEvent
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import com.sothree.slidinguppanel.ViewDragHelper.Companion.create
import com.sothree.slidinguppanel.canvasSaveProxy.CanvasSaveProxy
import com.sothree.slidinguppanel.canvasSaveProxy.CanvasSaveProxyFactory
import com.sothree.slidinguppanel.library.R
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Default peeking out panel height
 */
private const val DEFAULT_PANEL_HEIGHT = 68 // dp;

/**
 * Default anchor point height
 */
private const val DEFAULT_ANCHOR_POINT = 1.0f // In relative %

/**
 * Default maximum sliding offset
 */
private const val DEFAULT_MAX_SLIDING_OFFSET = 1.0f

/**
 * Default initial state for the component
 */
private val DEFAULT_SLIDE_STATE = PanelState.COLLAPSED

/**
 * Default height of the shadow above the peeking out panel
 */
private const val DEFAULT_SHADOW_HEIGHT = 4 // dp;

/**
 * If no fade color is given by default it will fade to 80% gray.
 */
private const val DEFAULT_FADE_COLOR = -0x67000000

/**
 * Default Minimum velocity that will be detected as a fling
 */
private const val DEFAULT_MIN_FLING_VELOCITY = 400 // dips per second

/**
 * Default is set to false because that is how it was written
 */
private const val DEFAULT_OVERLAY_FLAG = false

/**
 * Default is set to true for clip panel for performance reasons
 */
private const val DEFAULT_CLIP_PANEL_FLAG = true

/**
 * Default attributes for layout
 */
private val DEFAULT_ATTRS = intArrayOf(
    android.R.attr.gravity
)

/**
 * Tag for the sliding state stored inside the bundle
 */
const val SLIDING_STATE = "sliding_state"

/**
 * Default parallax length of the main view
 */
private const val DEFAULT_PARALLAX_OFFSET = 0

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class SlidingUpPanelLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) :
    ViewGroup(context, attrs, defStyle) {
    /**
     * Minimum velocity that will be detected as a fling
     */
    var minFlingVelocity = DEFAULT_MIN_FLING_VELOCITY

    /**
     * The ARGB-packed fade color used for the panel covered by the slider. 0 = no fading.
     */
    var coveredFadeColor = DEFAULT_FADE_COLOR
        /**
         * Set the color used to fade the pane covered by the sliding pane out when the pane
         * will become fully covered in the expanded state.
         */
        set(color) {
            field = color
            requestLayout()
        }

    /**
     * The paint used to dim the main layout when sliding
     */
    private val coveredFadePaint = Paint()

    /**
     * Drawable used to draw the shadow between panes.
     */
    private var shadowDrawable: Drawable? = null

    /**
     * The size of the overhang in pixels, when the panel is collapsed
     */
    private var panelHeight = -1

    /**
     * The size of the shadow in pixels.
     */
    private var shadowHeight = -1

    /**
     * Parallax offset
     */
    private var parallaxOffset = -1

    /**
     * True if the collapsed panel should be dragged up.
     */
    private var isSlidingUp = false

    /**
     * Panel overlays the windows instead of putting it underneath it.
     */
    var overlayContent = DEFAULT_OVERLAY_FLAG

    /**
     * Determines whether or not the main content is clipped to the top of the panel (the main view
     * border)
     */
    var clipPanel = DEFAULT_CLIP_PANEL_FLAG

    /**
     * If provided, the panel can be dragged by only this view. Otherwise, the entire panel can be
     * used for dragging.
     */
    private var dragView: View? = null

    /**
     * If provided, the panel can be dragged by only this view. Otherwise, the entire panel can be
     * used for dragging.
     */
    private var dragViewResId = -1

    /**
     * If set, the panel will transfer the scroll from this view to itself when needed.
     */
    private var scrollableView: View? = null
    private var scrollableViewResId = 0
    private var scrollableViewHelper = ScrollableViewHelper()

    /**
     * The child view that can slide, if any.
     */
    var slideableView: View? = null
        private set

    /*
     * The main view
     */
    private var mainView: View? = null

    private var slideState: PanelState = DEFAULT_SLIDE_STATE

    /**
     * Returns the current state of the panel as an enum.
     *
     * @return the current panel state
     */
    fun getPanelState(): PanelState {
        return slideState
    }

    /**
     * Change panel state to the given state with
     *
     * @param state - new panel state
     */
    fun setPanelState(state: PanelState) {
        // Abort any running animation, to allow state change
        if (dragHelper?.viewDragState == ViewDragHelper.STATE_SETTLING) {
            dragHelper?.abort()
        }
        require(state !== PanelState.DRAGGING) { "Panel state can't be DRAGGING during state set" }
        if (!isEnabled ||
            (!firstLayout && (slideableView == null)) ||
            (state === slideState) || (slideState === PanelState.DRAGGING)
        ) return
        if (firstLayout) {
            setPanelStateInternal(state)
        } else {
            if (slideState === PanelState.HIDDEN) {
                slideableView!!.visibility = VISIBLE
                requestLayout()
            }
            when (state) {
                PanelState.ANCHORED -> smoothSlideTo(anchorPoint, 0)
                PanelState.COLLAPSED -> smoothSlideTo(0f, 0)
                PanelState.EXPANDED -> smoothSlideTo(maxSlideOffset, 0)
                PanelState.HIDDEN -> {
                    val newTop =
                        computePanelTopPosition(0.0f) + if (isSlidingUp) +panelHeight else -panelHeight
                    smoothSlideTo(computeSlideOffset(newTop), 0)
                }

                PanelState.DRAGGING -> Unit
            }
        }
    }

    /**
     * If the current slide state is DRAGGING, this will store the last non dragging state
     */
    private var lastNotDraggingSlideState: PanelState = DEFAULT_SLIDE_STATE

    /**
     * How far the panel is offset from its expanded position.
     * range [0, 1] where 0 = collapsed, 1 = expanded.
     */
    private var slideOffset = 0f

    /**
     * How far in pixels the slideable panel may move.
     */
    private var slideRange = 0

    /**
     * Maximum sliding panel movement in expanded state
     */
    private var maxSlideOffset = DEFAULT_MAX_SLIDING_OFFSET

    /**
     * An anchor point where the panel can stop during sliding
     */
    var anchorPoint = 1f
        /**
         * Set an anchor point where the panel can stop during sliding
         *
         * @param value A value between 0 and 1, determining the position of the anchor point
         * starting from the top of the layout.
         */
        set(value) = if (value > 0 && value <= 1) {
            field = value
            firstLayout = true
            requestLayout()
        } else {
            field = value
        }

    /**
     * A panel view is locked into internal scrolling or another condition that
     * is preventing a drag.
     */
    private var isUnableToDrag = false

    /**
     * Flag indicating that sliding feature is enabled\disabled
     */
    private var touchEnabled: Boolean = true

    /**
     * Shadow style which will replace default if provided
     */
    private var aboveShadowResId = 0
    private var belowShadowResId = 0
    private var prevMotionX = 0f
    private var prevMotionY = 0f
    private var initialMotionX = 0f
    private var initialMotionY = 0f
    private var isScrollableViewHandlingTouch = false
    private val panelSlideListeners: MutableList<PanelSlideListener> = CopyOnWriteArrayList()
    private var fadeOnClickListener: OnClickListener? = null
    private var dragHelper: ViewDragHelper?
    private val canvasSaveProxyFactory: CanvasSaveProxyFactory = CanvasSaveProxyFactory()
    private var canvasSaveProxy: CanvasSaveProxy? = null

    /**
     * Stores whether or not the pane was expanded the last time it was slideable.
     * If expand/collapse operations are invoked this state is modified. Used by
     * instance state save/restore.
     */
    private var firstLayout = true
    private val tmpRect = Rect()

    init {
        if (isInEditMode) {
            shadowDrawable = null
            dragHelper = null
        } else {
            var scrollerInterpolator: Interpolator? = null
            if (attrs != null) {
                scrollerInterpolator = initAttributes(context, attrs)
            }
            val density = context.resources.displayMetrics.density
            if (panelHeight == -1) {
                panelHeight = (DEFAULT_PANEL_HEIGHT * density + 0.5f).toInt()
            }
            if (shadowHeight == -1) {
                shadowHeight = (DEFAULT_SHADOW_HEIGHT * density + 0.5f).toInt()
            }
            if (parallaxOffset == -1) {
                parallaxOffset = (DEFAULT_PARALLAX_OFFSET * density).toInt()
            }
            // If the shadow height is zero, don't show the shadow
            shadowDrawable = makeShadowDrawable(context)
            setWillNotDraw(false)
            dragHelper = create(this, 0.5f, scrollerInterpolator, DragHelperCallback())
            dragHelper?.minVelocity = minFlingVelocity * density
            touchEnabled = true
        }
    }

    private fun initAttributes(context: Context, attrs: AttributeSet?): Interpolator? {
        val defAttrs = context.obtainStyledAttributes(attrs, DEFAULT_ATTRS)
        try {
            val gravity = defAttrs.getInt(0, Gravity.NO_GRAVITY)
            setGravity(gravity)
        } finally {
            defAttrs.recycle()
        }
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SlidingUpPanelLayout)
        var scrollerInterpolator: Interpolator? = null
        try {
            panelHeight = ta.getDimensionPixelSize(
                R.styleable.SlidingUpPanelLayout_umanoPanelHeight,
                -1
            )
            shadowHeight = ta.getDimensionPixelSize(
                R.styleable.SlidingUpPanelLayout_umanoShadowHeight,
                -1
            )
            parallaxOffset = ta.getDimensionPixelSize(
                R.styleable.SlidingUpPanelLayout_umanoParallaxOffset,
                -1
            )
            minFlingVelocity = ta.getInt(
                R.styleable.SlidingUpPanelLayout_umanoFlingVelocity,
                DEFAULT_MIN_FLING_VELOCITY
            )
            coveredFadeColor = ta.getColor(
                R.styleable.SlidingUpPanelLayout_umanoFadeColor,
                DEFAULT_FADE_COLOR
            )
            dragViewResId =
                ta.getResourceId(R.styleable.SlidingUpPanelLayout_umanoDragView, -1)
            scrollableViewResId =
                ta.getResourceId(R.styleable.SlidingUpPanelLayout_umanoScrollableView, -1)
            aboveShadowResId =
                ta.getResourceId(R.styleable.SlidingUpPanelLayout_umanoAboveShadowStyle, -1)
            belowShadowResId =
                ta.getResourceId(R.styleable.SlidingUpPanelLayout_umanoBelowShadowStyle, -1)
            overlayContent = ta.getBoolean(
                R.styleable.SlidingUpPanelLayout_umanoOverlay,
                DEFAULT_OVERLAY_FLAG
            )
            clipPanel = ta.getBoolean(
                R.styleable.SlidingUpPanelLayout_umanoClipPanel,
                DEFAULT_CLIP_PANEL_FLAG
            )
            anchorPoint = ta.getFloat(
                R.styleable.SlidingUpPanelLayout_umanoAnchorPoint,
                DEFAULT_ANCHOR_POINT
            )
            maxSlideOffset = ta.getFloat(
                R.styleable.SlidingUpPanelLayout_umanoMaxSlidingOffset,
                DEFAULT_MAX_SLIDING_OFFSET
            )
            slideState = PanelState.values()[ta.getInt(
                R.styleable.SlidingUpPanelLayout_umanoInitialState,
                DEFAULT_SLIDE_STATE.ordinal
            )]
            val interpolatorResId = ta.getResourceId(
                R.styleable.SlidingUpPanelLayout_umanoScrollInterpolator,
                -1
            )
            if (interpolatorResId != -1) {
                scrollerInterpolator =
                    AnimationUtils.loadInterpolator(context, interpolatorResId)
            }
        } finally {
            ta.recycle()
        }
        return scrollerInterpolator
    }

    private fun makeShadowDrawable(context: Context) = if (shadowHeight > 0) {
        if (isSlidingUp) {
            if (aboveShadowResId == -1) {
                ContextCompat.getDrawable(context, R.drawable.above_shadow)
            } else {
                ContextCompat.getDrawable(context, aboveShadowResId)
            }
        } else {
            if (belowShadowResId == -1) {
                ContextCompat.getDrawable(context, R.drawable.below_shadow)
            } else {
                ContextCompat.getDrawable(context, belowShadowResId)
            }
        }
    } else {
        null
    }

    /**
     * Set the Drag View after the view is inflated
     */
    override fun onFinishInflate() {
        super.onFinishInflate()
        if (dragViewResId != -1) {
            setDragView(findViewById(dragViewResId))
        }
        if (scrollableViewResId != -1) {
            scrollableView = findViewById(scrollableViewResId)
        }
    }

    fun setGravity(gravity: Int) {
        require(!(gravity != Gravity.TOP && gravity != Gravity.BOTTOM)) { "gravity must be set to either top or bottom" }
        isSlidingUp = gravity == Gravity.BOTTOM
        if (!firstLayout) {
            requestLayout()
        }
    }

    /**
     * Set sliding enabled flag
     */
    var isTouchEnabled: Boolean
        get() = touchEnabled && slideableView != null && slideState !== PanelState.HIDDEN
        set(enabled) {
            touchEnabled = enabled
        }

    protected fun smoothToBottom(): Boolean {
        return smoothSlideTo(0f, 0)
    }

    /**
     * @return The current shadow height
     */
    fun getShadowHeight(): Int {
        return shadowHeight
    }

    /**
     * Set the shadow height
     *
     * @param val A height in pixels
     */
    fun setShadowHeight(`val`: Int) {
        shadowHeight = `val`
        if (!firstLayout) {
            invalidate()
        }
    }

    /**
     * @return The current parallax offset
     */
    fun getCurrentParallaxOffset(): Int {
        // Clamp slide offset at zero for parallax computation;
        val offset = (parallaxOffset * max(slideOffset, 0f)).toInt()
        return if (isSlidingUp) -offset else offset
    }

    /**
     * Set parallax offset for the panel
     *
     * @param val A height in pixels
     */
    fun setParallaxOffset(`val`: Int) {
        parallaxOffset = `val`
        if (!firstLayout) {
            requestLayout()
        }
    }

    fun addPanelSlideListener(listener: PanelSlideListener) {
        // TODO why do we synchronize again, this is already a costly CopyOnWriteArrayList? Maybe should use Kotlin Collections.synchronizedList() wrapper instead?
        synchronized(panelSlideListeners) { panelSlideListeners.add(listener) }
    }

    fun removePanelSlideListener(listener: PanelSlideListener) {
        synchronized(panelSlideListeners) { panelSlideListeners.remove(listener) }
    }

    /**
     * Provides an on click for the portion of the main view that is dimmed. The listener is not
     * triggered if the panel is in a collapsed or a hidden position. If the on click listener is
     * not provided, the clicks on the dimmed area are passed through to the main layout.
     */
    fun setFadeOnClickListener(listener: OnClickListener?) {
        fadeOnClickListener = listener
    }

    /**
     * Set the draggable view portion. Use to null, to allow the whole panel to be draggable
     *
     * @param dragView A view that will be used to drag the panel.
     */
    fun setDragView(dragView: View?) {
        // First: Unset any listeners to prevent leaks on old references
        this.dragView?.setOnClickListener(null)
        this.dragView = dragView
        this.dragView?.let {
            it.isClickable = true
            it.isFocusable = false
            it.isFocusableInTouchMode = false
            it.setOnClickListener(OnClickListener {
                if (!isEnabled || !isTouchEnabled) return@OnClickListener
                slideState =
                    if (slideState !== PanelState.EXPANDED && slideState !== PanelState.ANCHORED) {
                        if (anchorPoint < DEFAULT_ANCHOR_POINT) {
                            PanelState.ANCHORED
                        } else {
                            PanelState.EXPANDED
                        }
                    } else {
                        PanelState.COLLAPSED
                    }
            })
        }
    }

    /**
     * Set the draggable view portion. Use to null, to allow the whole panel to be draggable
     *
     * @param dragViewResId The resource ID of the new drag view
     */
    fun setDragView(@IdRes dragViewResId: Int) {
        this.dragViewResId = dragViewResId
        setDragView(findViewById(dragViewResId))
    }

    /**
     * Sets the current scrollable view helper. See ScrollableViewHelper description for details.
     */
    fun setScrollableViewHelper(helper: ScrollableViewHelper) {
        scrollableViewHelper = helper
    }

    /**
     * Set maximum slide offset to move sliding layout in expanded state
     * The value must be in range of [ 0, 1]
     *
     * @param offset max sliding offset
     */
    fun setMaxSlideOffset(offset: Float) {
        if (offset <= DEFAULT_MAX_SLIDING_OFFSET) {
            maxSlideOffset = offset
        }
    }

    /**
     * Sets whether or not the panel overlays the content
     */
    @Deprecated("Use proper Kotlin syntax accessor", ReplaceWith("overlayContent = value"))
    fun setOverlayed(overlayed: Boolean) {
        overlayContent = overlayed
    }

    /**
     * Check if the panel is set as an overlay.
     */
    @Deprecated("Use proper Kotlin syntax accessor", ReplaceWith("getOverlayContent()"))
    fun isOverlayed(): Boolean {
        return overlayContent
    }

    /**
     * Check whether or not the main content is clipped to the top of the panel
     */
    @Deprecated("Use proper Kotlin syntax access", ReplaceWith("getClipPanel()"))
    fun isClipPanel(): Boolean {
        return clipPanel
    }

    /**
     * Triggers a panel slide callback on all available [panelSlideListeners]. Careful: this process
     * is synchronized to avoid out-of-order callbacks. Call this method sparingly.
     */
    fun dispatchOnPanelSlide(panel: View) {
        synchronized(panelSlideListeners) {
            for (l in panelSlideListeners) {
                l.onPanelSlide(panel, slideOffset)
            }
        }
    }

    /**
     * Triggers a panel state change callback on all available [panelSlideListeners], including the
     * previous state and the new state. Careful: this process is synchronized to avoid out-of-order
     * callbacks. Call this method sparingly.
     */
    fun dispatchOnPanelStateChanged(panel: View, previousState: PanelState, newState: PanelState) {
        synchronized(panelSlideListeners) {
            for (l in panelSlideListeners) {
                l.onPanelStateChanged(panel, previousState, newState)
            }
        }
        sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    }

    fun updateObscuredViewVisibility() {
        if (childCount == 0) return
        val left: Int
        val right: Int
        val top: Int
        val bottom: Int
        if (slideableView?.hasOpaqueBackground() == true) {
            left = slideableView!!.left
            right = slideableView!!.right
            top = slideableView!!.top
            bottom = slideableView!!.bottom
        } else {
            bottom = 0
            top = 0
            right = 0
            left = 0
        }
        val child = getChildAt(0)
        val clampedChildLeft = max(paddingLeft, child.left)
        val clampedChildTop = max(paddingTop, child.top)
        val clampedChildRight = min(width - paddingRight, child.right)
        val clampedChildBottom = min(height - paddingBottom, child.bottom)
        val vis: Int =
            if (clampedChildLeft >= left && clampedChildTop >= top && clampedChildRight <= right && clampedChildBottom <= bottom) {
                INVISIBLE
            } else {
                VISIBLE
            }
        child.visibility = vis
    }

    fun setAllChildrenVisible() {
        var i = 0
        val childCount = childCount
        while (i < childCount) {
            val child = getChildAt(i)
            if (child.visibility == INVISIBLE) {
                child.visibility = VISIBLE
            }
            i++
        }
    }

    fun View.hasOpaqueBackground(): Boolean {
        val background = this.background
        return background != null && background.opacity == PixelFormat.OPAQUE
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        firstLayout = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        firstLayout = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        check(!(widthMode != MeasureSpec.EXACTLY && widthMode != MeasureSpec.AT_MOST)) { "Width must have an exact value or MATCH_PARENT" }
        check(!(heightMode != MeasureSpec.EXACTLY && heightMode != MeasureSpec.AT_MOST)) { "Height must have an exact value or MATCH_PARENT" }
        val childCount = childCount
        check(childCount == 2) { "Sliding up panel layout must have exactly 2 children!" }
        mainView = getChildAt(0)
        slideableView = getChildAt(1)
        if (dragView == null) {
            setDragView(slideableView)
        }

        // If the sliding panel is not visible, then put the whole view in the hidden state
        if (slideableView?.visibility != VISIBLE) {
            slideState = PanelState.HIDDEN
        }
        val layoutHeight = heightSize - paddingTop - paddingBottom
        val layoutWidth = widthSize - paddingLeft - paddingRight

        // First pass. Measure based on child LayoutParams width/height.
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val lp = child.layoutParams as LayoutWeightParams

            // We always measure the sliding panel in order to know it's height (needed for show panel)
            if (child.visibility == GONE && i == 0) {
                continue
            }
            var height = layoutHeight
            var width = layoutWidth
            if (child === mainView) {
                if (!overlayContent && slideState !== PanelState.HIDDEN) {
                    height -= panelHeight
                }
                width -= lp.leftMargin + lp.rightMargin
            } else if (child === slideableView) {
                // The slideable view should be aware of its top margin.
                // See https://github.com/umano/AndroidSlidingUpPanel/issues/412.
                height -= lp.topMargin
            }
            val childWidthSpec: Int = when (lp.width) {
                MarginLayoutParams.WRAP_CONTENT -> {
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST)
                }
                MarginLayoutParams.MATCH_PARENT -> {
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
                }
                else -> {
                    MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY)
                }
            }
            var childHeightSpec: Int
            if (lp.height == MarginLayoutParams.WRAP_CONTENT) {
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST)
            } else {
                // Modify the height based on the weight.
                if (lp.weight > 0 && lp.weight < 1) {
                    height = (height * lp.weight).toInt()
                } else if (lp.height != MarginLayoutParams.MATCH_PARENT) {
                    height = lp.height
                }
                childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            }
            child.measure(childWidthSpec, childHeightSpec)
            if (child === slideableView) {
                slideRange = slideableView!!.measuredHeight - panelHeight
            }
        }
        setMeasuredDimension(widthSize, heightSize)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        val childCount = childCount
        if (firstLayout) {
            slideOffset = when (slideState) {
                PanelState.EXPANDED -> maxSlideOffset
                PanelState.ANCHORED -> anchorPoint
                PanelState.HIDDEN -> {
                    val newTop =
                        computePanelTopPosition(0.0f) + if (isSlidingUp) +panelHeight else -panelHeight
                    computeSlideOffset(newTop)
                }

                else -> 0f
            }
        }
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val lp = child.layoutParams as LayoutWeightParams

            // Always layout the sliding view on the first layout
            if (child.visibility == GONE && (i == 0 || firstLayout)) {
                continue
            }
            val childHeight = child.measuredHeight
            var childTop = paddingTop
            if (child === slideableView) {
                childTop = computePanelTopPosition(slideOffset)
            }
            if (!isSlidingUp) {
                if (child === mainView && !overlayContent) {
                    childTop =
                        computePanelTopPosition(slideOffset) + slideableView!!.measuredHeight
                }
            }
            val childBottom = childTop + childHeight
            val childLeft = paddingLeft + lp.leftMargin
            val childRight = childLeft + child.measuredWidth
            child.layout(childLeft, childTop, childRight, childBottom)
        }
        if (firstLayout) {
            updateObscuredViewVisibility()
        }
        applyParallaxForCurrentSlideOffset()
        firstLayout = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Recalculate sliding panes and their details
        if (h != oldh) {
            firstLayout = true
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        // If the scrollable view is handling touch, never intercept
        if (isScrollableViewHandlingTouch || !isTouchEnabled) {
            dragHelper?.abort()
            return false
        }
        val action = ev.action
        val x = ev.x
        val y = ev.y
        val adx = abs(x - initialMotionX)
        val ady = abs(y - initialMotionY)
        val dragSlop = dragHelper!!.touchSlop
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                isUnableToDrag = false
                initialMotionX = x
                initialMotionY = y
                if (!isViewUnder(dragView, x.toInt(), y.toInt())) {
                    dragHelper!!.cancel()
                    isUnableToDrag = true
                    return false
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (ady > dragSlop && adx > ady) {
                    dragHelper!!.cancel()
                    isUnableToDrag = true
                    return false
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                // If the dragView is still dragging when we get here, we need to call processTouchEvent
                // so that the view is settled
                // Added to make scrollable views work (tokudu)
                if (dragHelper!!.isDragging) {
                    dragHelper!!.processTouchEvent(ev)
                    return true
                }
                // Check if this was a click on the faded part of the screen, and fire off the listener if there is one.
                if (ady <= dragSlop && adx <= dragSlop && slideOffset > 0 && !isViewUnder(
                        slideableView,
                        initialMotionX.toInt(),
                        initialMotionY.toInt()
                    ) && fadeOnClickListener != null
                ) {
                    playSoundEffect(SoundEffectConstants.CLICK)
                    fadeOnClickListener!!.onClick(this)
                    return true
                }
            }
        }
        return dragHelper!!.shouldInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return if (!isEnabled || !isTouchEnabled) {
            super.onTouchEvent(ev)
        } else try {
            dragHelper!!.processTouchEvent(ev)
            true
        } catch (ex: Exception) {
            // Ignore the pointer out of range exception
            false
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        if (!isEnabled || !isTouchEnabled || isUnableToDrag && action != MotionEvent.ACTION_DOWN) {
            dragHelper?.abort()
            return super.dispatchTouchEvent(ev)
        }
        val x = ev.x
        val y = ev.y
        if (action == MotionEvent.ACTION_DOWN) {
            isScrollableViewHandlingTouch = false
            prevMotionX = x
            prevMotionY = y
        } else if (action == MotionEvent.ACTION_MOVE) {
            val dx = x - prevMotionX
            val dy = y - prevMotionY
            prevMotionX = x
            prevMotionY = y
            if (abs(dx) > abs(dy)) {
                // Scrolling horizontally, so ignore
                return super.dispatchTouchEvent(ev)
            }

            // If the scroll view isn't under the touch, pass the
            // event along to the dragView.
            if (!isViewUnder(scrollableView, initialMotionX.toInt(), initialMotionY.toInt())) {
                return super.dispatchTouchEvent(ev)
            }

            // Which direction (up or down) is the drag moving?
            if (dy * (if (isSlidingUp) 1 else -1) > 0) { // Collapsing
                // Is the child less than fully scrolled?
                // Then let the child handle it.
                if (scrollableViewHelper.getScrollableViewScrollPosition(
                        scrollableView,
                        isSlidingUp
                    ) > 0
                ) {
                    isScrollableViewHandlingTouch = true
                    return super.dispatchTouchEvent(ev)
                }

                // Was the child handling the touch previously?
                // Then we need to rejigger things so that the
                // drag panel gets a proper down event.
                if (isScrollableViewHandlingTouch) {
                    // Send an 'UP' event to the child.
                    val up = MotionEvent.obtain(ev)
                    up.action = MotionEvent.ACTION_CANCEL
                    super.dispatchTouchEvent(up)
                    up.recycle()

                    // Send a 'DOWN' event to the panel. (We'll cheat
                    // and hijack this one)
                    ev.action = MotionEvent.ACTION_DOWN
                }
                isScrollableViewHandlingTouch = false
                return onTouchEvent(ev)
            } else if (dy * (if (isSlidingUp) 1 else -1) < 0) { // Expanding
                // Is the panel less than fully expanded?
                // Then we'll handle the drag here.
                if (slideOffset < maxSlideOffset) {
                    isScrollableViewHandlingTouch = false
                    return onTouchEvent(ev)
                }

                // Was the panel handling the touch previously?
                // Then we need to rejigger things so that the
                // child gets a proper down event.
                if (!isScrollableViewHandlingTouch && dragHelper!!.isDragging) {
                    dragHelper!!.cancel()
                    ev.action = MotionEvent.ACTION_DOWN
                }
                isScrollableViewHandlingTouch = true
                return super.dispatchTouchEvent(ev)
            }
        } else if (action == MotionEvent.ACTION_UP) {
            // If the scrollable view was handling the touch and we receive an up
            // we want to clear any previous dragging state so we don't intercept a touch stream accidentally
            if (isScrollableViewHandlingTouch) {
                dragHelper!!.setDragState(ViewDragHelper.STATE_IDLE)
            }
        }

        // In all other cases, just let the default behavior take over.
        return super.dispatchTouchEvent(ev)
    }

    private fun isViewUnder(view: View?, x: Int, y: Int): Boolean {
        if (view == null) return false
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val parentLocation = IntArray(2)
        getLocationOnScreen(parentLocation)
        val screenX = parentLocation[0] + x
        val screenY = parentLocation[1] + y
        return screenX >= viewLocation[0] && screenX < viewLocation[0] + view.width && screenY >= viewLocation[1] && screenY < viewLocation[1] + view.height
    }

    /*
     * Computes the top position of the panel based on the slide offset.
     */
    private fun computePanelTopPosition(slideOffset: Float): Int {
        val slidingViewHeight = if (slideableView != null) slideableView!!.measuredHeight else 0
        val slidePixelOffset = (slideOffset * slideRange).toInt()
        // Compute the top of the panel if its collapsed
        return if (isSlidingUp) measuredHeight - paddingBottom - panelHeight - slidePixelOffset else paddingTop - slidingViewHeight + panelHeight + slidePixelOffset
    }

    /*
     * Computes the slide offset based on the top position of the panel
     */
    private fun computeSlideOffset(topPosition: Int): Float {
        // Compute the panel top position if the panel is collapsed (offset 0)
        val topBoundCollapsed = computePanelTopPosition(0f)

        // Determine the new slide offset based on the collapsed top position and the new required
        // top position
        return if (isSlidingUp) (topBoundCollapsed - topPosition).toFloat() / slideRange else (topPosition - topBoundCollapsed).toFloat() / slideRange
    }

    private fun setPanelStateInternal(state: PanelState) {
        if (slideState === state) return
        val oldState = slideState
        slideState = state
        dispatchOnPanelStateChanged(this, oldState, state)
    }

    /**
     * Update the parallax based on the current slide offset.
     */
    @SuppressLint("NewApi")
    private fun applyParallaxForCurrentSlideOffset() {
        if (parallaxOffset > 0) {
            val mainViewOffset = getCurrentParallaxOffset()
            mainView!!.translationY = mainViewOffset.toFloat()
        }
    }

    private fun onPanelDragged(newTop: Int) {
        if (slideState !== PanelState.DRAGGING) {
            lastNotDraggingSlideState = slideState
        }
        setPanelStateInternal(PanelState.DRAGGING)
        // Recompute the slide offset based on the new top position
        slideOffset = computeSlideOffset(newTop)
        applyParallaxForCurrentSlideOffset()
        // Dispatch the slide event
        dispatchOnPanelSlide(slideableView!!)
        // If the slide offset is negative, and overlay is not on, we need to increase the
        // height of the main content
        val lp = mainView!!.layoutParams as LayoutWeightParams
        val defaultHeight =
            height - paddingBottom - paddingTop - if (slideOffset < 0) 0 else panelHeight
        if (slideOffset <= 0 && !overlayContent) {
            // expand the main view
            lp.height =
                if (isSlidingUp) newTop - paddingBottom else height - paddingBottom - slideableView!!.measuredHeight - newTop
            if (lp.height == defaultHeight) {
                lp.height = MarginLayoutParams.MATCH_PARENT
            }
            mainView!!.requestLayout()
        } else if (lp.height != MarginLayoutParams.MATCH_PARENT && !overlayContent) {
            lp.height = MarginLayoutParams.MATCH_PARENT
            mainView!!.requestLayout()
        }
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        val result: Boolean
        if (canvasSaveProxy == null || !canvasSaveProxy!!.isFor(canvas)) {
            canvasSaveProxy = canvasSaveProxyFactory.create(canvas)
        }
        val save = canvasSaveProxy!!.save()
        if (slideableView != null && slideableView !== child) { // if main view
            // Clip against the slider; no sense drawing what will immediately be covered,
            // Unless the panel is set to overlay content
            canvas.getClipBounds(tmpRect)
            if (!overlayContent) {
                if (isSlidingUp) {
                    tmpRect.bottom = min(tmpRect.bottom, slideableView!!.top)
                } else {
                    tmpRect.top = max(tmpRect.top, slideableView!!.bottom)
                }
            }
            if (clipPanel) {
                canvas.clipRect(tmpRect)
            }
            result = super.drawChild(canvas, child, drawingTime)
            if (coveredFadeColor != 0 && slideOffset > 0) {
                val baseAlpha = coveredFadeColor and -0x1000000 ushr 24
                val imag = (baseAlpha * slideOffset).toInt()
                val color = imag shl 24 or (coveredFadeColor and 0xffffff)
                coveredFadePaint.color = color
                canvas.drawRect(tmpRect, coveredFadePaint)
            }
        } else {
            result = super.drawChild(canvas, child, drawingTime)
        }
        canvas.restoreToCount(save)
        return result
    }

    /**
     * Smoothly animate mDraggingPane to the target X position within its range.
     *
     * @param slideOffset position to animate to
     * @param velocity    initial velocity in case of fling, or 0.
     */
    fun smoothSlideTo(slideOffset: Float, velocity: Int): Boolean {
        if (!isEnabled || slideableView == null) {
            // Nothing to do.
            return false
        }
        val panelTop = computePanelTopPosition(slideOffset)
        if (dragHelper!!.smoothSlideViewTo(slideableView, slideableView!!.left, panelTop)) {
            setAllChildrenVisible()
            ViewCompat.postInvalidateOnAnimation(this)
            return true
        }
        return false
    }

    override fun computeScroll() {
        dragHelper?.let {
            if (it.continueSettling(true)) {
                if (!isEnabled) {
                    it.abort()
                    return
                }
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }
    }

    override fun draw(c: Canvas) {
        super.draw(c)

        // draw the shadow
        if (shadowDrawable != null && slideableView != null) {
            val right = slideableView!!.right
            val top: Int
            val bottom: Int
            if (isSlidingUp) {
                top = slideableView!!.top - shadowHeight
                bottom = slideableView!!.top
            } else {
                top = slideableView!!.bottom
                bottom = slideableView!!.bottom + shadowHeight
            }
            val left = slideableView!!.left
            shadowDrawable?.setBounds(left, top, right, bottom)
            shadowDrawable?.draw(c)
        }
    }

    /**
     * Tests scrollability within child views of v given a delta of dx.
     *
     * @param view      View to test for horizontal scrollability
     * @param checkV Whether the view v passed should itself be checked for scrollability (true),
     * or just its children (false).
     * @param dx     Delta scrolled in pixels
     * @param x      X coordinate of the active touch point
     * @param y      Y coordinate of the active touch point
     * @return true if child views of v can be scrolled by delta of dx.
     */
    protected fun canScroll(view: View, checkV: Boolean, dx: Int, x: Int, y: Int): Boolean {
        if (view is ViewGroup) {
            val scrollX = view.getScrollX()
            val scrollY = view.getScrollY()
            val count = view.childCount
            // Count backwards - let topmost views consume scroll distance first.
            for (i in count - 1 downTo 0) {
                val child = view.getChildAt(i)
                if (x + scrollX >= child.left && x + scrollX < child.right && y + scrollY >= child.top && y + scrollY < child.bottom &&
                    canScroll(
                        child, true, dx, x + scrollX - child.left,
                        y + scrollY - child.top
                    )
                ) {
                    return true
                }
            }
        }
        return checkV && view.canScrollHorizontally(-dx)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutWeightParams()
    }

    override fun generateLayoutParams(p: LayoutParams): LayoutParams {
        return if (p is MarginLayoutParams) LayoutWeightParams(p) else LayoutWeightParams(p)
    }

    override fun checkLayoutParams(p: LayoutParams): Boolean {
        return p is LayoutWeightParams && super.checkLayoutParams(p)
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return LayoutWeightParams(context, attrs)
    }

    public override fun onSaveInstanceState(): Parcelable {
        val bundle = Bundle()
        bundle.putParcelable("superState", super.onSaveInstanceState())
        bundle.putSerializable(
            SLIDING_STATE,
            if (slideState !== PanelState.DRAGGING) slideState else lastNotDraggingSlideState
        )
        return bundle
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        var parcelable: Parcelable? = state
        if (parcelable is Bundle) {
            val bundle = parcelable
            slideState = requireNotNull(bundle.getSerializable(SLIDING_STATE)) as PanelState
            parcelable = bundle.getParcelable("superState")
        }
        super.onRestoreInstanceState(parcelable)
    }

    private inner class DragHelperCallback : ViewDragHelper.Callback() {
        override fun tryCaptureView(child: View?, pointerId: Int): Boolean {
            return !isUnableToDrag && child === slideableView
        }

        override fun onViewDragStateChanged(state: Int) {
            dragHelper?.let {
                if (it.viewDragState == ViewDragHelper.STATE_IDLE) {
                    slideOffset = computeSlideOffset(slideableView!!.top)
                    applyParallaxForCurrentSlideOffset()
                    if (slideOffset == 1f) {
                        updateObscuredViewVisibility()
                        setPanelStateInternal(PanelState.EXPANDED)
                    } else if (slideOffset == 0f) {
                        setPanelStateInternal(PanelState.COLLAPSED)
                    } else if (slideOffset < 0) {
                        setPanelStateInternal(PanelState.HIDDEN)
                        slideableView!!.visibility = INVISIBLE
                    } else {
                        updateObscuredViewVisibility()
                        setPanelStateInternal(PanelState.ANCHORED)
                    }
                }
            }
        }

        override fun onViewCaptured(capturedChild: View?, activePointerId: Int) {
            setAllChildrenVisible()
        }

        override fun onViewPositionChanged(changedView: View?, left: Int, top: Int, dx: Int, dy: Int) {
            onPanelDragged(top)
            invalidate()
        }

        override fun onViewReleased(releasedChild: View?, xvel: Float, yvel: Float) {
            val target: Int

            // direction is always positive if we are sliding in the expanded direction
            val direction = if (isSlidingUp) -yvel else yvel
            target = when {
                direction > 0 && slideOffset <= anchorPoint -> {
                    // swipe up -> expand and stop at anchor point
                    computePanelTopPosition(anchorPoint)
                }

                direction > 0 && slideOffset > anchorPoint -> {
                    // swipe up past anchor -> expand
                    computePanelTopPosition(maxSlideOffset)
                }

                direction < 0 && slideOffset >= anchorPoint -> {
                    // swipe down -> collapse and stop at anchor point
                    computePanelTopPosition(anchorPoint)
                }

                direction < 0 && slideOffset < anchorPoint -> {
                    // swipe down past anchor -> collapse
                    computePanelTopPosition(0.0f)
                }

                slideOffset >= (1f + anchorPoint) / 2 -> {
                    // zero velocity, and far enough from anchor point => expand to the top
                    computePanelTopPosition(maxSlideOffset)
                }

                slideOffset >= anchorPoint / 2 -> {
                    // zero velocity, and close enough to anchor point => go to anchor
                    computePanelTopPosition(anchorPoint)
                }

                else -> {
                    // settle at the bottom
                    computePanelTopPosition(0.0f)
                }
            }
            dragHelper?.settleCapturedViewAt(releasedChild!!.left, target)
            invalidate()
        }

        override fun getViewVerticalDragRange(child: View?): Int {
            return slideRange
        }

        override fun clampViewPositionVertical(child: View?, top: Int, dy: Int): Int {
            val collapsedTop = computePanelTopPosition(0f)
            val expandedTop = computePanelTopPosition(maxSlideOffset)
            return if (isSlidingUp) {
                min(max(top, expandedTop), collapsedTop)
            } else {
                min(max(top, collapsedTop), expandedTop)
            }
        }
    }
}
