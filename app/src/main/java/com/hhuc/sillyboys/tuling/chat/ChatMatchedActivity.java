package com.hhuc.sillyboys.tuling.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.util.StatusBarCompat;

public class ChatMatchedActivity extends AppCompatActivity {
    private static final String TAG = "secretActivity";

    private static SharedPreferences pref;
    private static SharedPreferences.Editor editor;
    private int selfId = 0;

    private ImageView configure_button, user_talk;
    private Context uiContext = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        StatusBarCompat.compat(this, getResources().getColor(R.color.status_bar_color));
        setContentView(R.layout.activity_chat_secret);
        init();
    }

    private void init() {
        // 动态设置高度
        String version_sdk = Build.VERSION.SDK; // 设备SDK版本
        String version_release = Build.VERSION.RELEASE; // 设备的系统版本
        Log.d(TAG, "version_sdk:" + version_sdk);
        Log.d(TAG, "version_release:" + version_release);
        if (Integer.parseInt("" + version_release.charAt(0)) >= 5) {
            Log.d(TAG, "高版本:" + version_release.charAt(0));
            CoordinatorLayout.LayoutParams statusBarParam = new CoordinatorLayout.LayoutParams
                    (CoordinatorLayout.LayoutParams.WRAP_CONTENT, CoordinatorLayout.LayoutParams.MATCH_PARENT);
            statusBarParam.topMargin = 0;
            LinearLayout layout = (LinearLayout) findViewById(R.id.status_bar_height);
            layout.setLayoutParams(statusBarParam);
        }
        // 获取个人信息
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();
        selfId = pref.getInt("selfid", 0);
        uiContext = this;
        // 背景色
        findViewById(R.id.chat_fragment).setBackgroundColor(getResources().getColor(R.color.transparent));
        // 标题
        ((TextView)findViewById(R.id.chat_cname)).setText("悄悄话聊天室");
        // 按钮
        // 设置
        configure_button = (ImageView)findViewById(R.id.chat_configure_button);
        configure_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "开启弹出菜单");
                PopupMenu popCfg = new PopupMenu(uiContext, view);
                MenuInflater menuInft = popCfg.getMenuInflater();
                menuInft.inflate(R.menu.chat_settings, popCfg.getMenu());
                popCfg.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        return onMainMenuItemClicked(item);
                    }
                });
                popCfg.show();
            }
        });
        // 话筒
        user_talk = (ImageView)findViewById(R.id.chat_user_talk);
        user_talk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "按住说话");
            }
        });

    }


    // 弹出菜单的子项点击事件
    public boolean onMainMenuItemClicked(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.chat_action_bluetooth:
                Log.d(TAG, "开启蓝牙");
                return true;
            case R.id.chat_action_set_audio:
                Log.d(TAG, "音频设置");
                return true;
            case R.id.chat_action_trig_ptt:
                Log.d(TAG, "PTT按钮");
                return true;
            default:
                return false;
        }
    }

}
