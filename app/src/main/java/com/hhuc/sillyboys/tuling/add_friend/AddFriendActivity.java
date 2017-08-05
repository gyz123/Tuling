package com.hhuc.sillyboys.tuling.add_friend;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.hhuc.sillyboys.tuling.MainActivity;
import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.util.StatusBarCompat;

public class AddFriendActivity extends AppCompatActivity{
    private static final String TAG = "addFriendAcitivity";

    private TextView back, search;
    private RadioGroup method;
    private EditText keyword;

    private String searchMethod = "id";
    private String searchKeyword = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        StatusBarCompat.compat(this, getResources().getColor(R.color.status_bar_color));
        setContentView(R.layout.activity_add_friend);
        setDefaultFragment();
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

        keyword = (EditText)findViewById(R.id.add_friend_keyword);
        method = (RadioGroup)findViewById(R.id.add_friend_method);
        method.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int selectedId = radioGroup.getCheckedRadioButtonId();
                if(selectedId == R.id.add_friend_id)
                    searchMethod = "id";
                else if(selectedId == R.id.add_friend_nick)
                    searchMethod = "nick";
                else if(selectedId == R.id.add_friend_school)
                    searchMethod = "school";
            }
        });
        search = (TextView)findViewById(R.id.add_friend_search);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchKeyword = keyword.getText().toString().trim();
                Log.d(TAG, "关键字：" + searchKeyword);
                FragmentTransaction transaction =  getFragmentManager().beginTransaction();
                SearchResultFragment searchResultFragment = new SearchResultFragment();
                transaction.replace(R.id.add_friend_fragment, searchResultFragment);
                transaction.commit();
            }
        });
        back = (TextView)findViewById(R.id.add_friend_back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(AddFriendActivity.this, MainActivity.class));
            }
        });

    }

    // 初始化碎片
    private void setDefaultFragment() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        RecFriendsFragment fragment = new RecFriendsFragment();
        transaction.replace(R.id.add_friend_fragment, fragment);
        transaction.commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(AddFriendActivity.this, MainActivity.class));
    }
}
