package com.hansoolabs.and.view;

import android.view.MotionEvent;

/**
 * Convert scrollProgress ratio with y position Or versus
 * Created by brownsoo on 2017. 8. 23..
 */

public class ScrollerCalculator {
    private float minScrollY;
    private float maxScrollY;
    public ScrollerCalculator(float minScrollY, float maxScrollY) {
        this.minScrollY = minScrollY;
        this.maxScrollY = maxScrollY;
    }

    public void updateScrollBounds(float minScrollY, float maxScrollY) {
        this.minScrollY = minScrollY;
        this.maxScrollY = maxScrollY;
    }

    public float calculateYFromScrollProgress(float scrollProgress) {
        return (maxScrollY - minScrollY) * scrollProgress + minScrollY;
    }

    public float calculateScrollProgress(MotionEvent event) {
        float height = maxScrollY - minScrollY;
        if (height == 0) {
            return 0.0f;
        }
        float y = event.getY();
        y = Math.min(Math.max(minScrollY, y), maxScrollY) - minScrollY;
        return y / height;
    }
}
