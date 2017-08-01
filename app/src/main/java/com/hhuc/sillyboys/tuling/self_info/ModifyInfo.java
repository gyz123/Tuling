package com.hhuc.sillyboys.tuling.self_info;

import android.content.Intent;
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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.hhuc.sillyboys.tuling.MainActivity;
import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.util.StatusBarCompat;

public class ModifyInfo extends AppCompatActivity{
    private static final String TAG = "modify_info";

    private EditText nick, age, school, phone;
    private RadioGroup sex;
    private TextView confirm, back;
    private String myNick, myAge, mySchool, myPhone;
    private String mySex = "男";

    private static SharedPreferences pref;
    private static SharedPreferences.Editor editor;
    private int selfId = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        StatusBarCompat.compat(this, getResources().getColor(R.color.status_bar_color));
        setContentView(R.layout.activity_modify_info);
        init();
        initComponent();
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
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();
        selfId = getIntent().getIntExtra("selfid", 0);
    }

    private void initComponent(){
        nick = (EditText)findViewById(R.id.modify_info_nick);
        age = (EditText)findViewById(R.id.modify_info_age);
        school = (EditText)findViewById(R.id.modify_info_school);
        phone = (EditText)findViewById(R.id.modify_info_phone);
        sex = (RadioGroup)findViewById(R.id.modify_info_sex);
        sex.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                mySex = ((RadioButton)findViewById(selectedId)).getText().toString();
            }
        });
        confirm = (TextView)findViewById(R.id.modify_info_confirm);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myNick = nick.getText().toString().trim();
                myAge = age.getText().toString().trim();
                mySchool = school.getText().toString().trim();
                myPhone = phone.getText().toString().trim();
                Log.d(TAG, "我的信息：" + myNick + "," + myAge + "," + mySex + "," + mySchool + "," + myPhone);
                editor.putString("selfnick", myNick).putString("selfage", myAge).putString("selfsex", mySex)
                        .putString("selfschool", mySchool).putString("selfphone", myPhone);
                editor.commit();
                Log.d(TAG, "保存信息成功");
                Intent intent = new Intent(ModifyInfo.this, MainActivity.class);
                intent.putExtra("lastAc", "fourth").putExtra("selfid", selfId);
                startActivity(intent);
            }
        });
        back = (TextView)findViewById(R.id.modify_info_back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ModifyInfo.this, MainActivity.class);
                intent.putExtra("lastAc", "fourth").putExtra("selfid", selfId);
                startActivity(intent);
            }
        });
    }

}
