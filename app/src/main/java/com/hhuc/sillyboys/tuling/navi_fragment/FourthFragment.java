package com.hhuc.sillyboys.tuling.navi_fragment;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.hhuc.sillyboys.tuling.MainActivity;
import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.entity.MsgCode;
import com.hhuc.sillyboys.tuling.self_info.ModifyInfo;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** 个人信息板块
 *
 */
public class FourthFragment extends Fragment implements View.OnClickListener{
    private static final String TAG = "FourthFragment";
    private ImageView headimg;
    private TextView myChannel,favorChannel,message;
    private Button modify,logout;

    private static SharedPreferences pref;
    private int selfId = 0;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                // FourthFragment
                case MsgCode.GETUSERINFOSUCCESS:
                    Log.d(TAG, "获取用户资料成功");
                    break;
                case MsgCode.GETUSERINFOFAILED:
                    Log.d(TAG, "获取用户资料失败");
                    Glide.with(getActivity())
                            .load(R.drawable.tuling)
                            .asBitmap()
                            .error(R.mipmap.ic_launcher)
                            .centerCrop()
                            .into(headimg);
                    break;
                default:
                    break;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.navi_fragment_fourth,container,false);
        Log.d(TAG, "onCreatView");
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        selfId = pref.getInt("selfid", 0);
        init();
        TextView toolbarText = (TextView)getActivity().findViewById(R.id.toolbar_text);
        toolbarText.setText("个人信息");

        Log.d(TAG, "用户id：" + selfId);
        queryUserInfo(selfId);
    }

    private void init(){
        // 个人信息
        headimg = (ImageView)getActivity().findViewById(R.id.user_image);
        Glide.with(getActivity())
                .load("http://wx.qlogo.cn/mmopen/DZtibRDXICYabayGEnDE945eS02pbcBP53kI6LjyLODJqt59NpHVdXf1MHU1CwzRKNXcXt3cEdshTHTEIXsibNh4dVuIMyGfM5/0")
                .asBitmap()
                .placeholder(R.mipmap.ic_launcher)
                .centerCrop()
                .into(headimg);
        String selfnick = pref.getString("selfnick", "");
        ((TextView)getActivity().findViewById(R.id.user_nick)).setText(selfnick);
        String selfage = pref.getString("selfage", "");
        ((TextView)getActivity().findViewById(R.id.user_age)).setText(selfage);
        String selfsex = pref.getString("selfsex", "");
        ((TextView)getActivity().findViewById(R.id.user_sex)).setText(selfsex);
        String selfschool = pref.getString("selfschool", "");
        ((TextView)getActivity().findViewById(R.id.user_school)).setText(selfschool);
        if((selfnick + selfage + selfsex + selfschool).isEmpty()){
            Glide.with(getActivity())
                    .load(R.drawable.img_default)
                    .asBitmap()
                    .placeholder(R.mipmap.ic_launcher)
                    .centerCrop()
                    .into(headimg);
            getActivity().findViewById(R.id.user_info).setVisibility(View.INVISIBLE);
            getActivity().findViewById(R.id.user_hint).setVisibility(View.VISIBLE);
        }else{
            getActivity().findViewById(R.id.user_info).setVisibility(View.INVISIBLE);
            getActivity().findViewById(R.id.user_hint).setVisibility(View.VISIBLE);
        }
        // 功能列表
        (getActivity().findViewById(R.id.user_my_channel)).setOnClickListener(this);
        (getActivity().findViewById(R.id.user_my_favorate)).setOnClickListener(this);
        (getActivity().findViewById(R.id.user_my_message)).setOnClickListener(this);
        // 账号操作
        (getActivity().findViewById(R.id.user_modify)).setOnClickListener(this);
        (getActivity().findViewById(R.id.user_logout)).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.user_my_channel:
                Log.d(TAG, "进入我的频道");
                break;
            case R.id.user_my_favorate:
                Log.d(TAG, "进入我的关注");
                break;
            case R.id.user_my_message:
                Log.d(TAG, "进入我的消息");
                break;
            case R.id.user_modify:
                Log.d(TAG, "修改资料");
                startActivity(new Intent(getActivity(), ModifyInfo.class));
                break;
            case R.id.user_logout:
                Log.d(TAG, "退出登录");
                break;
            default:
                break;
        }
    }

    private void queryUserInfo(int selfId){
        OkHttpClient client = new OkHttpClient();
        FormBody formBody = new FormBody.Builder()
                .add("userid", selfId + "")
                .build();
        Request request = new Request.Builder()
                .url("http://www.iotesta.com.cn/library/searchlist.action")     // 网址需要修改
                .post(formBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
               handler.obtainMessage(MsgCode.GETUSERINFOFAILED).sendToTarget();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String result = response.body().string();
                Log.d(TAG, "用户信息：" + result);
                handler.obtainMessage(MsgCode.GETUSERINFOSUCCESS, 0, 0, result).sendToTarget();
            }
        });
    }

}
