package com.hhuc.sillyboys.tuling.chat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.algebra.sdk.API;
import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.broadcast.BroadcastActivity;
import com.hhuc.sillyboys.tuling.util.StatusBarCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
    private String selfSex = "female";
    private Context mContext;
    SearchDialog searchDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        StatusBarCompat.compat(this, getResources().getColor(R.color.status_bar_color));
        setContentView(R.layout.activity_chat_secret);
        mContext = this;
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
        selfId = getIntent().getIntExtra("selfid", 0);
        //TODO 年龄暂时设置为18，之后将从数据库读取
        selfAge = "19";
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
                start(confirm);
            }
        });
    }

    public void start(View view){
        show_anim(view);

        uihandler.postDelayed(runnable, 2000);
    }

    /**
     * msg.what
     * 1 测试用例
     * 2 为匹配序列号线程发送
     * 3 匹配成功停止线程
     */
    Handler uihandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    //测试用线程
                    Log.d("uihandler", "handleMessage: 匹配成功");
                    uihandler.removeCallbacks(runnable);
                    searchDialog.cancel();
//TODO                    Intent intent = new Intent(mContext, ChatMatchedActivity.class);
//                    mContext.startActivity(intent);
                    break;
                case 2:
                    uihandler.postDelayed(getObjectRunnable, 2000);
                    //postGetObject();
                    break;
                case 3:
                    uihandler.postDelayed(delayRunnable, 8000);
                    break;
            }
        }
    };

    //启动用线程
    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            //要做的事情
            //httpGet();
            postGetID(selfName, selfAge, selfSex, targetAge, targetSex, postGetIDUrl);
            uihandler.postDelayed(this, 2000);
        }
    };

    /**
     * 重复调用postGetObject方法
     */
    Runnable getObjectRunnable = new Runnable() {
        @Override
        public void run() {
            postGetObject(myMatchInt, postGetObjectUrl);
            //uihandler.postDelayed(this, 2000);
        }
    };

    /**
     * 延时调用，防止过快跳转
     */
    Runnable delayRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d("delayRunnable", "我只是想让程序慢一点 ");
            uihandler.removeCallbacks(getObjectRunnable);
            searchDialog.cancel();
//            Intent intent2 = new Intent(mContext, ChatMatchedActivity.class);
//            mContext.startActivity(intent2);
            // TODO 在这里设置了匹配成功后的跳转
            Intent broadcastIntent = new Intent(mContext, BroadcastActivity.class);
            broadcastIntent.putExtra("compactId", "com.algebra.sdk.entity.CompactID@42cc5ce0")
                    .putExtra("cname", "悄悄话")
                    .putExtra("selfid", selfId)
                    .putExtra("type", "secret");
            startActivity(broadcastIntent);
        }
    };

    /**
     * 帧动画通过dialog启动
     * @param view
     */
    public void show_anim(View view){

        searchDialog = new SearchDialog(SecretActivity.this, uihandler, getObjectRunnable, runnable);
        //设置背景透明
        searchDialog.getWindow().setBackgroundDrawable(new ColorDrawable());
        searchDialog.show();
    }

    //原测试服务器
    private String url = "http://192.168.43.25:8888/";

    public void httpGet(){

        Log.d("run", "httpGet: ");
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .get()
                .url(url + "okhttpmzy/servlet/OkHttpServlet")
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                Log.d("http", "onFailure: 回调失败");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String result = response.body().string();
                Log.d("response", "onResponse() returned: " +  result);
                ResponseThread responseThread = new ResponseThread(result);
                responseThread.start();
            }
        });
    }

    class ResponseThread extends Thread{
        private String content;
        private int contentInt;
        public ResponseThread(String content){
            this.content = content;
        }

        @Override
        public void run() {
            super.run();
            contentInt = Integer.parseInt(content);
            if(contentInt % 10 == 0){
                uihandler.sendEmptyMessage(1);
            }
            Log.d("ResponseThread", "执行子线程 ");
        }
    }
//    局域网IP
//    private String IPv4 = "http://192.168.43.25:8888/";

//    远程服务器
    private String IPv4 = "http://115.159.189.169:8080/";

    private String postGetIDUrl = IPv4 + "web02/servlet/CreateRecordServlet";
    int count = 0;//计数器当服务器忙碌时停止调用
    String myMatchInt;//存储匹配序列号
    /**
     * 用户获取匹配ID的方法，向服务器请求，三次失败则停止请求
     * @param userName 用户的姓名
     * @param userAge 用户年龄
     * @param userSex 用户性别
     * @param targetAge 目标年龄
     * @param targetSex 目标性别
     * @param postGetIDUrl 服务器地址
     * response:失败false
     *          成功得到匹配序列号matchId
     */
    public void postGetID(final String userName, final String userAge, final String userSex, final String targetAge,
                          final String targetSex, final String postGetIDUrl){
        OkHttpClient client = new OkHttpClient();
        FormBody formBody = new FormBody.Builder()
                .add("UserName", userName)
                .add("UserEge", userAge)
                .add("UserSex", userSex)
                .add("TargetEge", targetAge)
                .add("TargetSex", targetSex)
                .build();
        Request request = new Request.Builder()
                .url(postGetIDUrl)
                .post(formBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("postGetID", "onFailure:回调失败 ");
                count++;
                if(count >= 3){
                    uihandler.removeCallbacks(runnable);
                    searchDialog.cancel();
                    count = 0;
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String result = response.body().string();
                Log.d("postGetID", "onResponse() returned: " +  result);
                if(!result.equals("false") && result != null){
                    //获取到匹配ID
                    String matchId = result;
                    Log.d("postGetID", "matchId is" + matchId);
                    myMatchInt =  result;
                    //移除回调线程
                    uihandler.removeCallbacks(runnable);

                    //利用子线程向主线程发送数据
                    threadGetID threadGetID = new threadGetID();
                    threadGetID.start();


                }else{
                    Log.d("postGetID", "count: " + count);
                    //超过三次请求失败停止请求
                    if(count <= 3){
                        count++;
                        postGetID(userName,userAge,userSex,targetAge,targetSex,postGetIDUrl);
                    }else{
                        count = 0;
//                        Toast.makeText(SecretActivity.this, "服务器出了点小问题，请稍后再试", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "服务器出了点小问题，请稍后再试");
                        searchDialog.cancel();
                        uihandler.removeCallbacks(runnable);
                    }
                }
            }
        });
    }

    /**
     * GetID方法线程，发送标志位msg.what=2 arg1 = matchId
     */
    class threadGetID extends Thread{

        public threadGetID(){

        }

        @Override
        public void run() {
            super.run();
//            Message message = uihandler.obtainMessage();
//            message.arg1 = matchIdInt;
//            message.what = 2;
//            uihandler.sendMessage(message);
            uihandler.sendEmptyMessage(2);
        }
    }


    private String postGetObjectUrl = IPv4 + "web02/servlet/MatchingServlet";

    /**
     * 用户获取匹配序列号后，定时向服务器进行请求匹配
     * @param matchId 匹配序列号
     * @param postGetObjectUrl 服务器地址
     * response：失败wait
     *           成功（JSON）Channel 房间号
     *                       IsCreateChannel 用户是否需要创建房间（0加入房间，1创建房间）
     */
    public void postGetObject(String matchId, String postGetObjectUrl){
        OkHttpClient client = new OkHttpClient();
        FormBody formBody = new FormBody.Builder()
                .add("UsrID", matchId)
                .build();
        Request request = new Request.Builder()
                .url(postGetObjectUrl)
                .post(formBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("postGetObject", "onFailure:回调失败 ");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String result = response.body().string();
                Log.d("postGetObject", "onResponse() returned: " +  result);
                if(result.equals("false") || result.equals("wait") ||result == null){
                    //继续轮询
                    uihandler.sendEmptyMessage(2);
                    return;
                }
                //报错时停止
                if(result.length() > 100){
                    searchDialog.cancel();
                    Log.d(TAG, "出错了，请稍后再试");
                }
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    String channelId = jsonObject.getString("Channel");
                    String isCreateChannel = jsonObject.getString("IsCreateChannel");
                    Log.d("postGetObject", "channelId: " + channelId + ",isCreateChannel:" + isCreateChannel);

                    if(isCreateChannel.equals("0")){
                        joinChannel();
                    }else if(isCreateChannel.equals("1")){
                        createChannel();
                    }

                    uihandler.sendEmptyMessage(3);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });
    }

    public void joinChannel(){

    }

    public void createChannel(){

    }
}
