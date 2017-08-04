package com.hhuc.sillyboys.tuling.broadcast;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.ImageView;

import com.hhuc.sillyboys.tuling.R;

public class ShareDialog extends ProgressDialog {
    private ImageView code;

    /**
     * 设置弹窗属性
     * @param context 上下文
     */
    public ShareDialog(Context context) {
        super(context);
        setCanceledOnTouchOutside(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_show_code);
        init();
    }


    private void init(){
        code = (ImageView) findViewById(R.id.show_code_code);
        code.setImageResource(R.drawable.code);
    }



}
