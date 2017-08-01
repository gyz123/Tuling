package com.hhuc.sillyboys.tuling.mzy.dritfbottle;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.hhuc.sillyboys.tuling.R;

import java.io.File;

/**
 * Created by mzy on 2017/8/1.
 */

public class GetBottle extends Activity{

    private Context mContext;
    private ImageView bottle;

    private ImageView voice_dialog;

    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.getbottle);

        mContext = this;

        bottle = (ImageView) findViewById(R.id.getmybottle);
        voice_dialog = (ImageView) findViewById(R.id.id_recorder_dialog_voice);

        getBottle(bottle);

        //播放海浪的音频
        MediaManager.playRaw(mContext, R.raw.getbottle);

        bottle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String fileRoot = Environment.getExternalStorageDirectory()+"/imooc_recorder_audios";
                Log.d("fileGetRandom", "fileRoot 是" + fileRoot);
                File myRoot = new File(fileRoot);
                final File[] files = myRoot.listFiles();
                if(files.length == 0){
                    Log.d("fileGetRandom", "getRandomFile: 不存在的");
                    Toast.makeText(mContext, "暂时没有录音哦", Toast.LENGTH_SHORT).show();
                    return;
                }
                final int random = (int)(Math.random() * files.length);

                //Dialog
                final DialogManager_Speaker dialogManager_speaker = new DialogManager_Speaker(mContext);
                dialogManager_speaker.playingSound();

                MediaManager.playSound(files[random]+"", new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Toast.makeText(GetBottle.this, "放完了,回去扔一个试试", Toast.LENGTH_SHORT).show();
                        dialogManager_speaker.dismissDialog();//隐藏对话框
                        hide_bottle(bottle);
                    }
                });
            }
        });
    }

    /**
     * 显示漂流瓶动画
     * @param view
     */
    public void getBottle(View view){
        ObjectAnimator animator1 = ObjectAnimator.ofFloat(view, "scaleX", 0f, 3f, 3f);
        ObjectAnimator animator2 = ObjectAnimator.ofFloat(view, "scaleY", 0f, 3f, 3f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animator1,animator2);
        set.setDuration(2500);
        set.start();
        Toast.makeText(GetBottle.this, "海里出现了一个漂流瓶", Toast.LENGTH_SHORT).show();
    }

    /**
     * 返回主界面
     * @param view
     */
    public void back_get(View view){
        mContext = this;
        Intent intent = new Intent(mContext, Drift_main.class);
        mContext.startActivity(intent);
    }

    public void hide_bottle(final View view){
        ObjectAnimator animator1 = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(animator1);
        set.setDuration(2000);
        set.start();
        set.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                view.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
    }

}
