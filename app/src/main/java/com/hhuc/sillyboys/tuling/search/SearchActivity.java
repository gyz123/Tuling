package com.hhuc.sillyboys.tuling.search;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.entity.MsgCode;
import com.hhuc.sillyboys.tuling.navi_fragment.MainFragment;
import com.hhuc.sillyboys.tuling.util.StatusBarCompat;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SearchActivity extends Activity {
    private static final String TAG = "searchAcitivity";
    public EditText keyword;
    public TextView search;
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MsgCode.SEARCHBROADCASTSUCCESS:
                    Log.d(TAG, "搜索广播成功");
                    break;
                case MsgCode.SEARCHBROADCASTFAILED:
                    Log.d(TAG, "搜索广播失败");
                    break;
                default:
                    break;
            }
        }
    };
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private String searchType = "search_broadcast";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);  // 不使用actionBar
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        StatusBarCompat.compat(this, getResources().getColor(R.color.status_bar_color));
        setContentView(R.layout.navi_fragment_search);
        setDefaultFragment();
        init();
    }


    private void init(){
        searchType = getIntent().getStringExtra("search_type");
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();
        keyword = (EditText)findViewById(R.id.keyword);
        search = (TextView)findViewById(R.id.search);
        // 搜索广播
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String word = keyword.getText().toString().trim();
                Log.d(TAG, "关键字：" + word);
                editor.putString("keyword", word);
                editor.commit();
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                if(searchType.equals("search_broadcast")){
                    Log.d(TAG, "搜索广播");
                    SearchBroadcastFragment searchBroadcastFragment = new SearchBroadcastFragment();
                    transaction.replace(R.id.search_fragment, searchBroadcastFragment);
                    transaction.commit();
                }else if(searchType.equals("search_people")){
                    Log.d(TAG, "搜索用户");
                    SearchPeopleFragment searchPeopleFragment = new SearchPeopleFragment();
                    transaction.replace(R.id.search_fragment, searchPeopleFragment);
                    transaction.commit();
                }else if(searchType.equals("search_shop")){
                    Log.d(TAG, "搜索店铺");
                    SearchShopFragment searchShopFragment = new SearchShopFragment();
                    transaction.replace(R.id.search_fragment, searchShopFragment);
                    transaction.commit();
                }else{
                    Log.d(TAG, "未知的搜索操作");
                }

            }
        });
    }


    private void queryPeople(String keyword){
        OkHttpClient client = new OkHttpClient();
        FormBody formBody = new FormBody.Builder()
                .add("people", keyword)
                .build();
        Request request = new Request.Builder()
                .url("http://www.iotesta.cn/library/searchlist.action")
                .post(formBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.obtainMessage(MsgCode.SEARCHBROADCASTFAILED).sendToTarget();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String result = response.body().string();
                Log.d(TAG, "搜索到的广播：" + result);
                handler.obtainMessage(MsgCode.SEARCHBROADCASTSUCCESS, 0, 0, result).sendToTarget();
            }
        });
    }

    // 初始化碎片
    private void setDefaultFragment() {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        MainFragment mainFragment = new MainFragment();
        transaction.replace(R.id.search_fragment, mainFragment);
        transaction.commit();
    }

}
