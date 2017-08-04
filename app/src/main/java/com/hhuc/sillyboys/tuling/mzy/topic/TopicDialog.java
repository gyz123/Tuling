package com.hhuc.sillyboys.tuling.mzy.topic;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.hhuc.sillyboys.tuling.R;

/**
 * Created by mzy on 2017/8/4.
 */

public class TopicDialog extends Dialog{

    public TopicDialog(@NonNull Context context) {
        super(context);
        setCanceledOnTouchOutside(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.topic_dialog);
    }

}
