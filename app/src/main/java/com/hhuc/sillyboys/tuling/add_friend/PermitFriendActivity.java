package com.hhuc.sillyboys.tuling.add_friend;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.hhuc.sillyboys.tuling.MainActivity;
import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.self_info.MyMessage;
import com.hhuc.sillyboys.tuling.util.StatusBarCompat;

public class PermitFriendActivity extends AppCompatActivity{
    private static final String TAG = "permitFriendAcitivity";

    private TextView back;
    private ImageView imageView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        StatusBarCompat.compat(this, getResources().getColor(R.color.status_bar_color));
        setContentView(R.layout.activity_permit_friend);
        init();
        initComponent();
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
    }


    private void initComponent(){
        (findViewById(R.id.permit_friend_agree)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "同意好友申请");
                Toast.makeText(PermitFriendActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(PermitFriendActivity.this, MainActivity.class));
            }
        });
        (findViewById(R.id.permit_friend_refuse)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "拒绝好友申请");
                Toast.makeText(PermitFriendActivity.this, "已拒绝", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(PermitFriendActivity.this, MainActivity.class));
            }
        });
        (findViewById(R.id.permit_friend_back)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(PermitFriendActivity.this, MyMessage.class));
            }
        });
        imageView = (ImageView)findViewById(R.id.permit_friend_image);
        Glide.with(this)
                .load(R.drawable.friend_dashi)
                .asBitmap()
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .centerCrop()
                .into(new BitmapImageViewTarget(imageView){
                    @Override
                    protected void setResource(Bitmap resource) {
                        RoundedBitmapDrawable circleImage =
                                RoundedBitmapDrawableFactory.create(getResources(),resource);
                        circleImage.setCircular(true);
                        imageView.setImageDrawable(circleImage);
                    }
                });

    }


}
