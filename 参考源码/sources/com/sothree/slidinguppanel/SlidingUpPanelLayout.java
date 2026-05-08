package com.sothree.slidinguppanel;

import android.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.constraintlayout.solver.widgets.analyzer.BasicMeasure;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.ViewCompat;
import com.sothree.slidinguppanel.ViewDragHelper;
import java.util.Iterator;
import java.util.List;

/* JADX INFO: loaded from: classes.dex */
public class SlidingUpPanelLayout extends ViewGroup {
    private static final float DEFAULT_ANCHOR_POINT = 1.0f;
    private static final boolean DEFAULT_CLIP_PANEL_FLAG = true;
    private static final int DEFAULT_FADE_COLOR = -1728053248;
    private static final int DEFAULT_MIN_FLING_VELOCITY = 400;
    private static final boolean DEFAULT_OVERLAY_FLAG = false;
    private static final int DEFAULT_PANEL_HEIGHT = 68;
    private static final int DEFAULT_PARALLAX_OFFSET = 0;
    private static final int DEFAULT_SHADOW_HEIGHT = 4;
    public static final String SLIDING_STATE = "sliding_state";
    private static final String TAG = "SlidingUpPanelLayout";
    private float mAnchorPoint;
    private boolean mClipPanel;
    private int mCoveredFadeColor;
    private final Paint mCoveredFadePaint;
    private final ViewDragHelper mDragHelper;
    private View mDragView;
    private int mDragViewResId;
    private View.OnClickListener mFadeOnClickListener;
    private boolean mFirstLayout;
    private float mInitialMotionX;
    private float mInitialMotionY;
    private boolean mIsScrollableViewHandlingTouch;
    private boolean mIsSlidingUp;
    private boolean mIsTouchEnabled;
    private boolean mIsUnableToDrag;
    private PanelState mLastNotDraggingSlideState;
    private View mMainView;
    private int mMinFlingVelocity;
    private boolean mOverlayContent;
    private int mPanelHeight;
    private final List<PanelSlideListener> mPanelSlideListeners;
    private int mParallaxOffset;
    private float mPrevMotionX;
    private float mPrevMotionY;
    private View mScrollableView;
    private ScrollableViewHelper mScrollableViewHelper;
    private int mScrollableViewResId;
    private final Drawable mShadowDrawable;
    private int mShadowHeight;
    private float mSlideOffset;
    private int mSlideRange;
    private PanelState mSlideState;
    private View mSlideableView;
    private final Rect mTmpRect;
    private static PanelState DEFAULT_SLIDE_STATE = PanelState.COLLAPSED;
    private static final int[] DEFAULT_ATTRS = {R.attr.gravity};

    public interface PanelSlideListener {
        void onPanelSlide(View view, float f);

        void onPanelStateChanged(View view, PanelState panelState, PanelState panelState2);
    }

    public enum PanelState {
        EXPANDED,
        COLLAPSED,
        ANCHORED,
        HIDDEN,
        DRAGGING
    }

    public static class SimplePanelSlideListener implements PanelSlideListener {
        @Override // com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener
        public void onPanelSlide(View view, float f) {
        }

        @Override // com.sothree.slidinguppanel.SlidingUpPanelLayout.PanelSlideListener
        public void onPanelStateChanged(View view, PanelState panelState, PanelState panelState2) {
        }
    }

    public SlidingUpPanelLayout(Context context) {
        this(context, null);
    }

    public SlidingUpPanelLayout(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    /* JADX WARN: Removed duplicated region for block: B:17:0x00e2  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public SlidingUpPanelLayout(android.content.Context r9, android.util.AttributeSet r10, int r11) {
        /*
            Method dump skipped, instruction units count: 334
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: com.sothree.slidinguppanel.SlidingUpPanelLayout.<init>(android.content.Context, android.util.AttributeSet, int):void");
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        int i = this.mDragViewResId;
        if (i != -1) {
            setDragView(findViewById(i));
        }
        int i2 = this.mScrollableViewResId;
        if (i2 != -1) {
            setScrollableView(findViewById(i2));
        }
    }

    public void setGravity(int i) {
        if (i != 48 && i != 80) {
            throw new IllegalArgumentException("gravity must be set to either top or bottom");
        }
        this.mIsSlidingUp = i == 80 ? DEFAULT_CLIP_PANEL_FLAG : false;
        if (this.mFirstLayout) {
            return;
        }
        requestLayout();
    }

    public void setCoveredFadeColor(int i) {
        this.mCoveredFadeColor = i;
        requestLayout();
    }

    public int getCoveredFadeColor() {
        return this.mCoveredFadeColor;
    }

    public void setTouchEnabled(boolean z) {
        this.mIsTouchEnabled = z;
    }

    public boolean isTouchEnabled() {
        if (!this.mIsTouchEnabled || this.mSlideableView == null || this.mSlideState == PanelState.HIDDEN) {
            return false;
        }
        return DEFAULT_CLIP_PANEL_FLAG;
    }

    public void setPanelHeight(int i) {
        if (getPanelHeight() == i) {
            return;
        }
        this.mPanelHeight = i;
        if (!this.mFirstLayout) {
            requestLayout();
        }
        if (getPanelState() == PanelState.COLLAPSED) {
            smoothToBottom();
            invalidate();
        }
    }

    protected void smoothToBottom() {
        smoothSlideTo(0.0f, 0);
    }

    public int getShadowHeight() {
        return this.mShadowHeight;
    }

    public void setShadowHeight(int i) {
        this.mShadowHeight = i;
        if (this.mFirstLayout) {
            return;
        }
        invalidate();
    }

    public int getPanelHeight() {
        return this.mPanelHeight;
    }

    public int getCurrentParallaxOffset() {
        int iMax = (int) (this.mParallaxOffset * Math.max(this.mSlideOffset, 0.0f));
        return this.mIsSlidingUp ? -iMax : iMax;
    }

    public void setParallaxOffset(int i) {
        this.mParallaxOffset = i;
        if (this.mFirstLayout) {
            return;
        }
        requestLayout();
    }

    public int getMinFlingVelocity() {
        return this.mMinFlingVelocity;
    }

    public void setMinFlingVelocity(int i) {
        this.mMinFlingVelocity = i;
    }

    public void addPanelSlideListener(PanelSlideListener panelSlideListener) {
        synchronized (this.mPanelSlideListeners) {
            this.mPanelSlideListeners.add(panelSlideListener);
        }
    }

    public void removePanelSlideListener(PanelSlideListener panelSlideListener) {
        synchronized (this.mPanelSlideListeners) {
            this.mPanelSlideListeners.remove(panelSlideListener);
        }
    }

    public void setFadeOnClickListener(View.OnClickListener onClickListener) {
        this.mFadeOnClickListener = onClickListener;
    }

    public void setDragView(View view) {
        View view2 = this.mDragView;
        if (view2 != null) {
            view2.setOnClickListener(null);
        }
        this.mDragView = view;
        if (view != null) {
            view.setClickable(DEFAULT_CLIP_PANEL_FLAG);
            this.mDragView.setFocusable(false);
            this.mDragView.setFocusableInTouchMode(false);
            this.mDragView.setOnClickListener(new View.OnClickListener() { // from class: com.sothree.slidinguppanel.SlidingUpPanelLayout.1
                @Override // android.view.View.OnClickListener
                public void onClick(View view3) {
                    if (SlidingUpPanelLayout.this.isEnabled() && SlidingUpPanelLayout.this.isTouchEnabled()) {
                        if (SlidingUpPanelLayout.this.mSlideState == PanelState.EXPANDED || SlidingUpPanelLayout.this.mSlideState == PanelState.ANCHORED) {
                            SlidingUpPanelLayout.this.setPanelState(PanelState.COLLAPSED);
                        } else if (SlidingUpPanelLayout.this.mAnchorPoint < 1.0f) {
                            SlidingUpPanelLayout.this.setPanelState(PanelState.ANCHORED);
                        } else {
                            SlidingUpPanelLayout.this.setPanelState(PanelState.EXPANDED);
                        }
                    }
                }
            });
        }
    }

    public void setDragView(int i) {
        this.mDragViewResId = i;
        setDragView(findViewById(i));
    }

    public void setScrollableView(View view) {
        this.mScrollableView = view;
    }

    public void setScrollableViewHelper(ScrollableViewHelper scrollableViewHelper) {
        this.mScrollableViewHelper = scrollableViewHelper;
    }

    public void setAnchorPoint(float f) {
        if (f <= 0.0f || f > 1.0f) {
            return;
        }
        this.mAnchorPoint = f;
        this.mFirstLayout = DEFAULT_CLIP_PANEL_FLAG;
        requestLayout();
    }

    public float getAnchorPoint() {
        return this.mAnchorPoint;
    }

    public void setOverlayed(boolean z) {
        this.mOverlayContent = z;
    }

    public boolean isOverlayed() {
        return this.mOverlayContent;
    }

    public void setClipPanel(boolean z) {
        this.mClipPanel = z;
    }

    public boolean isClipPanel() {
        return this.mClipPanel;
    }

    void dispatchOnPanelSlide(View view) {
        synchronized (this.mPanelSlideListeners) {
            Iterator<PanelSlideListener> it = this.mPanelSlideListeners.iterator();
            while (it.hasNext()) {
                it.next().onPanelSlide(view, this.mSlideOffset);
            }
        }
    }

    void dispatchOnPanelStateChanged(View view, PanelState panelState, PanelState panelState2) {
        synchronized (this.mPanelSlideListeners) {
            Iterator<PanelSlideListener> it = this.mPanelSlideListeners.iterator();
            while (it.hasNext()) {
                it.next().onPanelStateChanged(view, panelState, panelState2);
            }
        }
        sendAccessibilityEvent(32);
    }

    void updateObscuredViewVisibility() {
        int left;
        int right;
        int top;
        int bottom;
        if (getChildCount() == 0) {
            return;
        }
        int paddingLeft = getPaddingLeft();
        int width = getWidth() - getPaddingRight();
        int paddingTop = getPaddingTop();
        int height = getHeight() - getPaddingBottom();
        View view = this.mSlideableView;
        int i = 0;
        if (view == null || !hasOpaqueBackground(view)) {
            left = 0;
            right = 0;
            top = 0;
            bottom = 0;
        } else {
            left = this.mSlideableView.getLeft();
            right = this.mSlideableView.getRight();
            top = this.mSlideableView.getTop();
            bottom = this.mSlideableView.getBottom();
        }
        View childAt = getChildAt(0);
        int iMax = Math.max(paddingLeft, childAt.getLeft());
        int iMax2 = Math.max(paddingTop, childAt.getTop());
        int iMin = Math.min(width, childAt.getRight());
        int iMin2 = Math.min(height, childAt.getBottom());
        if (iMax >= left && iMax2 >= top && iMin <= right && iMin2 <= bottom) {
            i = 4;
        }
        childAt.setVisibility(i);
    }

    void setAllChildrenVisible() {
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.getVisibility() == 4) {
                childAt.setVisibility(0);
            }
        }
    }

    private static boolean hasOpaqueBackground(View view) {
        Drawable background = view.getBackground();
        if (background == null || background.getOpacity() != -1) {
            return false;
        }
        return DEFAULT_CLIP_PANEL_FLAG;
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.mFirstLayout = DEFAULT_CLIP_PANEL_FLAG;
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.mFirstLayout = DEFAULT_CLIP_PANEL_FLAG;
    }

    @Override // android.view.View
    protected void onMeasure(int i, int i2) {
        int i3;
        int i4;
        int iMakeMeasureSpec;
        int iMakeMeasureSpec2;
        int mode = View.MeasureSpec.getMode(i);
        int size = View.MeasureSpec.getSize(i);
        int mode2 = View.MeasureSpec.getMode(i2);
        int size2 = View.MeasureSpec.getSize(i2);
        if (mode != 1073741824 && mode != Integer.MIN_VALUE) {
            throw new IllegalStateException("Width must have an exact value or MATCH_PARENT");
        }
        if (mode2 != 1073741824 && mode2 != Integer.MIN_VALUE) {
            throw new IllegalStateException("Height must have an exact value or MATCH_PARENT");
        }
        int childCount = getChildCount();
        if (childCount != 2) {
            throw new IllegalStateException("Sliding up panel layout must have exactly 2 children!");
        }
        this.mMainView = getChildAt(0);
        View childAt = getChildAt(1);
        this.mSlideableView = childAt;
        if (this.mDragView == null) {
            setDragView(childAt);
        }
        if (this.mSlideableView.getVisibility() != 0) {
            this.mSlideState = PanelState.HIDDEN;
        }
        int paddingTop = (size2 - getPaddingTop()) - getPaddingBottom();
        int paddingLeft = (size - getPaddingLeft()) - getPaddingRight();
        for (int i5 = 0; i5 < childCount; i5++) {
            View childAt2 = getChildAt(i5);
            LayoutParams layoutParams = (LayoutParams) childAt2.getLayoutParams();
            if (childAt2.getVisibility() != 8 || i5 != 0) {
                if (childAt2 == this.mMainView) {
                    i3 = (this.mOverlayContent || this.mSlideState == PanelState.HIDDEN) ? paddingTop : paddingTop - this.mPanelHeight;
                    i4 = paddingLeft - (layoutParams.leftMargin + layoutParams.rightMargin);
                } else {
                    i3 = childAt2 == this.mSlideableView ? paddingTop - layoutParams.topMargin : paddingTop;
                    i4 = paddingLeft;
                }
                if (layoutParams.width == -2) {
                    iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(i4, Integer.MIN_VALUE);
                } else if (layoutParams.width == -1) {
                    iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(i4, BasicMeasure.EXACTLY);
                } else {
                    iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(layoutParams.width, BasicMeasure.EXACTLY);
                }
                if (layoutParams.height == -2) {
                    iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(i3, Integer.MIN_VALUE);
                } else {
                    if (layoutParams.weight > 0.0f && layoutParams.weight < 1.0f) {
                        i3 = (int) (i3 * layoutParams.weight);
                    } else if (layoutParams.height != -1) {
                        i3 = layoutParams.height;
                    }
                    iMakeMeasureSpec2 = View.MeasureSpec.makeMeasureSpec(i3, BasicMeasure.EXACTLY);
                }
                childAt2.measure(iMakeMeasureSpec, iMakeMeasureSpec2);
                View view = this.mSlideableView;
                if (childAt2 == view) {
                    this.mSlideRange = view.getMeasuredHeight() - this.mPanelHeight;
                }
            }
        }
        setMeasuredDimension(size, size2);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int childCount = getChildCount();
        if (this.mFirstLayout) {
            int i5 = C08342.f149x1974508[this.mSlideState.ordinal()];
            if (i5 == 1) {
                this.mSlideOffset = 1.0f;
            } else if (i5 == 2) {
                this.mSlideOffset = this.mAnchorPoint;
            } else if (i5 == 3) {
                this.mSlideOffset = computeSlideOffset(computePanelTopPosition(0.0f) + (this.mIsSlidingUp ? this.mPanelHeight : -this.mPanelHeight));
            } else {
                this.mSlideOffset = 0.0f;
            }
        }
        for (int i6 = 0; i6 < childCount; i6++) {
            View childAt = getChildAt(i6);
            LayoutParams layoutParams = (LayoutParams) childAt.getLayoutParams();
            if (childAt.getVisibility() != 8 || (i6 != 0 && !this.mFirstLayout)) {
                int measuredHeight = childAt.getMeasuredHeight();
                int iComputePanelTopPosition = childAt == this.mSlideableView ? computePanelTopPosition(this.mSlideOffset) : paddingTop;
                if (!this.mIsSlidingUp && childAt == this.mMainView && !this.mOverlayContent) {
                    iComputePanelTopPosition = computePanelTopPosition(this.mSlideOffset) + this.mSlideableView.getMeasuredHeight();
                }
                int i7 = layoutParams.leftMargin + paddingLeft;
                childAt.layout(i7, iComputePanelTopPosition, childAt.getMeasuredWidth() + i7, measuredHeight + iComputePanelTopPosition);
            }
        }
        if (this.mFirstLayout) {
            updateObscuredViewVisibility();
        }
        applyParallaxForCurrentSlideOffset();
        this.mFirstLayout = false;
    }

    /* JADX INFO: renamed from: com.sothree.slidinguppanel.SlidingUpPanelLayout$2 */
    static /* synthetic */ class C08342 {

        /* JADX INFO: renamed from: $SwitchMap$com$sothree$slidinguppanel$SlidingUpPanelLayout$PanelState */
        static final /* synthetic */ int[] f149x1974508;

        static {
            int[] iArr = new int[PanelState.values().length];
            f149x1974508 = iArr;
            try {
                iArr[PanelState.EXPANDED.ordinal()] = 1;
            } catch (NoSuchFieldError unused) {
            }
            try {
                f149x1974508[PanelState.ANCHORED.ordinal()] = 2;
            } catch (NoSuchFieldError unused2) {
            }
            try {
                f149x1974508[PanelState.HIDDEN.ordinal()] = 3;
            } catch (NoSuchFieldError unused3) {
            }
            try {
                f149x1974508[PanelState.COLLAPSED.ordinal()] = 4;
            } catch (NoSuchFieldError unused4) {
            }
        }
    }

    @Override // android.view.View
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (i2 != i4) {
            this.mFirstLayout = DEFAULT_CLIP_PANEL_FLAG;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:21:0x004c  */
    @Override // android.view.ViewGroup
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public boolean onInterceptTouchEvent(android.view.MotionEvent r9) {
        /*
            r8 = this;
            boolean r0 = r8.mIsScrollableViewHandlingTouch
            r1 = 0
            if (r0 != 0) goto La4
            boolean r0 = r8.isTouchEnabled()
            if (r0 != 0) goto Ld
            goto La4
        Ld:
            int r0 = androidx.core.view.MotionEventCompat.getActionMasked(r9)
            float r2 = r9.getX()
            float r3 = r9.getY()
            float r4 = r8.mInitialMotionX
            float r4 = r2 - r4
            float r4 = java.lang.Math.abs(r4)
            float r5 = r8.mInitialMotionY
            float r5 = r3 - r5
            float r5 = java.lang.Math.abs(r5)
            com.sothree.slidinguppanel.ViewDragHelper r6 = r8.mDragHelper
            int r6 = r6.getTouchSlop()
            r7 = 1
            if (r0 == 0) goto L85
            if (r0 == r7) goto L4c
            r2 = 2
            if (r0 == r2) goto L3b
            r2 = 3
            if (r0 == r2) goto L4c
            goto L9d
        L3b:
            float r0 = (float) r6
            int r0 = (r5 > r0 ? 1 : (r5 == r0 ? 0 : -1))
            if (r0 <= 0) goto L9d
            int r0 = (r4 > r5 ? 1 : (r4 == r5 ? 0 : -1))
            if (r0 <= 0) goto L9d
            com.sothree.slidinguppanel.ViewDragHelper r9 = r8.mDragHelper
            r9.cancel()
            r8.mIsUnableToDrag = r7
            return r1
        L4c:
            com.sothree.slidinguppanel.ViewDragHelper r0 = r8.mDragHelper
            boolean r0 = r0.isDragging()
            if (r0 == 0) goto L5a
            com.sothree.slidinguppanel.ViewDragHelper r0 = r8.mDragHelper
            r0.processTouchEvent(r9)
            return r7
        L5a:
            float r0 = (float) r6
            int r2 = (r5 > r0 ? 1 : (r5 == r0 ? 0 : -1))
            if (r2 > 0) goto L9d
            int r0 = (r4 > r0 ? 1 : (r4 == r0 ? 0 : -1))
            if (r0 > 0) goto L9d
            float r0 = r8.mSlideOffset
            r2 = 0
            int r0 = (r0 > r2 ? 1 : (r0 == r2 ? 0 : -1))
            if (r0 <= 0) goto L9d
            android.view.View r0 = r8.mSlideableView
            float r2 = r8.mInitialMotionX
            int r2 = (int) r2
            float r3 = r8.mInitialMotionY
            int r3 = (int) r3
            boolean r0 = r8.isViewUnder(r0, r2, r3)
            if (r0 != 0) goto L9d
            android.view.View$OnClickListener r0 = r8.mFadeOnClickListener
            if (r0 == 0) goto L9d
            r8.playSoundEffect(r1)
            android.view.View$OnClickListener r9 = r8.mFadeOnClickListener
            r9.onClick(r8)
            return r7
        L85:
            r8.mIsUnableToDrag = r1
            r8.mInitialMotionX = r2
            r8.mInitialMotionY = r3
            android.view.View r0 = r8.mDragView
            int r2 = (int) r2
            int r3 = (int) r3
            boolean r0 = r8.isViewUnder(r0, r2, r3)
            if (r0 != 0) goto L9d
            com.sothree.slidinguppanel.ViewDragHelper r9 = r8.mDragHelper
            r9.cancel()
            r8.mIsUnableToDrag = r7
            return r1
        L9d:
            com.sothree.slidinguppanel.ViewDragHelper r0 = r8.mDragHelper
            boolean r9 = r0.shouldInterceptTouchEvent(r9)
            return r9
        La4:
            com.sothree.slidinguppanel.ViewDragHelper r9 = r8.mDragHelper
            r9.abort()
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.sothree.slidinguppanel.SlidingUpPanelLayout.onInterceptTouchEvent(android.view.MotionEvent):boolean");
    }

    @Override // android.view.View
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (!isEnabled() || !isTouchEnabled()) {
            return super.onTouchEvent(motionEvent);
        }
        try {
            this.mDragHelper.processTouchEvent(motionEvent);
            return DEFAULT_CLIP_PANEL_FLAG;
        } catch (Exception unused) {
            return false;
        }
    }

    @Override // android.view.ViewGroup, android.view.View
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        int actionMasked = MotionEventCompat.getActionMasked(motionEvent);
        if (!isEnabled() || !isTouchEnabled() || (this.mIsUnableToDrag && actionMasked != 0)) {
            this.mDragHelper.abort();
            return super.dispatchTouchEvent(motionEvent);
        }
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        if (actionMasked == 0) {
            this.mIsScrollableViewHandlingTouch = false;
            this.mPrevMotionX = x;
            this.mPrevMotionY = y;
        } else if (actionMasked == 2) {
            float f = x - this.mPrevMotionX;
            float f2 = y - this.mPrevMotionY;
            this.mPrevMotionX = x;
            this.mPrevMotionY = y;
            if (Math.abs(f) > Math.abs(f2)) {
                return super.dispatchTouchEvent(motionEvent);
            }
            if (!isViewUnder(this.mScrollableView, (int) this.mInitialMotionX, (int) this.mInitialMotionY)) {
                return super.dispatchTouchEvent(motionEvent);
            }
            boolean z = this.mIsSlidingUp;
            if ((z ? 1 : -1) * f2 > 0.0f) {
                if (this.mScrollableViewHelper.getScrollableViewScrollPosition(this.mScrollableView, z) > 0) {
                    this.mIsScrollableViewHandlingTouch = DEFAULT_CLIP_PANEL_FLAG;
                    return super.dispatchTouchEvent(motionEvent);
                }
                if (this.mIsScrollableViewHandlingTouch) {
                    MotionEvent motionEventObtain = MotionEvent.obtain(motionEvent);
                    motionEventObtain.setAction(3);
                    super.dispatchTouchEvent(motionEventObtain);
                    motionEventObtain.recycle();
                    motionEvent.setAction(0);
                }
                this.mIsScrollableViewHandlingTouch = false;
                return onTouchEvent(motionEvent);
            }
            if (f2 * (z ? 1 : -1) < 0.0f) {
                if (this.mSlideOffset < 1.0f) {
                    this.mIsScrollableViewHandlingTouch = false;
                    return onTouchEvent(motionEvent);
                }
                if (!this.mIsScrollableViewHandlingTouch && this.mDragHelper.isDragging()) {
                    this.mDragHelper.cancel();
                    motionEvent.setAction(0);
                }
                this.mIsScrollableViewHandlingTouch = DEFAULT_CLIP_PANEL_FLAG;
                return super.dispatchTouchEvent(motionEvent);
            }
        } else if (actionMasked == 1 && this.mIsScrollableViewHandlingTouch) {
            this.mDragHelper.setDragState(0);
        }
        return super.dispatchTouchEvent(motionEvent);
    }

    private boolean isViewUnder(View view, int i, int i2) {
        if (view == null) {
            return false;
        }
        int[] iArr = new int[2];
        view.getLocationOnScreen(iArr);
        int[] iArr2 = new int[2];
        getLocationOnScreen(iArr2);
        int i3 = iArr2[0] + i;
        int i4 = iArr2[1] + i2;
        if (i3 < iArr[0] || i3 >= iArr[0] + view.getWidth() || i4 < iArr[1] || i4 >= iArr[1] + view.getHeight()) {
            return false;
        }
        return DEFAULT_CLIP_PANEL_FLAG;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public int computePanelTopPosition(float f) {
        View view = this.mSlideableView;
        int measuredHeight = view != null ? view.getMeasuredHeight() : 0;
        int i = (int) (f * this.mSlideRange);
        if (this.mIsSlidingUp) {
            return ((getMeasuredHeight() - getPaddingBottom()) - this.mPanelHeight) - i;
        }
        return (getPaddingTop() - measuredHeight) + this.mPanelHeight + i;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public float computeSlideOffset(int i) {
        int iComputePanelTopPosition = computePanelTopPosition(0.0f);
        return (this.mIsSlidingUp ? iComputePanelTopPosition - i : i - iComputePanelTopPosition) / this.mSlideRange;
    }

    public PanelState getPanelState() {
        return this.mSlideState;
    }

    public void setPanelState(PanelState panelState) {
        PanelState panelState2;
        if (this.mDragHelper.getViewDragState() == 2) {
            Log.d(TAG, "View is settling. Aborting animation.");
            this.mDragHelper.abort();
        }
        if (panelState == null || panelState == PanelState.DRAGGING) {
            throw new IllegalArgumentException("Panel state cannot be null or DRAGGING.");
        }
        if (isEnabled()) {
            if ((!this.mFirstLayout && this.mSlideableView == null) || panelState == (panelState2 = this.mSlideState) || panelState2 == PanelState.DRAGGING) {
                return;
            }
            if (this.mFirstLayout) {
                setPanelStateInternal(panelState);
                return;
            }
            if (this.mSlideState == PanelState.HIDDEN) {
                this.mSlideableView.setVisibility(0);
                requestLayout();
            }
            int i = C08342.f149x1974508[panelState.ordinal()];
            if (i == 1) {
                smoothSlideTo(1.0f, 0);
                return;
            }
            if (i == 2) {
                smoothSlideTo(this.mAnchorPoint, 0);
            } else if (i == 3) {
                smoothSlideTo(computeSlideOffset(computePanelTopPosition(0.0f) + (this.mIsSlidingUp ? this.mPanelHeight : -this.mPanelHeight)), 0);
            } else {
                if (i != 4) {
                    return;
                }
                smoothSlideTo(0.0f, 0);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void setPanelStateInternal(PanelState panelState) {
        PanelState panelState2 = this.mSlideState;
        if (panelState2 == panelState) {
            return;
        }
        this.mSlideState = panelState;
        dispatchOnPanelStateChanged(this, panelState2, panelState);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void applyParallaxForCurrentSlideOffset() {
        if (this.mParallaxOffset > 0) {
            ViewCompat.setTranslationY(this.mMainView, getCurrentParallaxOffset());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void onPanelDragged(int i) {
        if (this.mSlideState != PanelState.DRAGGING) {
            this.mLastNotDraggingSlideState = this.mSlideState;
        }
        setPanelStateInternal(PanelState.DRAGGING);
        this.mSlideOffset = computeSlideOffset(i);
        applyParallaxForCurrentSlideOffset();
        dispatchOnPanelSlide(this.mSlideableView);
        LayoutParams layoutParams = (LayoutParams) this.mMainView.getLayoutParams();
        int height = ((getHeight() - getPaddingBottom()) - getPaddingTop()) - this.mPanelHeight;
        if (this.mSlideOffset <= 0.0f && !this.mOverlayContent) {
            layoutParams.height = this.mIsSlidingUp ? i - getPaddingBottom() : ((getHeight() - getPaddingBottom()) - this.mSlideableView.getMeasuredHeight()) - i;
            if (layoutParams.height == height) {
                layoutParams.height = -1;
            }
            this.mMainView.requestLayout();
            return;
        }
        if (layoutParams.height == -1 || this.mOverlayContent) {
            return;
        }
        layoutParams.height = -1;
        this.mMainView.requestLayout();
    }

    @Override // android.view.ViewGroup
    protected boolean drawChild(Canvas canvas, View view, long j) {
        boolean zDrawChild;
        int iSave = canvas.save(2);
        View view2 = this.mSlideableView;
        if (view2 != null && view2 != view) {
            canvas.getClipBounds(this.mTmpRect);
            if (!this.mOverlayContent) {
                if (this.mIsSlidingUp) {
                    Rect rect = this.mTmpRect;
                    rect.bottom = Math.min(rect.bottom, this.mSlideableView.getTop());
                } else {
                    Rect rect2 = this.mTmpRect;
                    rect2.top = Math.max(rect2.top, this.mSlideableView.getBottom());
                }
            }
            if (this.mClipPanel) {
                canvas.clipRect(this.mTmpRect);
            }
            zDrawChild = super.drawChild(canvas, view, j);
            int i = this.mCoveredFadeColor;
            if (i != 0) {
                float f = this.mSlideOffset;
                if (f > 0.0f) {
                    this.mCoveredFadePaint.setColor((i & ViewCompat.MEASURED_SIZE_MASK) | (((int) ((((-16777216) & i) >>> 24) * f)) << 24));
                    canvas.drawRect(this.mTmpRect, this.mCoveredFadePaint);
                }
            }
        } else {
            zDrawChild = super.drawChild(canvas, view, j);
        }
        canvas.restoreToCount(iSave);
        return zDrawChild;
    }

    boolean smoothSlideTo(float f, int i) {
        if (isEnabled() && this.mSlideableView != null) {
            int iComputePanelTopPosition = computePanelTopPosition(f);
            ViewDragHelper viewDragHelper = this.mDragHelper;
            View view = this.mSlideableView;
            if (viewDragHelper.smoothSlideViewTo(view, view.getLeft(), iComputePanelTopPosition)) {
                setAllChildrenVisible();
                ViewCompat.postInvalidateOnAnimation(this);
                return DEFAULT_CLIP_PANEL_FLAG;
            }
        }
        return false;
    }

    @Override // android.view.View
    public void computeScroll() {
        ViewDragHelper viewDragHelper = this.mDragHelper;
        if (viewDragHelper == null || !viewDragHelper.continueSettling(DEFAULT_CLIP_PANEL_FLAG)) {
            return;
        }
        if (!isEnabled()) {
            this.mDragHelper.abort();
        } else {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override // android.view.View
    public void draw(Canvas canvas) {
        View view;
        int bottom;
        int bottom2;
        super.draw(canvas);
        if (this.mShadowDrawable == null || (view = this.mSlideableView) == null) {
            return;
        }
        int right = view.getRight();
        if (this.mIsSlidingUp) {
            bottom = this.mSlideableView.getTop() - this.mShadowHeight;
            bottom2 = this.mSlideableView.getTop();
        } else {
            bottom = this.mSlideableView.getBottom();
            bottom2 = this.mSlideableView.getBottom() + this.mShadowHeight;
        }
        this.mShadowDrawable.setBounds(this.mSlideableView.getLeft(), bottom, right, bottom2);
        this.mShadowDrawable.draw(canvas);
    }

    protected boolean canScroll(View view, boolean z, int i, int i2, int i3) {
        int i4;
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            int scrollX = view.getScrollX();
            int scrollY = view.getScrollY();
            for (int childCount = viewGroup.getChildCount() - 1; childCount >= 0; childCount--) {
                View childAt = viewGroup.getChildAt(childCount);
                int i5 = i2 + scrollX;
                if (i5 >= childAt.getLeft() && i5 < childAt.getRight() && (i4 = i3 + scrollY) >= childAt.getTop() && i4 < childAt.getBottom() && canScroll(childAt, DEFAULT_CLIP_PANEL_FLAG, i, i5 - childAt.getLeft(), i4 - childAt.getTop())) {
                    return DEFAULT_CLIP_PANEL_FLAG;
                }
            }
        }
        if (z && ViewCompat.canScrollHorizontally(view, -i)) {
            return DEFAULT_CLIP_PANEL_FLAG;
        }
        return false;
    }

    @Override // android.view.ViewGroup
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }

    @Override // android.view.ViewGroup
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
        return layoutParams instanceof ViewGroup.MarginLayoutParams ? new LayoutParams((ViewGroup.MarginLayoutParams) layoutParams) : new LayoutParams(layoutParams);
    }

    @Override // android.view.ViewGroup
    protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
        if ((layoutParams instanceof LayoutParams) && super.checkLayoutParams(layoutParams)) {
            return DEFAULT_CLIP_PANEL_FLAG;
        }
        return false;
    }

    @Override // android.view.ViewGroup
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override // android.view.View
    public Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putSerializable(SLIDING_STATE, this.mSlideState != PanelState.DRAGGING ? this.mSlideState : this.mLastNotDraggingSlideState);
        return bundle;
    }

    @Override // android.view.View
    public void onRestoreInstanceState(Parcelable parcelable) {
        if (parcelable instanceof Bundle) {
            Bundle bundle = (Bundle) parcelable;
            PanelState panelState = (PanelState) bundle.getSerializable(SLIDING_STATE);
            this.mSlideState = panelState;
            if (panelState == null) {
                panelState = DEFAULT_SLIDE_STATE;
            }
            this.mSlideState = panelState;
            parcelable = bundle.getParcelable("superState");
        }
        super.onRestoreInstanceState(parcelable);
    }

    private class DragHelperCallback extends ViewDragHelper.Callback {
        private DragHelperCallback() {
        }

        @Override // com.sothree.slidinguppanel.ViewDragHelper.Callback
        public boolean tryCaptureView(View view, int i) {
            if (SlidingUpPanelLayout.this.mIsUnableToDrag || view != SlidingUpPanelLayout.this.mSlideableView) {
                return false;
            }
            return SlidingUpPanelLayout.DEFAULT_CLIP_PANEL_FLAG;
        }

        @Override // com.sothree.slidinguppanel.ViewDragHelper.Callback
        public void onViewDragStateChanged(int i) {
            if (SlidingUpPanelLayout.this.mDragHelper == null || SlidingUpPanelLayout.this.mDragHelper.getViewDragState() != 0) {
                return;
            }
            SlidingUpPanelLayout slidingUpPanelLayout = SlidingUpPanelLayout.this;
            slidingUpPanelLayout.mSlideOffset = slidingUpPanelLayout.computeSlideOffset(slidingUpPanelLayout.mSlideableView.getTop());
            SlidingUpPanelLayout.this.applyParallaxForCurrentSlideOffset();
            if (SlidingUpPanelLayout.this.mSlideOffset == 1.0f) {
                SlidingUpPanelLayout.this.updateObscuredViewVisibility();
                SlidingUpPanelLayout.this.setPanelStateInternal(PanelState.EXPANDED);
            } else if (SlidingUpPanelLayout.this.mSlideOffset == 0.0f) {
                SlidingUpPanelLayout.this.setPanelStateInternal(PanelState.COLLAPSED);
            } else if (SlidingUpPanelLayout.this.mSlideOffset < 0.0f) {
                SlidingUpPanelLayout.this.setPanelStateInternal(PanelState.HIDDEN);
                SlidingUpPanelLayout.this.mSlideableView.setVisibility(4);
            } else {
                SlidingUpPanelLayout.this.updateObscuredViewVisibility();
                SlidingUpPanelLayout.this.setPanelStateInternal(PanelState.ANCHORED);
            }
        }

        @Override // com.sothree.slidinguppanel.ViewDragHelper.Callback
        public void onViewCaptured(View view, int i) {
            SlidingUpPanelLayout.this.setAllChildrenVisible();
        }

        @Override // com.sothree.slidinguppanel.ViewDragHelper.Callback
        public void onViewPositionChanged(View view, int i, int i2, int i3, int i4) {
            SlidingUpPanelLayout.this.onPanelDragged(i2);
            SlidingUpPanelLayout.this.invalidate();
        }

        @Override // com.sothree.slidinguppanel.ViewDragHelper.Callback
        public void onViewReleased(View view, float f, float f2) {
            int iComputePanelTopPosition;
            if (SlidingUpPanelLayout.this.mIsSlidingUp) {
                f2 = -f2;
            }
            if (f2 > 0.0f && SlidingUpPanelLayout.this.mSlideOffset <= SlidingUpPanelLayout.this.mAnchorPoint) {
                SlidingUpPanelLayout slidingUpPanelLayout = SlidingUpPanelLayout.this;
                iComputePanelTopPosition = slidingUpPanelLayout.computePanelTopPosition(slidingUpPanelLayout.mAnchorPoint);
            } else if (f2 > 0.0f && SlidingUpPanelLayout.this.mSlideOffset > SlidingUpPanelLayout.this.mAnchorPoint) {
                iComputePanelTopPosition = SlidingUpPanelLayout.this.computePanelTopPosition(1.0f);
            } else if (f2 < 0.0f && SlidingUpPanelLayout.this.mSlideOffset >= SlidingUpPanelLayout.this.mAnchorPoint) {
                SlidingUpPanelLayout slidingUpPanelLayout2 = SlidingUpPanelLayout.this;
                iComputePanelTopPosition = slidingUpPanelLayout2.computePanelTopPosition(slidingUpPanelLayout2.mAnchorPoint);
            } else if (f2 < 0.0f && SlidingUpPanelLayout.this.mSlideOffset < SlidingUpPanelLayout.this.mAnchorPoint) {
                iComputePanelTopPosition = SlidingUpPanelLayout.this.computePanelTopPosition(0.0f);
            } else if (SlidingUpPanelLayout.this.mSlideOffset >= (SlidingUpPanelLayout.this.mAnchorPoint + 1.0f) / 2.0f) {
                iComputePanelTopPosition = SlidingUpPanelLayout.this.computePanelTopPosition(1.0f);
            } else if (SlidingUpPanelLayout.this.mSlideOffset < SlidingUpPanelLayout.this.mAnchorPoint / 2.0f) {
                iComputePanelTopPosition = SlidingUpPanelLayout.this.computePanelTopPosition(0.0f);
            } else {
                SlidingUpPanelLayout slidingUpPanelLayout3 = SlidingUpPanelLayout.this;
                iComputePanelTopPosition = slidingUpPanelLayout3.computePanelTopPosition(slidingUpPanelLayout3.mAnchorPoint);
            }
            if (SlidingUpPanelLayout.this.mDragHelper != null) {
                SlidingUpPanelLayout.this.mDragHelper.settleCapturedViewAt(view.getLeft(), iComputePanelTopPosition);
            }
            SlidingUpPanelLayout.this.invalidate();
        }

        @Override // com.sothree.slidinguppanel.ViewDragHelper.Callback
        public int getViewVerticalDragRange(View view) {
            return SlidingUpPanelLayout.this.mSlideRange;
        }

        @Override // com.sothree.slidinguppanel.ViewDragHelper.Callback
        public int clampViewPositionVertical(View view, int i, int i2) {
            int iComputePanelTopPosition = SlidingUpPanelLayout.this.computePanelTopPosition(0.0f);
            int iComputePanelTopPosition2 = SlidingUpPanelLayout.this.computePanelTopPosition(1.0f);
            if (SlidingUpPanelLayout.this.mIsSlidingUp) {
                return Math.min(Math.max(i, iComputePanelTopPosition2), iComputePanelTopPosition);
            }
            return Math.min(Math.max(i, iComputePanelTopPosition), iComputePanelTopPosition2);
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        private static final int[] ATTRS = {R.attr.layout_weight};
        public float weight;

        public LayoutParams() {
            super(-1, -1);
            this.weight = 0.0f;
        }

        public LayoutParams(int i, int i2) {
            super(i, i2);
            this.weight = 0.0f;
        }

        public LayoutParams(int i, int i2, float f) {
            super(i, i2);
            this.weight = 0.0f;
            this.weight = f;
        }

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
            this.weight = 0.0f;
        }

        public LayoutParams(ViewGroup.MarginLayoutParams marginLayoutParams) {
            super(marginLayoutParams);
            this.weight = 0.0f;
        }

        public LayoutParams(LayoutParams layoutParams) {
            super((ViewGroup.MarginLayoutParams) layoutParams);
            this.weight = 0.0f;
        }

        public LayoutParams(Context context, AttributeSet attributeSet) {
            super(context, attributeSet);
            this.weight = 0.0f;
            TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, ATTRS);
            if (typedArrayObtainStyledAttributes != null) {
                this.weight = typedArrayObtainStyledAttributes.getFloat(0, 0.0f);
                typedArrayObtainStyledAttributes.recycle();
            }
        }
    }
}
