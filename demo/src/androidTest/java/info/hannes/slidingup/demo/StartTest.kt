package info.hannes.slidingup.demo

import android.util.Log
import android.view.View
import androidx.test.espresso.assertion.ViewAssertions.matches
import com.sothree.slidinguppanel.demo.R
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.screenshot.captureToBitmap
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sothree.slidinguppanel.PanelState
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import com.sothree.slidinguppanel.demo.DemoActivity
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartTest {

    private val WAIT_SLIDER = 600L

    @get:Rule
    val activityScenarioRule = activityScenarioRule<DemoActivity>()

    @get:Rule
    var nameRule = TestName()

    @Test
    fun smokeTestSimplyStart() {
        onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}")
    }

    @Test
    fun testExpand() {
        onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-1")
        onView(withId(R.id.sliding_layout)).check(matches(withValue(PanelState.COLLAPSED)))

        onView(withId(R.id.sliding_layout)).perform(setValue(PanelState.EXPANDED))

        Thread.sleep(WAIT_SLIDER)
        onView(withId(R.id.sliding_layout)).check(matches(withValue(PanelState.EXPANDED)))
        onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-2")

        onView(withId(R.id.sliding_layout)).perform(setValue(PanelState.COLLAPSED))
        Thread.sleep(WAIT_SLIDER)
        onView(withId(R.id.sliding_layout)).check(matches(withValue(PanelState.COLLAPSED)))
        onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-3")
    }

    @Test
    fun testAnchorWithoutSetAnchored() {
        onView(withId(R.id.sliding_layout)).check(matches(withValue(PanelState.COLLAPSED)))

        onView(withId(R.id.sliding_layout)).perform(setValue(PanelState.EXPANDED))

        Thread.sleep(WAIT_SLIDER)
        onView(withId(R.id.sliding_layout)).check(matches(withValue(PanelState.EXPANDED)))

        // without state anchored, a state ANCHORED should be ignored
        onView(withId(R.id.sliding_layout)).perform(setValue(PanelState.ANCHORED))
        Thread.sleep(WAIT_SLIDER)
        // should be still EXPANDED
        onView(withId(R.id.sliding_layout)).check(matches(withValue(PanelState.EXPANDED)))
        onView(ViewMatchers.isRoot())
            .captureToBitmap()
            .writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-2")
    }
}
