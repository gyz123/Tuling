package com.hhuc.sillyboys.tuling.search;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dinuscxj.itemdecoration.ShaderItemDecoration;
import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.adapter.RoundImgAdapter;
import com.hhuc.sillyboys.tuling.chat.ChatAcitivity;
import com.hhuc.sillyboys.tuling.util.DividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class SearchShopFragment  extends Fragment {
    private static final String TAG = "search_result";
    private RecyclerView mRecyclerView;
    private RoundImgAdapter mAdapter;
    private List<String> subject,description,pictures;
    private TextView hint;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private String adverChannel;
    private String userChannel;
    private int selfId = 0;
    private String keyword = "";
    private String shops = "";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.navi_fragment_search_result,container,false);
        mRecyclerView = (RecyclerView)view.findViewById(R.id.search_result_recycleview);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initDatas();
        init();
    }

    private void init(){
        mAdapter = new RoundImgAdapter(getActivity(),subject,description,pictures);
        mRecyclerView.setAdapter(mAdapter);
        // 设置布局
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mRecyclerView.getContext(),LinearLayoutManager.VERTICAL,false);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        // 设置分割线
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(),DividerItemDecoration.VERTICAL_LIST));
        ShaderItemDecoration shaderItemDecoration = new ShaderItemDecoration(getActivity(),
                ShaderItemDecoration.SHADER_BOTTOM | ShaderItemDecoration.SHADER_TOP);
        shaderItemDecoration.setShaderTopDistance(1);
        shaderItemDecoration.setShaderBottomDistance(1);
        mRecyclerView.addItemDecoration(shaderItemDecoration);
        // 点击事件
        mAdapter.setOnItemClickListener(new RoundImgAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String compactId = description.get(position);
                Intent enterBroadcastIntent = new Intent(getActivity(), ChatAcitivity.class);
                enterBroadcastIntent.putExtra("compactId", compactId)
                        .putExtra("cname", subject.get(position))
                        .putExtra("type", "shop");
                Log.d(TAG, "用户选择了商家: " + subject.get(position) + "；频道id：" + compactId);
                startActivity(enterBroadcastIntent);
            }

            @Override
            public void onItemLongClick(View view, int position) {
            }
        });

        // 提示文字
        hint = (TextView)getActivity().findViewById(R.id.search_hint);
        if(subject.size() == 0){
            hint.setText("未找到相关店家");
        }else{
            hint.setText("相关店家");
        }
        hint.setGravity(Gravity.CENTER);
    }

    private void initDatas(){
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        editor = pref.edit();
        selfId = pref.getInt("selfid", 0);    // 用户id
        keyword = pref.getString("keyword", "");    // 搜索关键字
        shops = pref.getString("shopChannels", "");

        subject = new ArrayList<String>();
        description = new ArrayList<String>();
        pictures = new ArrayList<String>();

        String[] shop = shops.split(";");
        Log.d(TAG, "商家：" + shop);

        for(int i=0; i<shop.length; i++){
            String userName = shop[i];
            if(userName.contains(keyword)){
                subject.add(userName);   // 商家名
                description.add("");        // 商家频道id
            }
        }
        for(int i = 0; i<subject.size(); i++){
            pictures.add("");
        }
    }

}