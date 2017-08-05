package com.hhuc.sillyboys.tuling.self_info;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.hhuc.sillyboys.tuling.adapter.MyMessageAdapter;
import com.hhuc.sillyboys.tuling.broadcast.BroadcastActivity;
import com.hhuc.sillyboys.tuling.util.DividerItemDecoration;
import com.hhuc.sillyboys.tuling.util.StatusBarCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MyMessage extends AppCompatActivity {
    private final static String TAG = "mymessage";

    private RecyclerView mRecyclerView;
    private MyMessageAdapter mAdapter;
    private List<String> user,pictures,time,content;
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
        ((TextView)findViewById(R.id.self_info_func)).setText("我的消息");
        context = this;
        mAdapter = new MyMessageAdapter(context,user,pictures,time,content);
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
        mAdapter.setOnItemClickListener(new MyMessageAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                // 跳转对应的广播
                Log.d(TAG, "跳转频道");
                String type = "chat";
                String cname = user.get(position);
                if(cname.equals("g17")){
                    type = "group";
                }else if(cname.equals("豆豆")){
                    type = "chat";
                }
                Intent broadcastIntent = new Intent(context, BroadcastActivity.class);
                broadcastIntent.putExtra("compactId", "com.algebra.sdk.entity.CompactID@42cc5ce0")
                        .putExtra("cname", cname)
                        .putExtra("type", type);
                startActivity(broadcastIntent);
            }
            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
    }


    private void initDatas(){
        user = new ArrayList<String>(Arrays.asList("g17", "豆豆"));
        pictures = new ArrayList<String>(
                Arrays.asList(
                        "http://wx.qlogo.cn/mmopen/DZtibRDXICYabayGEnDE945eS02pbcBP53kI6LjyLODJqt59NpHVdXf1MHU1CwzRKNXcXt3cEdshTHTEIXsibNh4dVuIMyGfM5/0"
                            ,"" + resourceIdToUri(this, R.drawable.friend_doudou)
                ));
        time = new ArrayList<String>(Arrays.asList("2017-7-1"
                        ,"2017-7-1"));
        content = new ArrayList<String>(Arrays.asList("我们正在广播~快来收听吧"
                        ,"向你发起了语言聊天"));

    }

    private static final String ANDROID_RESOURCE = "android.resource://";
    private static final String FOREWARD_SLASH = "/";
    private static Uri resourceIdToUri(Context context, int resourceId) {
        return Uri.parse(ANDROID_RESOURCE + context.getPackageName() + FOREWARD_SLASH + resourceId);
    }

}
