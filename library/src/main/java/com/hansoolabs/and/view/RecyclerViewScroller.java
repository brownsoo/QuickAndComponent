package com.hansoolabs.and.view;

import android.support.v7.widget.RecyclerView;

/**
 * Set RecyclerView, Scroll it
 * Created by brownsoo on 2017. 8. 23..
 */

public interface RecyclerViewScroller {
    void setRecyclerView(RecyclerView recyclerView);
    RecyclerView.OnScrollListener getOnScrollListener();
    void scrollTo(float scrollProgress, boolean fromTouch);
}
