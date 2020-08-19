package com.rhino.camera;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.rhino.log.LogUtils;


/**
 * Focus view
 *
 * @author LuoLin
 * @since Create on 2019/6/14.
 **/
public class FocusView extends View {

    private final String TAG = this.getClass().getSimpleName();
    private int radiusOuter, radiusInner, strokeWidth;
    private int colorSuccess = 0xFF13CE67;
    private int colorFailed = 0xFFCE132C;
    private int colorNormal = 0xFFE7D5FA;
    private int colorCurrent = 0xFFE7D5FA;
    private int previewWidth;
    private int previewHeight;
    private RectF outerRectF, innerRectF;
    private Paint paint;
    private ObjectAnimator animator;

    public FocusView(Context context) {
        this(context, null);
    }

    public FocusView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FocusView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        radiusOuter = dip2px(context, 26);
        radiusInner = dip2px(context, 14);
        strokeWidth = dip2px(context, 2);

        outerRectF = new RectF(strokeWidth, strokeWidth,
                radiusOuter * 2 - strokeWidth, radiusOuter * 2 - strokeWidth);
        innerRectF = new RectF(radiusOuter - radiusInner, radiusOuter - radiusInner,
                radiusOuter + radiusInner, radiusOuter + radiusInner);

        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(strokeWidth);

        initAnimation();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(radiusOuter * 2, radiusOuter * 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawCircle(canvas, colorCurrent);
    }

    private void drawCircle(Canvas canvas, int color) {
        paint.setColor(color);
        for (int i = 0; i < 4; i++) {
            canvas.drawArc(outerRectF, 90 * i + 5, 80, false, paint);
            canvas.drawArc(innerRectF, 90 * i + 50, 80, false, paint);
        }
    }

    public void startFocus() {
        //this.setRotation(0);
        this.setVisibility(VISIBLE);
        colorCurrent = colorNormal;
        invalidate();
        setAnimator(0, 90, 500).start();
    }

    public void focusSuccess() {
        colorCurrent = colorSuccess;
        invalidate();
        setAnimator(90, 0, 200).start();
    }

    public void focusFailed() {
        colorCurrent = colorFailed;
        invalidate();
        setAnimator(180, 0, 200).start();
    }

    public void hideFocusView() {
        this.setVisibility(GONE);
    }

    public void postDelayHideFocusView() {
        postDelayHideFocusView(1000);
    }

    public void postDelayHideFocusView(int delayTime) {
        removeCallbacks(delayHideRunnable);
        postDelayed(delayHideRunnable, delayTime);
    }

    private Runnable delayHideRunnable = new Runnable() {

        @Override
        public void run() {
            hideFocusView();
        }
    };

    public ObjectAnimator setAnimator(float from, float to, long duration) {
        //ObjectAnimator animator = ObjectAnimator.ofFloat(view, "rotation", from, to);
        animator.cancel();
        animator.setFloatValues(from, to);
        animator.setDuration(duration);
        return animator;
    }

    private void initAnimation() {
        animator = new ObjectAnimator();
        animator.setTarget(this);
        animator.setPropertyName("rotation");
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
    }

    public void moveToPosition(float x, float y) {
        x -= radiusOuter;
        y -= radiusOuter;
        this.setTranslationX(x);
        this.setTranslationY(y);
        this.setVisibility(VISIBLE);
        colorCurrent = colorNormal;
        invalidate();
    }

    public void resetToDefaultPosition() {
        int x = previewWidth / 2 - radiusOuter;
        int y = previewHeight / 2 - radiusOuter;
        this.setTranslationX(x);
        this.setTranslationY(y);
    }

    public void initFocusArea(int width, int height) {
        previewWidth = width;
        previewHeight = height;
        LogUtils.d("init focus view:" + previewWidth + "x" + previewHeight);
        resetToDefaultPosition();
    }

    public static RectF rectToRectF(Rect r) {
        return new RectF(r.left, r.top, r.right, r.bottom);
    }

    public static int dip2px(Context ctx, float dpValue) {
        final float scale = ctx.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
