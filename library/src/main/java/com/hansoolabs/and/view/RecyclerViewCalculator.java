package com.hansoolabs.and.view;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

/**
 * Get position from scrollProgress or
 * get scrollProgress from position of RecyclerView item
 * Created by brownsoo on 2017. 8. 23..
 */

public interface RecyclerViewCalculator {
    int calculatePositionFromScrollProgress(
            @NonNull RecyclerView recyclerView,
            float progress);
    float calculateScrollProgress(
            @NonNull RecyclerView recyclerView);
}
