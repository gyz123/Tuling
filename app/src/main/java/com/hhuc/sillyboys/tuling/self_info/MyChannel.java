package com.hhuc.sillyboys.tuling.self_info;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.dinuscxj.itemdecoration.ShaderItemDecoration;
import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.adapter.MyChannelAdapter;
import com.hhuc.sillyboys.tuling.broadcast.BroadcastActivity;
import com.hhuc.sillyboys.tuling.util.DividerItemDecoration;
import com.hhuc.sillyboys.tuling.util.StatusBarCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  我创建的频道
 */
public class MyChannel extends AppCompatActivity{
    private static final String TAG = "mychannel";

    private RecyclerView mRecyclerView;
    private MyChannelAdapter mAdapter;
    private List<String> subject,pictures;
    private Context context;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        StatusBarCompat.compat(this, getResources().getColor(R.color.status_bar_color));
        setContentView(R.layout.self_info);
        initDatas();
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
        ((TextView)findViewById(R.id.self_info_func)).setText("我的频道");
        context = this;
        mAdapter = new MyChannelAdapter(context,subject,pictures);
        mRecyclerView = (RecyclerView)findViewById(R.id.mychannel_recyclerView);
        mRecyclerView.setAdapter(mAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mRecyclerView.getContext(),LinearLayoutManager.VERTICAL,false);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        // 设置Item之间的分割线
        mRecyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
        ShaderItemDecoration shaderItemDecoration = new ShaderItemDecoration(context,
                ShaderItemDecoration.SHADER_BOTTOM | ShaderItemDecoration.SHADER_TOP);
        shaderItemDecoration.setShaderTopDistance(1);
        shaderItemDecoration.setShaderBottomDistance(1);
        mRecyclerView.addItemDecoration(shaderItemDecoration);


        // 设置增加删除的动画效果
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // 接口回调，设置点击事件
        mAdapter.setOnItemClickListener(new MyChannelAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                // 跳转对应的广播
                Log.d(TAG, "跳转频道");
                Intent broadcastIntent = new Intent(context, BroadcastActivity.class);
                broadcastIntent.putExtra("compactId", "com.algebra.sdk.entity.CompactID@42cc5ce0")
                            .putExtra("cname", subject.get(position))
                            .putExtra("type", "group")
                            .putExtra("identity", "admin");
                startActivity(broadcastIntent);
            }
            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
    }

    private void initDatas(){
        subject = new ArrayList<String>(Arrays.asList("g17"));
        pictures = new ArrayList<String>(
                Arrays.asList("http://wx.qlogo.cn/mmopen/DZtibRDXICYabayGEnDE945eS02pbcBP53kI6LjyLODJqt59NpHVdXf1MHU1CwzRKNXcXt3cEdshTHTEIXsibNh4dVuIMyGfM5/0"
                ));
    }

}
