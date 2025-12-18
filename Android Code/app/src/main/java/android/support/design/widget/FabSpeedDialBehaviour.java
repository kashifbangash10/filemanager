/*
 * Copyright 2016 Yavor Ivanov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.support.design.widget;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.coordinatorlayout.widget.ViewGroupUtils;
import androidx.core.view.ViewCompat;
import androidx.core.view.ViewPropertyAnimatorCompat;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import com.example.apps.solidfilemamnager.ui.fabs.FabSpeedDial;

import static android.view.View.VISIBLE;

public class FabSpeedDialBehaviour extends CoordinatorLayout.Behavior<FabSpeedDial> {



    private static final boolean SNACKBAR_BEHAVIOR_ENABLED = Build.VERSION.SDK_INT >= 11;

    private ViewPropertyAnimatorCompat mFabTranslationYAnimator;
    private float mFabTranslationY;
    private Rect mTmpRect;

    public FabSpeedDialBehaviour(){
        super();
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent,
                                   FabSpeedDial child, View dependency) {

        return true;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FabSpeedDial child,
                                          View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            updateFabTranslationForSnackbar(parent, child, true);
        } else if (dependency instanceof AppBarLayout) {


            updateFabVisibility(parent, (AppBarLayout) dependency, child);
        }
        return false;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onDependentViewRemoved(CoordinatorLayout parent, FabSpeedDial child,
                                       View dependency) {
        if (dependency instanceof Snackbar.SnackbarLayout) {
            updateFabTranslationForSnackbar(parent, child, true);
        }
    }

    @SuppressLint("RestrictedApi")
    private boolean updateFabVisibility(CoordinatorLayout parent,
                                        AppBarLayout appBarLayout, FabSpeedDial child) {
        final CoordinatorLayout.LayoutParams lp =
                (CoordinatorLayout.LayoutParams) child.getLayoutParams();
        if (lp.getAnchorId() != appBarLayout.getId()) {


            return false;
        }

        if (child.getUserSetVisibility() != VISIBLE) {

            return false;
        }

        if (mTmpRect == null) {
            mTmpRect = new Rect();
        }


        final Rect rect = mTmpRect;
        ViewGroupUtils.getDescendantRect(parent, appBarLayout, rect);

        if (rect.bottom <= appBarLayout.getMinimumHeightForVisibleOverlappingContent()) {

            child.hide();
        } else {

            child.show();
        }
        return true;
    }

    private void updateFabTranslationForSnackbar(CoordinatorLayout parent,
                                                 final FabSpeedDial fab, boolean animationAllowed) {

        if (fab.getVisibility() != View.VISIBLE) {
            return;
        }

        final float targetTransY = getFabTranslationYForSnackbar(parent, fab);
        if (mFabTranslationY == targetTransY) {

            return;
        }

        final float currentTransY = ViewCompat.getTranslationY(fab);


        if (mFabTranslationYAnimator != null) {
            mFabTranslationYAnimator.cancel();
        }

        if (Math.abs(currentTransY - targetTransY) > (fab.getHeight() * 0.667f)) {
            mFabTranslationYAnimator = ViewCompat.animate(fab)
                    .setInterpolator(FabSpeedDial.FAST_OUT_SLOW_IN_INTERPOLATOR)
                    .translationY(targetTransY);
            mFabTranslationYAnimator.start();
        } else {
            ViewCompat.setTranslationY(fab, targetTransY);
        }

        mFabTranslationY = targetTransY;
    }

    @SuppressLint("RestrictedApi")
    private float getFabTranslationYForSnackbar(CoordinatorLayout parent,
                                                FabSpeedDial fab) {
        float minOffset = 0;
        final List<View> dependencies = parent.getDependencies(fab);
        for (int i = 0, z = dependencies.size(); i < z; i++) {
            final View view = dependencies.get(i);
            if (view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(fab, view)) {
                minOffset = Math.min(minOffset,
                        ViewCompat.getTranslationY(view) - view.getHeight());
            }
        }

        return minOffset;
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, FabSpeedDial child,
                                 int layoutDirection) {

        final List<View> dependencies = parent.getDependencies(child);
        for (int i = 0, count = dependencies.size(); i < count; i++) {
            final View dependency = dependencies.get(i);
            if (dependency instanceof AppBarLayout
                    && updateFabVisibility(parent, (AppBarLayout) dependency, child)) {
                break;
            }
        }

        parent.onLayoutChild(child, layoutDirection);



        updateFabTranslationForSnackbar(parent, child, false);
        return true;
    }

    /**
     * Pre-Lollipop we use padding so that the shadow has enough space to be drawn. This method
     * offsets our layout position so that we're positioned correctly if we're on one of
     * our parent's edges.
     */
/*    private void offsetIfNeeded(CoordinatorLayout parent, FabSpeedDial fab) {
        final Rect padding = fab.mShadowPadding;

        if (padding != null && padding.centerX() > 0 && padding.centerY() > 0) {
            final CoordinatorLayout.LayoutParams lp =
                    (CoordinatorLayout.LayoutParams) fab.getLayoutParams();

            int offsetTB = 0, offsetLR = 0;

            if (fab.getRight() >= parent.getWidth() - lp.rightMargin) {

                offsetLR = padding.right;
            } else if (fab.getLeft() <= lp.leftMargin) {

                offsetLR = -padding.left;
            }
            if (fab.getBottom() >= parent.getBottom() - lp.bottomMargin) {

                offsetTB = padding.bottom;
            } else if (fab.getTop() <= lp.topMargin) {

                offsetTB = -padding.top;
            }

            fab.offsetTopAndBottom(offsetTB);
            fab.offsetLeftAndRight(offsetLR);
        }
    }*/

    @Override
    public void onNestedScroll(CoordinatorLayout coordinatorLayout, FabSpeedDial child, View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
        if(dyConsumed > 0 && child.getVisibility() == View.VISIBLE){
            child.hide();
        } else if(dyConsumed < 0 && child.getVisibility() == View.GONE){
            child.show();
        }
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout, FabSpeedDial child, View directTargetChild, View target, int nestedScrollAxes) {
        return nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL;
    }
}