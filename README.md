[![GitHub license](https://img.shields.io/badge/license-Apache%20Version%202.0-blue.svg)](https://github.com/sbrukhanda/fragmentviewpager/blob/master/LICENSE.txt)
[![Build Status](https://travis-ci.org/hannesa2/AndroidSlidingUpPanel.svg?branch=master)](https://travis-ci.org/hannesa2/AndroidSlidingUpPanel)
[![](https://jitpack.io/v/hannesa2/AndroidSlidingUpPanel.svg)](https://jitpack.io/#hannesa2/AndroidSlidingUpPanel)

**Note:** Because origin maintainer currently does not merge PR's, I collect some of them in this repository

Android Sliding Up Panel
=========================

This library provides a simple way to add a draggable sliding up panel (popularized by Google Music and Google Maps) to your Android application.

![Demo](https://github.com/hannesa2/AndroidSlidingUpPanel/blob/master/SlidingUp.gif)

### Importing the Library

Simply add the following dependency to your `build.gradle` file to use the latest version:

```Gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
```groovy
dependencies {
    implementation 'com.github.hannesa2:AndroidSlidingUpPanel:$latest_version' // Android X

    // or
    implementation 'com.github.hannesa2:AndroidSlidingUpPanel:3.5.0' // supportLib
}
```

### Usage

* Include `com.sothree.slidinguppanel.SlidingUpPanelLayout` as the root element in your activity layout.
* The layout must have `gravity` set to either `top` or `bottom`.
* Make sure that it has two children. The first child is your main layout. The second child is your layout for the sliding up panel.
* The main layout should have the width and the height set to `match_parent`.
* The sliding layout should have the width set to `match_parent` and the height set to either `match_parent`, `wrap_content` or the max desireable height. If you would like to define the height as the percetange of the screen, set it to `match_parent` and also define a `layout_weight` attribute for the sliding view.
* By default, the whole panel will act as a drag region and will intercept clicks and drag events. You can restrict the drag area to a specific view by using the `setDragView` method or `umanoDragView` attribute.

For more information, please refer to the sample code.

```xml
<com.sothree.slidinguppanel.SlidingUpPanelLayout
    xmlns:sothree="http://schemas.android.com/apk/res-auto"
    android:id="@+id/sliding_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="bottom"
    sothree:umanoPanelHeight="68dp"
    sothree:umanoShadowHeight="4dp">

    <TextView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center"
        android:text="Main Content"
        android:textSize="16sp" />

    <TextView
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:gravity="center|top"
        android:text="The Awesome Sliding Up Panel"
        android:textSize="16sp" />
</com.sothree.slidinguppanel.SlidingUpPanelLayout>
```
For smooth interaction with the ActionBar, make sure that `windowActionBarOverlay` is set to `true` in your styles:
```xml
<style name="AppTheme">
    <item name="android:windowActionBarOverlay">true</item>
</style>
```
However, in this case you would likely want to add a top margin to your main layout of `?android:attr/actionBarSize`
or `?attr/actionBarSize` to support older API versions.

### Caveats, Additional Features and Customization

* If you are using a custom `umanoDragView`, the panel will pass through the click events to the main layout. Make your second layout `clickable` to prevent this.
* You can change the panel height by using the `setPanelHeight` method or `umanoPanelHeight` attribute.
* If you would like to hide the shadow above the sliding panel, set `shadowHeight` attribute to 0.
* Use `setEnabled(false)` to completely disable the sliding panel (including touch and programmatic sliding)
* Use `setTouchEnabled(false)` to disables panel's touch responsiveness (drag and click), you can still control the panel programatically
* Use `getPanelState` to get the current panel state
* Use `setPanelState` to set the current panel state
* You can add parallax to the main view by setting `umanoParallaxOffset` attribute (see demo for the example).
* You can set a anchor point in the middle of the screen using `setAnchorPoint` to allow an intermediate expanded state for the panel (similar to Google Maps).
* You can set a `PanelSlideListener` to monitor events about sliding panes.
* You can also make the panel slide from the top by changing the `layout_gravity` attribute of the layout to `top`.
* You can provide a scroll interpolator for the panel movement by setting `umanoScrollInterpolator` attribute. For instance, if you want a bounce or overshoot effect for the panel.
* By default, the panel pushes up the main content. You can make it overlay the main content by using `setOverlayed` method or `umanoOverlay` attribute. This is useful if you would like to make the sliding layout semi-transparent. You can also set `umanoClipPanel` to false to make the panel transparent in non-overlay mode.
* By default, the main content is dimmed as the panel slides up. You can change the dim color by changing `umanoFadeColor`. Set it to `"@android:color/transparent"` to remove dimming completely.

### Scrollable Sliding Views

If you have a scrollable view inside of the sliding panel, make sure to set `umanoScrollableView` attribute on the panel to supported nested scrolling. The panel supports `ListView`, `ScrollView` and `RecyclerView` out of the box, but you can add support for any type of a scrollable view by setting a custom `ScrollableViewHelper`. Here is an example for `NestedScrollView`

```
public class NestedScrollableViewHelper extends ScrollableViewHelper {
  public int getScrollableViewScrollPosition(View scrollableView, boolean isSlidingUp) {
    if (mScrollableView instanceof NestedScrollView) {
      if(isSlidingUp){
        return mScrollableView.getScrollY();
      } else {
        NestedScrollView nsv = ((NestedScrollView) mScrollableView);
        View child = nsv.getChildAt(0);
        return (child.getBottom() - (nsv.getHeight() + nsv.getScrollY()));
      }
    } else {
      return 0;
    }
  }
}
```

Once you define your helper, you can set it using `setScrollableViewHelper` on the sliding panel.

### Licence

> Licensed under the Apache License, Version 2.0 (the "License");
> you may not use this work except in compliance with the License.
> You may obtain a copy of the License in the LICENSE file, or at:
>
>  [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)
>
> Unless required by applicable law or agreed to in writing, software
> distributed under the License is distributed on an "AS IS" BASIS,
> WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
> See the License for the specific language governing permissions and
> limitations under the License.
