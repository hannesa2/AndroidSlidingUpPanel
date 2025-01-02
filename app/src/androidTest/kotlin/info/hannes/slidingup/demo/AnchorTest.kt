package info.hannes.slidingup.demo

import android.graphics.Bitmap
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.core.graphics.writeToTestStorage
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.captureToBitmap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.sothree.slidinguppanel.PanelState
import info.hannes.slidingup.demo.tools.setValue
import info.hannes.slidingup.demo.tools.withValue
import info.hannes.slidinguppanel.demo.DemoActivity
import info.hannes.slidinguppanel.demo.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnchorTest {

    private val WAIT_SLIDER = 600L

    @get:Rule
    val activityScenarioRule = activityScenarioRule<DemoActivity>()

    @get:Rule
    var nameRule = TestName()

    @Before
    fun setup() {
        openActionBarOverflowOrOptionsMenu(getInstrumentation().targetContext)
        Thread.sleep(WAIT_SLIDER)
        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap -> bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}_setup") })

        onView(withText(R.string.action_anchor_enable)).perform(click())
    }

    @Test
    fun testExpand() {
        Thread.sleep(WAIT_SLIDER)
        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap -> bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-1") })
        onView(withId(R.id.sliding_layout)).check(matches(withValue(PanelState.ANCHORED)))

        onView(withId(R.id.sliding_layout)).perform(setValue(PanelState.EXPANDED))

        Thread.sleep(WAIT_SLIDER)
        onView(withId(R.id.sliding_layout)).check(matches(withValue(PanelState.EXPANDED)))
        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap -> bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-2") })

        onView(withId(R.id.sliding_layout)).perform(setValue(PanelState.COLLAPSED))
        Thread.sleep(WAIT_SLIDER)
        onView(withId(R.id.sliding_layout)).check(matches(withValue(PanelState.COLLAPSED)))
        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap -> bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-3") })
    }

    @Test
    fun testAnchorWithAnchored() {
        onView(withId(R.id.sliding_layout)).check(matches(withValue(PanelState.ANCHORED)))

        onView(withId(R.id.sliding_layout)).perform(setValue(PanelState.EXPANDED))

        Thread.sleep(WAIT_SLIDER)
        onView(withId(R.id.sliding_layout)).check(matches(withValue(PanelState.EXPANDED)))

        // without state anchored, a state ANCHORED should be ignored
        onView(withId(R.id.sliding_layout)).perform(setValue(PanelState.ANCHORED))
        Thread.sleep(WAIT_SLIDER)
        // should be still EXPANDED
        onView(withId(R.id.sliding_layout)).check(matches(withValue(PanelState.ANCHORED)))
        onView(isRoot())
            .perform(captureToBitmap { bitmap: Bitmap -> bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-2") })
    }

    @Test
    fun testSwipe() {
        onView(withId(R.id.sliding_layout)).perform(setValue(PanelState.COLLAPSED))
        Thread.sleep(WAIT_SLIDER)
        onView(withId(R.id.sliding_layout)).check(matches(withValue(PanelState.COLLAPSED)))

        for (i in 0..2) {
            onView(withId(R.id.follow)).perform(ViewActions.swipeUp())
            onView(withId(R.id.follow)).perform(ViewActions.swipeDown())
            onView(isRoot())
                .perform(captureToBitmap { bitmap: Bitmap -> bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-COLLAPSED-$i") })
        }

        onView(withId(R.id.sliding_layout)).perform(setValue(PanelState.ANCHORED))

        onView(withId(R.id.sliding_layout)).perform(setValue(PanelState.EXPANDED))
        Thread.sleep(WAIT_SLIDER)

        for (i in 0..2) {
            onView(withId(R.id.follow)).perform(ViewActions.swipeUp())
            onView(withId(R.id.follow)).perform(ViewActions.swipeDown())
            Thread.sleep(WAIT_SLIDER)
            onView(isRoot())
                .perform(captureToBitmap { bitmap: Bitmap -> bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-EXPANDED-$i") })
        }

        onView(withId(R.id.sliding_layout)).perform(setValue(PanelState.ANCHORED))
        Thread.sleep(WAIT_SLIDER)

        for (i in 0..2) {
            onView(withId(R.id.follow)).perform(ViewActions.swipeUp())
            onView(withId(R.id.follow)).perform(ViewActions.swipeDown())
            Thread.sleep(WAIT_SLIDER)
            onView(isRoot())
                .perform(captureToBitmap { bitmap: Bitmap -> bitmap.writeToTestStorage("${javaClass.simpleName}_${nameRule.methodName}-ANCHORED-$i") })
        }
    }
}
