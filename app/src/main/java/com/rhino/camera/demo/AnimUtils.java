package com.rhino.camera.demo;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;

/**
 * @author LuoLin
 * @since Create on 2019/8/14.
 **/
public class AnimUtils {

    /**
     * 旋转
     */
    public static void rotate(View view, int fromDegree, int toDegree, Animation.AnimationListener listener) {
        AnimationSet animationSet = new AnimationSet(true);
        RotateAnimation rotateAnimation = new RotateAnimation(fromDegree, toDegree,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotateAnimation.setDuration(500);
        animationSet.setFillAfter(true);
        animationSet.setAnimationListener(listener);
        animationSet.addAnimation(rotateAnimation);
        view.startAnimation(animationSet);
    }

    /**
     * 0-1动画
     */
    public static void startValueAnim(ValueAnimator.AnimatorUpdateListener listener) {
        ValueAnimator valueAnimator = new ValueAnimator();
        valueAnimator.setDuration(500);
        valueAnimator.setFloatValues(0, 1);
        valueAnimator.addUpdateListener(listener);
        valueAnimator.start();
    }

}
