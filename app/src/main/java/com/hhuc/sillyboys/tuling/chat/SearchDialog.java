package com.hhuc.sillyboys.tuling.chat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import com.hhuc.sillyboys.tuling.R;

/**
 * Created by mzy on 2017/8/1.
 */

public class SearchDialog extends ProgressDialog {
    private ImageView anim;
    private ImageView close;

    /**
     * 设置弹窗属性
     * @param context 上下文
     * @param handler
     * @param runnable 控制传入线程的中断..这里写的不大友好，有时间就改进吧
     */
    public SearchDialog(Context context, final Handler handler, final Runnable runnable, final Runnable runnnable2) {
        super(context);
        setCanceledOnTouchOutside(true);
        //取消则停止线程调用
        setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Log.d("dialog", "onCancel: 调用了移除方法");
                handler.removeCallbacks(runnable);
                handler.removeCallbacks(runnnable2);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_dialog);
        init();


    }

    private void init(){

        anim = (ImageView) findViewById(R.id.search_anim);
        //  close = (ImageView) findViewById(R.id.close_anim);
        // anim.setImageResource(R.drawable.loadanim);
        anim.setImageResource(R.drawable.loadanim);
        final AnimationDrawable animationDrawable = (AnimationDrawable) anim.getDrawable();

        //线程调用动画
        anim.post(new Runnable() {
            @Override
            public void run() {
                animationDrawable.start();
            }
        });

    }

    public void stopHandler(Handler handler, Runnable runnable){

    }
}
