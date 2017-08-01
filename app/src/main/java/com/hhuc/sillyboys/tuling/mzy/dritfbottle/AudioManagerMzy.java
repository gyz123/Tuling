package com.hhuc.sillyboys.tuling.mzy.dritfbottle;

import android.media.MediaRecorder;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Created by mzy on 2017/8/1.
 */

public class AudioManagerMzy {

    private MediaRecorder mMediaRecorder;
    private String mDir;//文件夹名
    private String mCurrentFilePath;//文件路径

    //单例模式
    private static AudioManagerMzy mInstance;

    private boolean isPrepared;//标志是否准备完毕

    private AudioManagerMzy(String dir){
        mDir = dir;
    }


    /**
     * 回调准备完毕
     */
    public interface AudioStateListener{
        void wellPrepare();
    }

    public AudioStateListener mListener;

    public void setOnAudioStateListener(AudioStateListener listener){
        mListener = listener;
    }

    //单例模式
    public static AudioManagerMzy getInstance(String dir){
        if (mInstance == null){
            synchronized (AudioManagerMzy.class){
                if(mInstance == null){
                    mInstance = new AudioManagerMzy(dir);
                }
            }
        }
        return mInstance;
    }

    //准备
    public void prepareAudio(){
        //查找文件夹
        File dir = new File(mDir);
        if (!dir.exists()){
            dir.mkdirs();
        }

        String fileName = generateFileName();
        File file = new File(dir, fileName);

        mCurrentFilePath = file.getAbsolutePath();
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setOutputFile(file.getAbsolutePath());//设置输出文件


        //设置音频源为麦克风
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //设置音频输出格式
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
        //设置音频编码amr
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            //准备结束
            isPrepared = true;
            //完成准备通知
            if (mListener != null){
                mListener.wellPrepare();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 随机生成文件的名称，Universally Unique Identifier
     * @return
     */
    private String generateFileName() {
        return UUID.randomUUID().toString() + ".amr";
    }

    /**
     * 音量等级
     */
    public int getVoiceLevel(int maxLevel){
        if(isPrepared){
            try {
                //MediaRecorder.getMaxAmplitude() 1-32767
                return maxLevel * mMediaRecorder.getMaxAmplitude()/32768 + 1;
            }catch (Exception e){

            }

        }
        return 1;//默认返回1
    }

    /**
     * 释放资源
     */
    public void release(){
        mMediaRecorder.stop();
        mMediaRecorder.release();
        mMediaRecorder = null;
    }

    /**
     * 取消
     */
    public void cancel(){

        release();
        if(mCurrentFilePath != null){
            File file = new File(mCurrentFilePath);
            file.delete();
            mCurrentFilePath = null;
        }
    }

    /**
     * 获得当前文件路径
     * @return
     */
    public String getCurrentFilePath() {
        return mCurrentFilePath;
    }
}
