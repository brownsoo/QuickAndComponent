package com.hansoolabs.and.view;

import android.support.design.widget.FloatingActionButton;
import android.view.View;

import com.hansoolabs.and.utils.UiUtil;

/**
 * Created by brownsoo on 2017. 8. 23..
 */

public final class ScrollToTopFabManager {

    private static final float THRESHOLD = 50f; // in dp
    private final FloatingActionButton fab;
    private final int threshold;

    public ScrollToTopFabManager(FloatingActionButton fab) {
        this.fab = fab;
        threshold = UiUtil.dp2px(THRESHOLD);
    }

    public void onScroll(int scrollTo, int dy) {
        if (fab.getVisibility() == View.VISIBLE && scrollTo < threshold) {
            fab.hide(new FloatingActionButton.OnVisibilityChangedListener() {
                @Override
                public void onHidden(FloatingActionButton fab) {
                    super.onHidden(fab);
                    fab.setVisibility(View.INVISIBLE);
                }
            });
        } else if (scrollTo >= threshold && fab.getVisibility() != View.VISIBLE) {
            fab.show();
        }
    }
}
