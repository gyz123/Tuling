package com.hhuc.sillyboys.tuling.chat;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.algebra.sdk.API;
import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.util.StatusBarCompat;

public class SecretActivity  extends AppCompatActivity {
    private static final String TAG = "secretActivity";
    private RadioGroup sexGroup, topic;
    private EditText age;
    private TextView confirm;
    private NumberPicker agePicker;

    private String targetAge = "";
    private String targetSex = "male";
    private String targetTopic = "情感";
    private static SharedPreferences pref;
    private static SharedPreferences.Editor editor;
    private int selfId = 0;
    private String selfName = "";
    private String selfAge = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        StatusBarCompat.compat(this, getResources().getColor(R.color.status_bar_color));
        setContentView(R.layout.activity_chat_secret);
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
        // 获取个人信息
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();
        selfId = pref.getInt("selfid", 0);
        selfAge = "18";     // 年龄暂时设置为18，之后将从数据库读取 ****************************************
        try{
            selfName = API.uid2nick(selfId);
        }catch (Exception e){
            Log.d(TAG,"获取用户昵称失败");
            selfName = "testName";
        }
        // 性别
        sexGroup = (RadioGroup)findViewById(R.id.secret_sex_radiogroup);
        sexGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                if(selectedId == R.id.secret_sex_male)
                    targetSex = "male";
                else
                    targetSex = "female";
            }
        });
        // 年龄
        age = (EditText)findViewById(R.id.secret_age);
//        agePicker = (NumberPicker)findViewById(R.id.secret_age);
        // 话题
        topic = (RadioGroup)findViewById(R.id.secret_topic);
        topic.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                targetTopic = ((RadioButton)SecretActivity.this.findViewById(selectedId)).getText().toString();
            }
        });

        confirm = (TextView)findViewById(R.id.secret_confirm);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                targetAge = age.getText().toString().trim();
//                targetAge = agePicker.getValue() + "";
                Log.d(TAG, "目标年龄：" + targetAge + "，目标性别：" + targetSex + "，目标话题：" + targetTopic);

            }
        });
    }


}
