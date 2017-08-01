package com.hhuc.sillyboys.tuling.mzy.dritfbottle;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.hhuc.sillyboys.tuling.R;

/**
 * Created by mzy on 2017/8/1.
 */

public class Drift_main extends Activity {

    private Context mContext;
    private ImageView drift;
    private ImageView get;
    private ImageView board;
    private AudioRecorderImageView audioRecorderImageView;
    private TextView text1;
    private TextView text2;

    private boolean voice_flag = false;//能进行隐藏图标的flag，防止在未显示声音图标的状态下播放动画

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drift_main_diy);

        drift = (ImageView) findViewById(R.id.driftbottle);
        get = (ImageView) findViewById(R.id.getbottle);
        board = (ImageView) findViewById(R.id.board);
        audioRecorderImageView = (AudioRecorderImageView) findViewById(R.id.id_recorder_button);
        text1 = (TextView) findViewById(R.id.textView);
        text2 = (TextView) findViewById(R.id.textView2);
        //audioRecorderImageView = (AudioRecorderImageView) findViewById(R.id.id_recorder_button);
        audioRecorderImageView.setVisibility(View.INVISIBLE);

        mContext = this;//上下文

        //6.0+版本还需要动态获取权限
        if (Build.VERSION.SDK_INT >= 23) {
            int REQUEST_CODE_CONTACT = 101;
            String[] permissions = {Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
            //验证是否许可权限
            for (String str : permissions) {
                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                    //申请权限
                    this.requestPermissions(permissions, REQUEST_CODE_CONTACT);
                    return;
                }
            }
        }


        get.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, GetBottle.class);
                mContext.startActivity(intent);
            }
        });

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final String TAG = "touch";
        View view = getCurrentFocus();
        Log.d(TAG, "按键范围外");
        float x = event.getRawX();
        float y = event.getRawY();
        Log.d(TAG, "x:" + x + ";y:" + y);
        if(voice_flag){
            hide_voice(view);
        }
        voice_flag = false;
        return super.onTouchEvent(event);
    }

    public void show_voice(View view){
        final String TAG = "show_voice";

//        voice.onVisibilityAggregated(true);//API>=24
//        常量值为0，意思是可见的
//        常量值为4，意思是不可见的
//        常量值为8，意思是不可见的，而且不占用布局空间


        //显示录音图标
        audioRecorderImageView.setVisibility(View.VISIBLE);
        drift.setVisibility(View.INVISIBLE);
        get.setVisibility(View.INVISIBLE);
        board.setVisibility(View.INVISIBLE);
        text1.setVisibility(View.INVISIBLE);
        text2.setVisibility(View.INVISIBLE);

        ObjectAnimator animator1 = ObjectAnimator.ofFloat(audioRecorderImageView, "scaleX", 0f, 1.1f, 1f);
        ObjectAnimator animator2 = ObjectAnimator.ofFloat(audioRecorderImageView, "scaleY", 0f, 1.1f, 1f);
        ObjectAnimator animator3 = ObjectAnimator.ofFloat(audioRecorderImageView, "translationY", 0f, -100f);

        //差值器
        animator1.setInterpolator(new BounceInterpolator());
        animator2.setInterpolator(new BounceInterpolator());
        animator3.setInterpolator(new BounceInterpolator());

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animator1,animator2,animator3);
        set.setDuration(618);
        set.start();

        //动画结束，设置flag
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                voice_flag = true;
            }
        });
    }

    public void hide_voice(View view){

        ObjectAnimator animator1 = ObjectAnimator.ofFloat(audioRecorderImageView, "scaleX", 1f, 0f, 0f);
        ObjectAnimator animator2 = ObjectAnimator.ofFloat(audioRecorderImageView, "scaleY", 1f, 0f, 0f);
        ObjectAnimator animator3 = ObjectAnimator.ofFloat(audioRecorderImageView, "translationY", 0f, 100f);
        final ObjectAnimator animator4 = ObjectAnimator.ofFloat(drift, "alpha", 0f, 1f, 1f);
        final ObjectAnimator animator5 = ObjectAnimator.ofFloat(get, "alpha", 0f, 1f, 1f);
        final ObjectAnimator animator6 = ObjectAnimator.ofFloat(board, "alpha", 0f, 1f, 1f);
        final ObjectAnimator animator7 = ObjectAnimator.ofFloat(text1, "alpha", 0f, 1f, 1f);
        final ObjectAnimator animator8 = ObjectAnimator.ofFloat(text2, "alpha", 0f, 1f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(animator1,animator2,animator3);
        set.setDuration(618);
        set.start();

        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //显示录音图标
                audioRecorderImageView.setVisibility(View.INVISIBLE);

                drift.setVisibility(View.VISIBLE);
                get.setVisibility(View.VISIBLE);
                board.setVisibility(View.VISIBLE);
                text1.setVisibility(View.VISIBLE);
                text2.setVisibility(View.VISIBLE);
                AnimatorSet set2 = new AnimatorSet();
                set2.playTogether(animator4,animator5,animator6,animator7,animator8);
                set2.setDuration(618);
                set2.start();
            }
        });
    }
}
