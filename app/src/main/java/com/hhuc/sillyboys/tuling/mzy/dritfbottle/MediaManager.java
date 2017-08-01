package com.hhuc.sillyboys.tuling.mzy.dritfbottle;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

/**
 * Created by mzy on 2017/8/1.
 */

public class MediaManager {

    private static MediaPlayer mMediaPlayer;
    private static boolean isPause;

    /**
     * 播放音乐
     * @param filePath
     * @param onCompletionListener
     */
    public static void playSound(String filePath, MediaPlayer.OnCompletionListener onCompletionListener){
        if(mMediaPlayer == null){
            mMediaPlayer = new MediaPlayer();

            //设置error监听器
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mMediaPlayer.reset();
                    return false;
                }
            });
        }else{
            mMediaPlayer.reset();
        }

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setOnCompletionListener(onCompletionListener);
        try {
            mMediaPlayer.setDataSource(filePath);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 暂停播放
     */
    public static void pause(){
        if(mMediaPlayer != null && mMediaPlayer.isPlaying()){
            //正在播放的时候
            mMediaPlayer.pause();
            isPause = true;
        }
    }

    /**
     * 当前是isPause的状态
     */
    public static void resume(){
        if(mMediaPlayer != null && isPause){
            mMediaPlayer.start();
            isPause = false;
        }
    }

    /**
     * 释放资源
     */
    public static void release(){
        if(mMediaPlayer != null){
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    /**
     *
     */
    public static void playRaw(Context mContext, int id){
        Log.d("playRwa", "playRaw: 执行了");
        //播放丢东西的音频
        if(mMediaPlayer == null){
            mMediaPlayer = new MediaPlayer();

            //设置error监听器
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    mMediaPlayer.reset();
                    Log.d("playRaw", "onError: 出错了");
                    return false;
                }
            });
        }else{
            mMediaPlayer.reset();
        }
        mMediaPlayer = MediaPlayer.create(mContext, id);
        if(mMediaPlayer != null){
            mMediaPlayer.start();
            Log.d("playRaw", "playRaw: " + id);
        }
        //mMediaPlayer.release();
    }
}
