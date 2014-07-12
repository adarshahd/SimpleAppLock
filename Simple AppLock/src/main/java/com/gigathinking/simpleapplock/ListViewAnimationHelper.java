package com.gigathinking.simpleapplock;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import java.util.HashMap;

public class ListViewAnimationHelper {

    BaseAdapter adapter;
    AbsListView listView;
    HashMap<Long, Integer> mItemIdTopMap = new HashMap<Long, Integer>();
    private static final int MOVE_DURATION = 150;

// ================================================================================
// Constructor
// ================================================================================

    public ListViewAnimationHelper(BaseAdapter adapter, AbsListView listView) {
        this.adapter = adapter;
        this.listView = listView;
    }

// ================================================================================
// ListView row animation helper
// ================================================================================

    /**
     * This method animates all other views in the ListView container (not including ignoreView)
     * into their final positions. It is called after ignoreView has been removed from the
     * adapter, but before layout has been run. The approach here is to figure out where
     * everything is now, then allow layout to run, then figure out where everything is after
     * layout, and then to run animations between all of those start/end positions.
     */

    public void animateRemoval(final AbsListView listview, View viewToRemove) {
        int firstVisiblePosition = listview.getFirstVisiblePosition();
        for (int i = 0; i < listview.getChildCount(); ++i) {
            View child = listview.getChildAt(i);
            if (child != viewToRemove) {
                int position = firstVisiblePosition + i;
                long itemId = adapter.getItemId(position);
                mItemIdTopMap.put(itemId, child.getTop());
            }
        }
        // Delete the item from the adapter
        adapter.notifyDataSetChanged();

        final ViewTreeObserver observer = listview.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                boolean firstAnimation = true;
                int firstVisiblePosition = listview.getFirstVisiblePosition();
                for (int i = 0; i < listview.getChildCount(); ++i) {
                    final View child = listview.getChildAt(i);
                    int position = firstVisiblePosition + i;
                    long itemId = adapter.getItemId(position);
                    Integer startTop = mItemIdTopMap.get(itemId);
                    int top = child.getTop();
                    if (startTop == null) {
                        // Animate new views along with the others. The catch is that they did not
                        // exist in the start state, so we must calculate their starting position
                        // based on whether they're coming in from the bottom (i > 0) or top.
                        int childHeight = child.getHeight();
                        startTop = top + (i > 0 ? childHeight : -childHeight);
                    }
                    int delta = startTop - top;
                    if (delta != 0) {
                        Runnable endAction = firstAnimation ?
                                new Runnable() {
                                    public void run() {
                                        listView.setEnabled(true);
                                    }
                                } : null;
                        firstAnimation = false;
                        moveView(child, 0, 0, delta, 0, endAction);
                    }
                }
                mItemIdTopMap.clear();
                return true;
            }
        });
    }

// ================================================================================
// Interface declaration
// ================================================================================

    /**
     * Utility, to avoid having to implement every method in AnimationListener in
     * every implementation class
     */
    static class AnimationListenerAdapter implements AnimationListener {

        @Override
        public void onAnimationEnd(Animation animation) {
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }

        @Override
        public void onAnimationStart(Animation animation) {
        }
    }

// ================================================================================
// Honeycomb support
// ================================================================================

    /**
     * Returns true if the current runtime is Honeycomb or later
     */
    private boolean isRuntimePostGingerbread() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB;
    }

    /**
     * Animate a view between start and end X/Y locations, using either old (pre-3.0) or
     * new animation APIs.
     */
    @SuppressLint("NewApi")
    private void moveView(View view, float startX, float endX, float startY, float endY, Runnable endAction) {
        final Runnable finalEndAction = endAction;
        if (isRuntimePostGingerbread()) {
            view.animate().setDuration(MOVE_DURATION);
            if (startX != endX) {
                ObjectAnimator anim = ObjectAnimator.ofFloat(view, View.TRANSLATION_X, startX, endX);
                anim.setDuration(MOVE_DURATION);
                anim.start();
                setAnimatorEndAction(anim, endAction);
                endAction = null;
            }
            if (startY != endY) {
                ObjectAnimator anim = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, startY, endY);
                anim.setDuration(MOVE_DURATION);
                anim.start();
                setAnimatorEndAction(anim, endAction);
            }
        } else {
            TranslateAnimation translator = new TranslateAnimation(startX, endX, startY, endY);
            translator.setDuration(MOVE_DURATION);
            view.startAnimation(translator);
            if (endAction != null) {
                view.getAnimation().setAnimationListener(new AnimationListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        finalEndAction.run();
                    }
                });
            }
        }
    }

    @SuppressLint("NewApi")
    private void setAnimatorEndAction(Animator animator, final Runnable endAction) {
        if (endAction != null) {
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    endAction.run();
                }
            });
        }
    }

}
