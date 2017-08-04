package com.hhuc.sillyboys.tuling.navi_fragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.algebra.sdk.API;
import com.algebra.sdk.DeviceApi;
import com.algebra.sdk.SessionApi;
import com.algebra.sdk.entity.CompactID;
import com.algebra.sdk.entity.IntStr;
import com.bartoszlipinski.recyclerviewheader.RecyclerViewHeader;
import com.dinuscxj.itemdecoration.ShaderItemDecoration;
import com.hhuc.sillyboys.tuling.LoginActivity;
import com.hhuc.sillyboys.tuling.broadcast.BroadcastActivity;
import com.hhuc.sillyboys.tuling.MainActivity;
import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.broadcast.BroadcastActivity2;
import com.hhuc.sillyboys.tuling.search.SearchActivity;
import com.hhuc.sillyboys.tuling.adapter.RoundImgAdapter;
import com.hhuc.sillyboys.tuling.tl_demo.TalkFragment;
import com.hhuc.sillyboys.tuling.util.DividerItemDecoration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FirstFragment extends Fragment {
    private static final String TAG = "firstFragment";
    private RecyclerView mRecyclerView;
    private RoundImgAdapter mAdapter;
    private List<String> subject,description,pictures;

    private static SharedPreferences pref;
    private static SharedPreferences.Editor editor;
    private String adverChannel;
    private String userChannel;
    private int selfId = 0;

    private SessionApi sessionApi = null;
    private Context uiContext = null;
    private Handler uiHandler = null;
    private DeviceApi deviceApi = null;
    private CompactID currSession = null;
    private List<IntStr> sessPresences = null;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.navi_fragment_first, container, false);
        mRecyclerView = (RecyclerView)view.findViewById(R.id.first_recyclerView);
        Log.d(TAG, "onCreatView");
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        TextView toolbarText = (TextView)getActivity().findViewById(R.id.toolbar_text);
        toolbarText.setText("校园广播");
        initDatas();

        // 设置布局
        mAdapter = new RoundImgAdapter(getActivity(),subject,description,pictures);
        mRecyclerView.setAdapter(mAdapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mRecyclerView.getContext(),LinearLayoutManager.VERTICAL,false);
        mRecyclerView.setLayoutManager(linearLayoutManager);

        // 设置Item之间的分割线
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(),DividerItemDecoration.VERTICAL_LIST));
        ShaderItemDecoration shaderItemDecoration = new ShaderItemDecoration(getActivity(),
                ShaderItemDecoration.SHADER_BOTTOM | ShaderItemDecoration.SHADER_TOP);
        shaderItemDecoration.setShaderTopDistance(1);
        shaderItemDecoration.setShaderBottomDistance(1);
        mRecyclerView.addItemDecoration(shaderItemDecoration);

        // 设置增加删除的动画效果
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // 接口回调，设置点击事件
        mAdapter.setOnItemClickListener(new RoundImgAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                // 跳转对应的广播
                String compactId = description.get(position);
                Intent broadcastIntent = new Intent(getActivity(), BroadcastActivity.class);
                broadcastIntent.putExtra("compactId", compactId)
                        .putExtra("cname", subject.get(position))
                        .putExtra("type", "broadcast");
                Log.d(TAG, "用户选择了频道: " + compactId);
                startActivity(broadcastIntent);
            }

            @Override
            public void onItemLongClick(View view, final int position) {
                PopupMenu popCfg = new PopupMenu(uiContext, view);
                MenuInflater menuInft = popCfg.getMenuInflater();
                menuInft.inflate(R.menu.channel_operation, popCfg.getMenu());
                popCfg.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.add_to_top:
                                Log.d(TAG, "置顶");
                                editor.putInt("addToTop", position);
                                editor.commit();
                                // 刷新碎片
                                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                                FirstFragment firstFragment = new FirstFragment();
                                transaction.replace(R.id.main_fragment, firstFragment);
                                transaction.commit();
                                return true;
                            case R.id.add_to_favourate:
                                Log.d(TAG, "关注");
                                Toast.makeText(uiContext, "关注成功", Toast.LENGTH_SHORT).show();
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popCfg.show();
            }
        });

        // 顶部搜索
        RecyclerViewHeader header = RecyclerViewHeader.fromXml(getActivity(),R.layout.navi_header);
        header.attachTo(mRecyclerView);
        TextView searchInfo = (TextView) getActivity().findViewById(R.id.search_info);
        searchInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent searchBroadcastIntent = new Intent(getActivity(), SearchActivity.class);
                searchBroadcastIntent.putExtra("search_type", "search_broadcast");
                startActivity(searchBroadcastIntent);
            }
        });
    }

    // 广播列表
    private void initDatas(){
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        editor = pref.edit();
        selfId = pref.getInt("selfid", 0);    // 用户id
        adverChannel = pref.getString("adverChannel","");
        userChannel = pref.getString("userChannel", "");
        Log.d(TAG, "获取系统频道:" + adverChannel);
        Log.d(TAG, "获取用户频道:" + userChannel);
        String[] advers = adverChannel.split(";");
        String[] users = userChannel.split(";");
        Log.d(TAG, advers.length + "--" + users.length);
        subject = new ArrayList<String>();
        description = new ArrayList<String>();
        int top = pref.getInt("addToTop", 1);
        Log.d(TAG, "置顶：" + 1);
        if(top < advers.length-1){
            String[] temp = advers[top].split("-");
            subject.add(temp[1]);
            description.add(temp[0]);
        }
        for(int i=0; i<advers.length-1; i++){
            if(i != top){
                String[] temp = advers[i].split("-");
                if(!temp[1].startsWith("TOUR_LINK")){
                    subject.add(temp[1]);   // 频道名
                    description.add(temp[0]);       // 频道的CompactId
                }
            }
        }
        for(int i=0; i<users.length-1; i++){
            String[] temp = users[i].split("-");
            subject.add(temp[1]);
            description.add(temp[0]);
        }
        int length = subject.size();

        // 测试图片
        pictures = new ArrayList<String>(
                Arrays.asList("http://wx.qlogo.cn/mmopen/DZtibRDXICYabayGEnDE945eS02pbcBP53kI6LjyLODJqt59NpHVdXf1MHU1CwzRKNXcXt3cEdshTHTEIXsibNh4dVuIMyGfM5/0",
                        "http://wx.qlogo.cn/mmopen/6klL4b65U1MPibPtbQ0N4nPLtPSa45uA501oSBwM34Obvl104c4AONMNVDrmAg8kbpQqjwaT5qic1AN1bNyH63tL8jAZx52bnI/0",
                        "http://wx.qlogo.cn/mmopen/yFGN6Nl8WAzDiaMENnsGRHayiby5jCvPuK08ibF1LBzAku1tQ4icliceNraFL2iaILzcPWBibDHTYFkkkyH8woMjca9XEiaYtUtYm4hia/0",
                        "http://wx.qlogo.cn/mmopen/2sTJHesZN78BxgFpicXd7bf11I2d2OvIDg2Oia20B0lrC9oFERib8chVicFj9LOmEXTicbYD8VvjoAIwyRbWnpTUNs7s2t5fqHk4h/0",
                        "http://wx.qlogo.cn/mmopen/DZtibRDXICYYpbf4ZJZlWKL1RdZEm4RzV0eD72xHBnA9iadXWI2H3FHFBXR82NopvpXCzXiamld6UeEsbedZRzuAN354ibDCUPvz/0",
                        "http://wx.qlogo.cn/mmopen/6klL4b65U1MPibPtbQ0N4nPwj5u6Q2cicibb1FnXcNTm2yEuWj6icy8wb4OzL4xbXSyA6NSop5g8StTqwaqtmwxYHGeGrVPZNXf6/0",
                        "http://wx.qlogo.cn/mmopen/2sTJHesZN78BxgFpicXd7beeghbibiamLumTmpOdt9qXevDvy43sKoBewzUE72acNKD37iaibcIgYuxy7REYiaUm54x99w7IhxBA1y/0"
                ));
        for(int i = 8; i<= length; i++){
            pictures.add("");
        }
    }

    @Override
    public void onAttach(Activity act) {
        super.onAttach(act);
        uiContext = act;
        Log.i(TAG, "onAttach ...");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume ...");
        uiHandler = MainActivity.getUiHandler();
        if (sessionApi == null) {
            uiHandler.postDelayed(delayInitApi, 100);
        } else {

        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy ...");
        if (sessionApi != null) {
            sessionApi.setOnSessionListener(null);
            sessionApi = null;
        } else if (uiHandler != null) {

        }
    }

    // 初始化
    private Runnable delayInitApi = new Runnable() {
        @Override
        public void run() {
            sessionApi = API.getSessionApi();
            if (sessionApi != null) {
//                sessionApi.setOnSessionListener(FirstFragment.this);
                deviceApi = API.getDeviceApi();
            } else {
                uiHandler.postDelayed(delayInitApi, 300);
            }
        }
    };

    // 菜单事件
    public boolean onMainMenuItemClicked(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add_to_top:
                Log.d(TAG, "置顶");
                return true;
            case R.id.add_to_favourate:
                Log.d(TAG, "关注");
                return true;
            default:
                return false;
        }
    }

}