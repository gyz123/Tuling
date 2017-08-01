package com.hhuc.sillyboys.tuling.mzy.dritfbottle;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.hhuc.sillyboys.tuling.R;

/**
 * Created by mzy on 2017/8/1.
 */

public class DialogManager_Speaker {

    private Dialog mDialog;
    private ImageView mVoice;
    private TextView mText;

    private Context mContext;

    /**
     * 构造方法
     * @param context
     */
    public DialogManager_Speaker(Context context){
        mContext = context;
    }

    /**
     * 播放时显示
     */
    public void playingSound(){
        mDialog = new Dialog(mContext, R.style.Theme_AudioDialog);

        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.dialog_speaker, null);
        mDialog.setContentView(view);

        mText = (TextView) mDialog.findViewById(R.id.id_speaker_dialog_label);
        mVoice = (ImageView) mDialog.findViewById(R.id.id_speaker_dialog_icon);
        mVoice.setImageResource(R.drawable.play_anim);
        AnimationDrawable animationDrawable = (AnimationDrawable) mVoice.getDrawable();
        animationDrawable.start();

        mDialog.show();

    }

    /**
     * 隐藏Dialog
     */
    public void dismissDialog(){
        if(mDialog != null && mDialog.isShowing()){
            mDialog.dismiss();
            mDialog = null;
        }
    }

}
