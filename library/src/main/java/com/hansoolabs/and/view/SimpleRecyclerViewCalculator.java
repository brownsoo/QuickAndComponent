package com.hansoolabs.and.view;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;

/**
 * Created by brownsoo on 2017. 8. 23..
 */

public class SimpleRecyclerViewCalculator implements RecyclerViewCalculator {

    public int calculatePositionFromScrollProgress(@NonNull RecyclerView recyclerView,
                                                   float progress) {
        RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
        return Math.round((adapter.getItemCount() - 1) * progress);
    }

    public float calculateScrollProgress(@NonNull RecyclerView recyclerView) {
        int scrollTop = recyclerView.computeVerticalScrollOffset() +
                recyclerView.computeHorizontalScrollExtent();
        int maxScrollTop = recyclerView.computeVerticalScrollRange();
        return scrollTop / (float) maxScrollTop;
    }
}

