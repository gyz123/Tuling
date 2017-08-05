package com.hhuc.sillyboys.tuling.search;

import android.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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

import com.algebra.sdk.API;
import com.algebra.sdk.ChannelApi;
import com.algebra.sdk.OnChannelListener;
import com.algebra.sdk.entity.Channel;
import com.algebra.sdk.entity.Contact;
import com.dinuscxj.itemdecoration.ShaderItemDecoration;
import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.adapter.RoundImgAdapter;
import com.hhuc.sillyboys.tuling.broadcast.BroadcastActivity;
import com.hhuc.sillyboys.tuling.chat.ChatAcitivity;
import com.hhuc.sillyboys.tuling.entity.MsgCode;
import com.hhuc.sillyboys.tuling.navi_fragment.SecondFragment;
import com.hhuc.sillyboys.tuling.util.DividerItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class SearchPeopleFragment extends Fragment implements OnChannelListener{
    private static final String TAG = "search_result";
    private RecyclerView mRecyclerView;
    private RoundImgAdapter mAdapter;
    private List<String> subject,description,pictures;
    private TextView hint;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private int selfId = 0;
    private String keyword = "";
    private String myFriends = "";

    private ChannelApi channelApi = null;
    private Handler uiHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MsgCode.SAVECRATEDCHANNEL:
                    String[] info = msg.obj.toString().split(";");  // cname;cid
                    SecondFragment.saveChannel(selfId, info[1], info[0]);
                    break;
                default:
                    break;
            }
        }
    };

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
                // 房间名：我的昵称-对方昵称
                String roomName = API.uid2nick(selfId) + "-" + subject.get(position);
                if(channelApi != null){
                    channelApi.createPublicChannel(selfId, roomName, null);   // 用户id，频道名，密码
                    editor.putString(selfId+"", roomName);
                    editor.commit();
                }else {
                    Intent broadcastIntent = new Intent(getActivity(), BroadcastActivity.class);
                    broadcastIntent.putExtra("cname", "好友聊天").putExtra("type", "chat");
                    startActivity(broadcastIntent);
                }

            }

            @Override
            public void onItemLongClick(View view, int position) {
            }
        });

        // 提示文字
        hint = (TextView)getActivity().findViewById(R.id.search_hint);
        if(subject.size() == 0){
            hint.setText("未找到相关用户");
        }else{
            hint.setText("相关用户");
        }
        hint.setGravity(Gravity.CENTER);
    }

    private void initDatas(){
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        editor = pref.edit();
        selfId = pref.getInt("selfid", 0);    // 用户id
        keyword = pref.getString("keyword", "");    // 搜索关键字
        myFriends = pref.getString("myFriends", "");

        try{
            channelApi = API.getChannelApi();
            channelApi.setOnChannelListener(SearchPeopleFragment.this);
        }catch (Exception e){
            Log.d(TAG, "channelApi初始化失败");
        }

        subject = new ArrayList<String>();
        description = new ArrayList<String>();
        pictures = new ArrayList<String>();

        String[] users = myFriends.split(";");
        Log.d(TAG, "我的好友：" + users);

        for(int i=0; i<users.length; i++){
            String userName = users[i];
            if(userName.contains(keyword)){
                subject.add(userName);   // 用户名
                description.add("");
            }
        }
        for(int i = 0; i<subject.size(); i++){
            pictures.add("");
        }
    }

    // 创建频道
    @Override
    public void onPubChannelCreate(int uid, int reason, int cid) {
        if (uid > 0) {
            String roomName = pref.getString(uid + "", "");
            editor.putString(uid + "", roomName + ";" + cid);
            editor.commit();
            Log.d(TAG, "新建的频道cid为：" + cid + ",创建的房间为" + pref.getString(uid + "", ""));
            uiHandler.obtainMessage(MsgCode.SAVECRATEDCHANNEL, uid, 0, roomName + ";" + cid).sendToTarget();

            Intent chatIntent = new Intent(getActivity(), BroadcastActivity.class);
            chatIntent.putExtra("compactId", cid)
                    .putExtra("cname", roomName)
                    .putExtra("type", "chat");
            startActivity(chatIntent);
        }
    }

    @Override
    public void onDefaultChannelSet(int i, int i1, int i2) {

    }

    @Override
    public void onAdverChannelsGet(int i, Channel channel, List<Channel> list) {

    }

    @Override
    public void onChannelListGet(int i, Channel channel, List<Channel> list) {

    }

    @Override
    public void onChannelMemberListGet(int i, int i1, int i2, List<Contact> list) {

    }

    @Override
    public void onChannelNameChanged(int i, int i1, int i2, String s) {

    }

    @Override
    public void onChannelAdded(int i, int i1, int i2, String s) {

    }

    @Override
    public void onChannelRemoved(int i, int i1, int i2) {

    }

    @Override
    public void onChannelMemberAdded(int i, int i1, List<Contact> list) {

    }

    @Override
    public void onChannelMemberRemoved(int i, int i1, List<Integer> list) {

    }

       @Override
    public void onPubChannelSearchResult(int i, List<Channel> list) {

    }

    @Override
    public void onPubChannelFocusResult(int i, int i1) {

    }

    @Override
    public void onPubChannelUnfocusResult(int i, int i1) {

    }

    @Override
    public void onPubChannelRenamed(int i, int i1) {

    }

    @Override
    public void onPubChannelDeleted(int i, int i1) {

    }

    @Override
    public void onCallMeetingStarted(int i, int i1, int i2, List<Contact> list) {

    }

    @Override
    public void onCallMeetingStopped(int i, int i1) {

    }
}
