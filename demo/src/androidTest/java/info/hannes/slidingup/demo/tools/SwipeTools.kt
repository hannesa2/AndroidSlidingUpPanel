package info.hannes.slidingup.demo.tools

import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.GeneralSwipeAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Swipe
import androidx.test.espresso.action.ViewActions


// Based on public static ViewAction swipeUp()/**
fun customSwipeUp(): ViewAction? {
    return ViewActions.actionWithAssertions(
        GeneralSwipeAction(
            Swipe.SLOW,
            GeneralLocation.translate(GeneralLocation.BOTTOM_CENTER, 0f, -0.083f), // TODO can't control the height
            GeneralLocation.TOP_CENTER,
            Press.FINGER
        )
    )
}

fun swipeFromBottomTop(): ViewAction? {
    return GeneralSwipeAction(
        Swipe.FAST, GeneralLocation.BOTTOM_CENTER,
        GeneralLocation.TOP_CENTER, Press.FINGER
    )
}