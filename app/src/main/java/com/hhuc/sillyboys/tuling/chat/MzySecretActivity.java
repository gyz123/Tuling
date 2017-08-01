package com.hhuc.sillyboys.tuling.chat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.hhuc.sillyboys.tuling.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by mzy on 2017/8/1.
 */

public class MzySecretActivity extends Activity{
    private Button bt1;
    private Context mContext;

    private EditText ed1;
    private EditText ed2;
    private EditText ed3;
    private EditText ed4;
    private EditText ed5;

    private String name;
    private String userAge;
    private String userSex;
    private String targetAge;
    private String targetSex;

    SearchDialog searchDialog ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mzysecretactivity);
        bt1 = (Button) findViewById(R.id.button);
        mContext = MzySecretActivity.this;

        ed1 = (EditText) findViewById(R.id.editText);
        ed2 = (EditText) findViewById(R.id.editText2);
        ed3 = (EditText) findViewById(R.id.editText3);
        ed4 = (EditText) findViewById(R.id.editText4);
        ed5 = (EditText) findViewById(R.id.editText5);


    }

    /**
     * 按钮触发效果
     * @param view
     */
    public void start(View view){

        show_anim(view);
        name = ed1.getText().toString();
        userAge = ed2.getText().toString();
        userSex = ed3.getText().toString();
        targetAge = ed4.getText().toString();
        targetSex = ed5.getText().toString();
        uihandler.postDelayed(runnable, 2000);
//        postGetID("", "", "", "", "", postGetIDUrl);
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
                    Intent intent = new Intent(mContext, SecretActivity.class);
                    mContext.startActivity(intent);
                    break;
                case 2:
                    uihandler.postDelayed(getObjectRunnable, 2000);
                    //postGetObject();
                    break;
                case 3:
                    uihandler.removeCallbacks(getObjectRunnable);
                    searchDialog.cancel();
                    Intent intent2 = new Intent(mContext, SecretActivity.class);
                    mContext.startActivity(intent2);
                    break;
            }
        }
    };

    //测试用线程
    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            //要做的事情
            //httpGet();
            postGetID(name, userAge, userSex, targetAge, targetSex, postGetIDUrl);
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
     * 帧动画通过dialog启动
     * @param view
     */
    public void show_anim(View view){

        searchDialog = new SearchDialog(MzySecretActivity.this, uihandler, getObjectRunnable, runnable);
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

    private String IPv4 = "http://192.168.43.25:8888/";

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
                        Toast.makeText(MzySecretActivity.this, "服务器出了点小问题，请稍后再试", Toast.LENGTH_SHORT).show();
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
