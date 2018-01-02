package com.hansoolabs.and.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by brownsoo on 2017. 8. 23..
 */

public final class IndexScrollerView extends AbsRecyclerViewScroller {

    public interface IndexIndicator {
        void moveToPosition(float scrollProgress);
        void show();
        void hide();
        boolean isShown();
        boolean isAvailable();
        void updateIndex(int current, int total);
    }

    @Nullable
    private IndexIndicator indexIndicator;

    public IndexScrollerView(@NonNull Context context) {
        super(context);
    }

    public IndexScrollerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public IndexScrollerView(@NonNull Context context,
                             @Nullable AttributeSet attrs,
                             @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public IndexScrollerView(@NonNull Context context,
                             @Nullable AttributeSet attrs,
                             @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setIndexIndicator(@Nullable IndexIndicator indexIndicator) {
        this.indexIndicator = indexIndicator;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        RecyclerView recyclerView = getRecyclerView();
        if (recyclerView != null && indexIndicator != null && indexIndicator.isShown()) {
            float scrollProgress = getRecyclerViewCalculator()
                    .calculateScrollProgress(recyclerView);
            onScrollProgressChanged(scrollProgress);
        }
    }

    @Override
    public void onScrollProgressChanged(float scrollProgress) {
        boolean handled = false;
        RecyclerView recyclerView = getRecyclerView();
        if (indexIndicator == null || !indexIndicator.isAvailable()) {
            if (indexIndicator != null && indexIndicator.isShown()) {
                indexIndicator.hide();
            }
            return;
        }
        if (recyclerView != null) {
            int total = 0;
            RecyclerView.Adapter<?> adapter = recyclerView.getAdapter();
            if (adapter != null) {
                total = adapter.getItemCount();
            }
            int firstVisibleIndex = RecyclerView.NO_POSITION;
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof LinearLayoutManager) {
                firstVisibleIndex = ((LinearLayoutManager) layoutManager)
                        .findFirstVisibleItemPosition();
            }
            if (total > 0 && firstVisibleIndex != RecyclerView.NO_POSITION) {
                if (!indexIndicator.isShown()) {
                    indexIndicator.show();
                }
                indexIndicator.moveToPosition(scrollProgress);
                indexIndicator.updateIndex(firstVisibleIndex, total);
                handled = true;
            }
        }
        if (!handled && indexIndicator.isShown()) {
            indexIndicator.hide();
        }
    }

    @Override
    public RecyclerViewCalculator createRecyclerViewCalculator() {
        return new SimpleRecyclerViewCalculator();
    }

    @Override
    public boolean onScrollerTouched(MotionEvent event) {
        return false;
    }
}
