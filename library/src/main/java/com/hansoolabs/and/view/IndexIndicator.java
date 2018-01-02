package com.hansoolabs.and.view;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import com.hansoolabs.and.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by vulpes on 2017. 6. 12..
 */

public class IndexIndicator implements IndexScrollerView.IndexIndicator {

    public interface Adapter {
        int getTotalSize();
        int getCurrentIndex(int adapterPosition, int totalAdapterSize);
        boolean isVisible();
    }

    private static final long FADE_ANIMATION_DURATION = 150;
    private static final long VISIBLE_TIMEOUT = 400;

    private final ViewGroup parent;
    private final View frame;
    private final Adapter adapter;
    private final TextView indexView;
    private final TextView totalView;
    private final Handler handler;

    private volatile long lastTouchedTime;
    private volatile Timer timer;

    private Animation animation;

    public IndexIndicator(View frame, Adapter adapter) {
        this.parent = (ViewGroup) frame.getParent();
        this.frame = frame;
        this.adapter = adapter;
        this.indexView = (TextView) frame.findViewById(R.id.index);
        this.totalView = (TextView) frame.findViewById(R.id.total);
        handler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void moveToPosition(float scrollProgress) {
        int height = parent.getHeight() - frame.getHeight();
        float y = scrollProgress * height;
        frame.setY(y);
        lastTouchedTime = System.currentTimeMillis();
    }

    private AlphaAnimation createAnimation(float from, float to) {
        AlphaAnimation anim = new AlphaAnimation(from, to);
        anim.setDuration(FADE_ANIMATION_DURATION);
        return anim;
    }

    @Override
    public void show() {
        lastTouchedTime = System.currentTimeMillis();
        startTimer();
        if (frame.getVisibility() != View.VISIBLE) {
            if (animation != null) {
                animation.cancel();
            }
            animation = createAnimation(frame.getAlpha(), 1.0f);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    frame.setAlpha(1.0f);
                    IndexIndicator.this.animation = null;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            frame.setVisibility(View.VISIBLE);
            frame.startAnimation(animation);
        }
    }

    @Override
    public void hide() {
        stopTimerIfExist();
        if (frame.getVisibility() != View.GONE) {
            if (animation != null) {
                animation.cancel();
            }
            frame.setVisibility(View.VISIBLE);
            animation = createAnimation(frame.getAlpha(), 0.0f);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    frame.setVisibility(View.GONE);
                    IndexIndicator.this.animation = null;
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            frame.startAnimation(animation);
        }
    }

    @Override
    public boolean isShown() {
        return frame.isShown();
    }

    @Override
    public boolean isAvailable() {
        return adapter.isVisible() && adapter.getTotalSize() > 0;
    }

    @Override
    public void updateIndex(int current, int total) {
        int index = adapter.getCurrentIndex(current, total);
        int size = adapter.getTotalSize();
        indexView.setText(String.valueOf(index));
        totalView.setText(String.valueOf(size));
    }

    private synchronized void startTimer() {
        stopTimerIfExist();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (lastTouchedTime + VISIBLE_TIMEOUT < System.currentTimeMillis()) {
                    stopTimerIfExist();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            hide();
                        }
                    });
                }
            }
        }, 0, 100);
    }

    private synchronized void stopTimerIfExist() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}
