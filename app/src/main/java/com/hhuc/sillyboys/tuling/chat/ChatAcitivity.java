package com.hhuc.sillyboys.tuling.chat;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
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
import com.hhuc.sillyboys.tuling.broadcast.BroadcastActivity;
import com.hhuc.sillyboys.tuling.util.StatusBarCompat;

public class ChatAcitivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "chat_activity";
    private String cname = "";
    private Context uiContext = null;

    private TextView roomname;
    private ImageView configure_button, user_talk;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        StatusBarCompat.compat(this, getResources().getColor(R.color.status_bar_color));
        setContentView(R.layout.activity_chat);
        init();

    }

    private void init(){
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
        uiContext = this;
        // 标题文字
        roomname = (TextView)findViewById(R.id.chat_cname);
        cname = getIntent().getStringExtra("cname");
        roomname.setText(cname);
        // 设置按钮
        configure_button = (ImageView)findViewById(R.id.chat_configure_button);
        configure_button.setOnClickListener(this);
        // 话筒
        user_talk = (ImageView)findViewById(R.id.chat_user_talk);
        user_talk.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            // 话筒
            case R.id.chat_user_talk:
                Log.d(TAG, "按住说话");
                break;
            // 弹出菜单
            case R.id.chat_configure_button:
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
                break;
            default:
                break;
        }
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
