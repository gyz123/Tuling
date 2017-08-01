package com.hhuc.sillyboys.tuling.mzy.dritfbottle;


import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import com.hhuc.sillyboys.tuling.R;

/**
 * Created by mzy on 2017/8/1.
 */

public class AudioRecorderImageView extends ImageView implements AudioManagerMzy.AudioStateListener{

    //状态标记
    private static final int STATE_NORMAL = 1;//默认状态
    private static final int STATE_RECORDING = 2;//正在录音
    private static final int STATE_WANT_TO_CANCEL = 3;//取消

    private static final int DISTANCE_Y_CANCEL = 50;//取消需要移动Y轴的距离

    //现在的状态
    private int mCurState = STATE_NORMAL;
    //录音标志位
    private boolean isRecording = false;

    private DialogManager mDialogManager;

    private AudioManagerMzy mAudioManager;

    private float mTime = 0;//录音计时

    //是否触发LongClick
    private boolean mReady;

    private Context mContext;

    //以下两个为构造方法
    public AudioRecorderImageView(Context context) {
        super(context);
    }

    public AudioRecorderImageView(final Context context, AttributeSet attrs) {
        super(context, attrs);
        mDialogManager = new DialogManager(getContext());

        String dir = Environment.getExternalStorageDirectory() + "/imooc_recorder_audios";
        mAudioManager = AudioManagerMzy.getInstance(dir);//创建单例
        mAudioManager.setOnAudioStateListener(this);

        mContext = context;//传入上下文

        setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mReady = true;

                mAudioManager.prepareAudio();
                return false;
            }
        });
    }



    /**
     * 录音完成后的回调
     */
    public interface AudioFinishRecorderListener{
        void onFinish(float seconds, String filePath);
    }

    private AudioFinishRecorderListener mListener;

    public void setAudioFinishRecorderListener(AudioFinishRecorderListener listener){
        mListener = listener;
    }

    /**
     * 获取音量大小的Runnable
     */
    private Runnable mGetVoiceLevelRunnable = new Runnable() {
        @Override
        public void run() {
            while(isRecording){
                try {
                    //0.1s更新一次音量
                    Thread.sleep(100);
                    mTime += 0.1f;
                    mHandler.sendEmptyMessage(MSG_VOICE_CHANGED);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private static final int MSG_AUDIO_PREPARED = 0X110;
    private static final int MSG_VOICE_CHANGED = 0X111;
    private static final int MSG_DIALOG_DISMISS = 0X112;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_AUDIO_PREPARED:
                    //TODO 显示应该录音准备完成之后
                    mDialogManager.showRecordingDialog();
                    isRecording = true;
                    new Thread(mGetVoiceLevelRunnable).start();
                    break;
                case MSG_VOICE_CHANGED:
                    mDialogManager.updateVoiceLevel(mAudioManager.getVoiceLevel(7));
                    break;
                case MSG_DIALOG_DISMISS:
                    mDialogManager.dismissDialog();
                    break;
            }
        }
    };

    /**
     * 构造完成后的回调
     */
    @Override
    public void wellPrepare() {
        mHandler.sendEmptyMessage(MSG_AUDIO_PREPARED);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int)event.getX();
        int y = (int)event.getY();


        switch (action){
            case MotionEvent.ACTION_DOWN:
                isRecording = true;
                changeState(STATE_RECORDING);
                break;
            case MotionEvent.ACTION_MOVE:

                if(isRecording){
                    //根据x,y的坐标判断是否想要取消
                    if(wantToCancel(x,y)){
                        changeState(STATE_WANT_TO_CANCEL);
                    }else{
                        changeState(STATE_RECORDING);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                //启动失败检测
                if(!mReady){
                    reset();
                    return super.onTouchEvent(event);
                }

                if(!isRecording || mTime < 0.6f){
                    //录音准备失败或时间过短
                    mDialogManager.tooShort();
                    mAudioManager.cancel();
                    mHandler.sendEmptyMessageDelayed(MSG_DIALOG_DISMISS, 1336);
                }else if(mCurState == STATE_RECORDING){
                    //正常录制结束
                    mDialogManager.dismissDialog();
                    mAudioManager.release();
                    //存储文件
                    if(mListener != null){
                        mListener.onFinish(mTime, mAudioManager.getCurrentFilePath());
                    }


                    Intent intent = new Intent(mContext, Drift.class);
                    mContext.startActivity(intent);

                }else if(mCurState == STATE_WANT_TO_CANCEL){
                    //cancel
                    mDialogManager.dismissDialog();
                    mAudioManager.cancel();
                }
                reset();
                break;

        }

        return super.onTouchEvent(event);
    }

    private boolean wantToCancel(int x, int y) {
        //判断横坐标是否超出按钮的范围
        if(x < 0 || x > getWidth()){
            return true;
        }
        if(y < -DISTANCE_Y_CANCEL || y > getHeight() + DISTANCE_Y_CANCEL){
            return true;
        }

        return false;
    }

    /**
     * 恢复状态以及标志位
     */
    private void reset() {
        isRecording = false;
        mTime = 0;
        mReady = false;
        changeState(STATE_NORMAL);
    }

    private void changeState(int state) {
        if(mCurState != state){
            mCurState = state;
            switch (state){
                case STATE_NORMAL:
                    setImageResource(R.drawable.voice_button2);
//                    setBackgroundResource(R.drawable.btn_recording_normal);
//                    setText(R.string.str_recorder_mormal);
                    break;
                case STATE_RECORDING:
                    setImageResource(R.drawable.voice_button2_press);
//                    setBackgroundResource(R.drawable.btn_recording);
//                    setText(R.string.str_recorder_recording);
                    if(isRecording){
                        mDialogManager.recording();
                    }
                    break;
                case STATE_WANT_TO_CANCEL:
                    setImageResource(R.drawable.voice_button2);
//                    setBackgroundResource(R.drawable.btn_recording);
//                    setText(R.string.str_recorder_want_cancel);
                    mDialogManager.wantToCancel();
                    break;
            }
        }
    }
}
