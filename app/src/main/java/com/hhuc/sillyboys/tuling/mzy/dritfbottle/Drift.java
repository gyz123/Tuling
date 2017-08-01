package com.hhuc.sillyboys.tuling.mzy.dritfbottle;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.hhuc.sillyboys.tuling.R;

/**
 * Created by mzy on 2017/8/1.
 */

public class Drift extends Activity{

    private Context mContext;
    private ImageView bottle;

    MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drift);


        mContext = this;

        bottle = (ImageView) findViewById(R.id.dbotton);
        drift(bottle);


    }

    public void drift(View view){

        //播放丢东西的音频
        MediaManager.playRaw(mContext, R.raw.driftbottle);

        DisplayMetrics dm =getResources().getDisplayMetrics();
        int w_screen = dm.widthPixels;
        int h_screen = dm.heightPixels;
        Log.i("my", "屏幕尺寸：宽度 = " + w_screen + ";高度 = " + h_screen + ";密度 = " + dm.densityDpi);
        //瓶子旋转缩小动画
        ObjectAnimator animator1 = ObjectAnimator.ofFloat(view, "translationX", 0f , w_screen*0.6f);
        ObjectAnimator animator2 = ObjectAnimator.ofFloat(view, "translationY", 0f , -h_screen*0.5f);
        ObjectAnimator animator3 = ObjectAnimator.ofFloat(view, "rotation", 0f , 1080f);
        // ObjectAnimator animator4 = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        ObjectAnimator animator5 = ObjectAnimator.ofFloat(view, "scaleX", 3f, 0f, 0f);
        ObjectAnimator animator6 = ObjectAnimator.ofFloat(view, "scaleY", 3f, 0f, 0f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animator1,animator2,animator3,animator5,animator6);
        set.setDuration(2500);
        set.start();
        //结束触发水花帧动画
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                upSpray();
                MediaManager.playRaw(mContext, R.raw.spray);
                Toast.makeText(Drift.this, "丢了一个漂流瓶", Toast.LENGTH_SHORT).show();
            }
        });

    }

    /**
     * 水花帧动画
     */
    public void upSpray(){
        ImageView imageView = (ImageView) findViewById(R.id.mySpray);
        imageView.setImageResource(R.drawable.spray);
        AnimationDrawable animationDrawable = (AnimationDrawable) imageView.getDrawable();
        animationDrawable.start();
    }

    public void back_drift(View view){
        mContext = this;
        Intent intent = new Intent(mContext, Drift_main.class);
        mContext.startActivity(intent);
    }

}
