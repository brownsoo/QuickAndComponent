package com.hansoolabs.and.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by brownsoo on 2017. 8. 23..
 */

public abstract class AbsRecyclerViewScroller extends FrameLayout implements RecyclerViewScroller {

    private RecyclerViewCalculator recyclerViewCalculator;
    private ScrollerCalculator scrollerCalculator;
    private RecyclerView recyclerView;

    public AbsRecyclerViewScroller(
            @NonNull Context context) {
        super(context);
        init(context, null, 0);
    }

    public AbsRecyclerViewScroller(@NonNull Context context,
                                   @Nullable
                                           AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public AbsRecyclerViewScroller(@NonNull Context context,
                                   @Nullable AttributeSet attrs,
                                   @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AbsRecyclerViewScroller(@NonNull Context context,
                                   @Nullable AttributeSet attrs,
                                   @AttrRes int defStyleAttr,
                                   @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr);
    }

    public abstract void onScrollProgressChanged(float scrollProgress);

    public abstract RecyclerViewCalculator createRecyclerViewCalculator();

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        recyclerViewCalculator = createRecyclerViewCalculator();
        setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return onScrollerTouched(event);
            }
        });
    }

    public boolean onScrollerTouched(MotionEvent event) {
        if (scrollerCalculator != null) {
            scrollTo(scrollerCalculator.calculateScrollProgress(event), true);
            return true;
        }
        return false;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (scrollerCalculator == null) {
            scrollerCalculator = new ScrollerCalculator(top, top + getHeight());
        } else {
            scrollerCalculator.updateScrollBounds(top, top + getHeight());
        }
    }

    @Override
    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        recyclerView.addOnScrollListener(getOnScrollListener());
    }

    @Override
    public RecyclerView.OnScrollListener getOnScrollListener() {
        return new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (Math.abs(dy) > 0) {
                    float progress = recyclerViewCalculator.calculateScrollProgress(recyclerView);
                    onScrollProgressChanged(progress);
                }
            }
        };
    }

    @Override
    public void scrollTo(float scrollProgress, boolean fromTouch) {
        if (recyclerView != null) {
            int position = recyclerViewCalculator
                    .calculatePositionFromScrollProgress(recyclerView, scrollProgress);
            recyclerView.scrollToPosition(position);
        }
    }

    @Nullable
    protected RecyclerView getRecyclerView() {
        return recyclerView;
    }

    @Nullable
    protected ScrollerCalculator getScrollerCalculator() {
        return scrollerCalculator;
    }

    @Nullable
    protected RecyclerViewCalculator getRecyclerViewCalculator() {
        return recyclerViewCalculator;
    }
}
