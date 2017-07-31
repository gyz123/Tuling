package com.hhuc.sillyboys.tuling.broadcast;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.algebra.sdk.AccountApi;
import com.algebra.sdk.SessionApi;
import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.navi_fragment.FirstFragment;
import com.hhuc.sillyboys.tuling.util.StatusBarCompat;

import java.lang.ref.WeakReference;

public class BroadcastActivity2 extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "broadcast2";
    private static Handler uiHandler = null;
    public static Handler getUiHandler() {
        return uiHandler;
    }
    private Context uiContext = null;
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private boolean isCreator = false;
    private int selfId = 0;
    private int ctype = 0;
    private String cid = "";
    private String cname = "";
    private AccountApi accountApi = null;
    private boolean userBoundPhone = false;
    private String userAccount = null;
    private String userPass = null;
    private String userNick = "???";
    private String userPhone = null;
    private SessionApi sessionApi = null;

    private TextView channelName;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate..");

        // 状态栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        StatusBarCompat.compat(this, getResources().getColor(R.color.status_bar_color));
        setContentView(R.layout.activity_broadcast2);
        // 动态设置高度
        String version_sdk = Build.VERSION.SDK; // 设备SDK版本
        String version_release = Build.VERSION.RELEASE; // 设备的系统版本
        Log.d(TAG,"version_sdk:" + version_sdk);
        Log.d(TAG,"version_release:" + version_release);
        if(Integer.parseInt("" + version_release.charAt(0)) >= 5){
            Log.d(TAG,"高版本:" + version_release.charAt(0));
            CoordinatorLayout.LayoutParams statusBarParam = new CoordinatorLayout.LayoutParams
                    (CoordinatorLayout.LayoutParams.WRAP_CONTENT,CoordinatorLayout.LayoutParams.MATCH_PARENT);
            statusBarParam.topMargin = 0;
            LinearLayout layout = (LinearLayout)findViewById(R.id.status_bar_height);
            layout.setLayoutParams(statusBarParam);
        }
        // 标题
        channelName = (TextView)findViewById(R.id.broadcast_cname);
        channelName.setText(getIntent().getStringExtra("cname"));
        // 初始化碎片
        setFragment();

    }


    private void setFragment() {
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        selfId = pref.getInt("selfid", 0);
        cid = getIntent().getStringExtra("compactId");
        cname = getIntent().getStringExtra("cname");
        isCreator = judgeCreator(selfId, cid);      // 依据selfid和cid判断当前为主播还是听众
        if(!isCreator){
            FragmentManager fragmentManager = getFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            UserFragment userFragment = new UserFragment();
            transaction.replace(R.id.broadcast_fragment, userFragment);
            transaction.commit();
        }
    }


    private boolean judgeCreator(int selfId, String cid){
        return false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){

            default:
                break;
        }
    }


    /**
     *  异步处理
     */
    private class UiHandler extends Handler {
        WeakReference<BroadcastActivity2> wrActi;
        BroadcastActivity2 mActi = null;
        public UiHandler(BroadcastActivity2 act) {
            wrActi = new WeakReference<BroadcastActivity2>(act);
        }
        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            mActi = wrActi.get();
            if (mActi == null)
                return;
            switch (msg.what) {


                default:
                    Log.e(TAG, "uiHandler unexpected msg: " + msg.what);
                    break;
            }
        }
    };
}
