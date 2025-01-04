package info.hannes.slidingup.demo.tools

import android.util.Log
import android.view.View
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import com.sothree.slidinguppanel.PanelState
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import org.hamcrest.Description
import org.hamcrest.Matcher

fun withValue(expectedValue: PanelState): Matcher<View?> {
    return object : BoundedMatcher<View?, SlidingUpPanelLayout>(SlidingUpPanelLayout::class.java) {
        override fun describeTo(description: Description) {
            description.appendText("expected: $expectedValue")
        }

        override fun matchesSafely(slider: SlidingUpPanelLayout): Boolean {
            Log.d("slider", slider.panelState.toString())
            return slider.panelState == expectedValue
        }
    }
}

fun setValue(value: PanelState): ViewAction {
    return object : ViewAction {
        override fun getDescription(): String {
            return "Set Slider value to $value"
        }

        override fun getConstraints(): Matcher<View> {
            return ViewMatchers.isAssignableFrom(SlidingUpPanelLayout::class.java)
        }

        override fun perform(uiController: UiController?, view: View) {
            val slidingUpPanelLayout = view as SlidingUpPanelLayout
            slidingUpPanelLayout.setPanelState(value)
        }
    }
}