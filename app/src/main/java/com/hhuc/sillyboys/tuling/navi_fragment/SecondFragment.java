package com.hhuc.sillyboys.tuling.navi_fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import com.hhuc.sillyboys.tuling.search.SearchActivity;
import com.hhuc.sillyboys.tuling.util.DividerItemDecoration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SecondFragment extends Fragment implements OnChannelListener{
    private static final String TAG = "secondFragment";
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private int selfId = 0;
    private String nickname = "";

    private TextView searchPeople;
    private RecyclerView mRecyclerView, mRecyclerView2;
    private RoundImgAdapter mAdapter, mAdapter2;
    private List<String> subject,description,pictures, subject2,description2,pictures2;
    private StringBuffer sb;

    private ChannelApi channelApi = null;
    public Handler uiHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MsgCode.SAVECRATEDCHANNEL:
                    String[] info = msg.obj.toString().split(";");  // cname;cid
                    saveChannel(selfId, info[1], info[0]);
                    break;
                case MsgCode.MC_ADDMYFRIENDS:
                    editor.putString("myFriends", msg.obj.toString());
                    editor.commit();
                    break;
                default:
                    break;
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.navi_fragment_second, container, false);
        Log.d(TAG, "onCreatView");
        return view;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        TextView toolbarText = (TextView)getActivity().findViewById(R.id.toolbar_text);
        toolbarText.setText("群聊/私聊");
        init();
        initSingle();
        initGroup();
        uiHandler.obtainMessage(MsgCode.MC_ADDMYFRIENDS, 0, 0, sb.toString()).sendToTarget();
    }

    private void init(){
        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        editor = pref.edit();
        sb = new StringBuffer();
        selfId = pref.getInt("selfid", 0);    // 用户id
        try{
            nickname = API.uid2nick(selfId);    // 用户昵称
            channelApi = API.getChannelApi();
            channelApi.setOnChannelListener(SecondFragment.this);
        }catch (Exception e){
            Log.d(TAG, "channelApi初始化失败,设置为默认昵称");
            nickname = "test_name";
        }

        // 顶部搜索
        searchPeople = (TextView)getActivity().findViewById(R.id.search_people);
        searchPeople.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent searchPeopleIntent = new Intent(getActivity(), SearchActivity.class);
                searchPeopleIntent.putExtra("search_type", "search_people");
                startActivity(searchPeopleIntent);
            }
        });
    }

    // 单聊
    private void initSingle(){
        mRecyclerView = (RecyclerView)getActivity().findViewById(R.id.second_recyclerView_single);

        subject = new ArrayList<String>(Arrays.asList("豆豆","小柯","马哲","红哥"));
        Iterator<String> i = subject.iterator();
        while(i.hasNext()){
            sb.append(i.next().toString() + ";");
        }
        description = new ArrayList<String>(Arrays.asList("","","",""));
        // 测试图片
        pictures = new ArrayList<String>(
                Arrays.asList("" + resourceIdToUri(getActivity(), R.drawable.friend_doudou),
                        "" + resourceIdToUri(getActivity(), R.drawable.friend_xiaoke),
                        "" + resourceIdToUri(getActivity(), R.drawable.friend_mazhe),
                        "" + resourceIdToUri(getActivity(), R.drawable.friend_hongge)
                ));

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

        // 点击事件
        mAdapter.setOnItemClickListener(new RoundImgAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                // 创建频道
                String roomName = nickname + "-" + subject.get(position);
                if(channelApi != null){
                    channelApi.createPublicChannel(selfId, roomName, null);   // 用户id，频道名，密码
                    editor.putString(selfId+"", roomName);
                    editor.commit();
                }else{
                    Intent broadcastIntent = new Intent(getActivity(), BroadcastActivity.class);
                    broadcastIntent.putExtra("cname", "好友聊天").putExtra("type", "chat");
                    startActivity(broadcastIntent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
            }
        });

    }

    // 群聊
    private void initGroup(){
        mRecyclerView2 = (RecyclerView)getActivity().findViewById(R.id.second_recyclerView_group);

        subject2 = new ArrayList<String>(Arrays.asList("g17"));
        Iterator<String> i = subject2.iterator();
        while(i.hasNext()){
            sb.append(i.next().toString() + ";");
        }
        description2 = new ArrayList<String>(Arrays.asList(""));
        // 测试图片
        pictures2 = new ArrayList<String>(
                Arrays.asList("http://wx.qlogo.cn/mmopen/DZtibRDXICYabayGEnDE945eS02pbcBP53kI6LjyLODJqt59NpHVdXf1MHU1CwzRKNXcXt3cEdshTHTEIXsibNh4dVuIMyGfM5/0"
                ));

        mAdapter2 = new RoundImgAdapter(getActivity(), subject2, description2, pictures2);
        mRecyclerView2.setAdapter(mAdapter2);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mRecyclerView2.getContext(),LinearLayoutManager.VERTICAL,false);
        mRecyclerView2.setLayoutManager(linearLayoutManager);

        // 设置Item之间的分割线
        mRecyclerView2.addItemDecoration(new DividerItemDecoration(getActivity(),DividerItemDecoration.VERTICAL_LIST));
        ShaderItemDecoration shaderItemDecoration = new ShaderItemDecoration(getActivity(),
                ShaderItemDecoration.SHADER_BOTTOM | ShaderItemDecoration.SHADER_TOP);
        shaderItemDecoration.setShaderTopDistance(1);
        shaderItemDecoration.setShaderBottomDistance(1);
        mRecyclerView2.addItemDecoration(shaderItemDecoration);

        // 点击事件
        mAdapter2.setOnItemClickListener(new RoundImgAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                // 创建频道
                String roomName = subject2.get(position);
                if(channelApi != null){
                    channelApi.createPublicChannel(selfId, roomName, null);   // 用户id，频道名，密码
                    editor.putString(selfId + "", roomName);
                    editor.commit();
                }else{
                    Intent broadcastIntent = new Intent(getActivity(), BroadcastActivity.class);
                    broadcastIntent.putExtra("cname", "好友聊天").putExtra("type", "group");
                    startActivity(broadcastIntent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
    }

    public static final String ANDROID_RESOURCE = "android.resource://";
    public static final String FOREWARD_SLASH = "/";
    private static Uri resourceIdToUri(Context context, int resourceId) {
        return Uri.parse(ANDROID_RESOURCE + context.getPackageName() + FOREWARD_SLASH + resourceId);
    }


    /**
     *  接口回调
     */
    @Override   // 频道创建
    public void onPubChannelCreate(int uid, int reason, int cid) {
        if (uid > 0) {
//            String roomName = pref.getString(uid + "", "");
            String roomName = "好友聊天";
            editor.putString(uid + "", roomName + ";" + cid);
            editor.commit();
            Log.d(TAG, "新建的频道cid为：" + cid + ",创建的房间为" + pref.getString(uid + "", ""));
            uiHandler.obtainMessage(MsgCode.SAVECRATEDCHANNEL, uid, 0, roomName + ";" + cid).sendToTarget();

//            Intent chatIntent = new Intent(getActivity(), ChatAcitivity.class);
//            chatIntent.putExtra("compactId", cid).putExtra("cname", roomName);
//            startActivity(chatIntent);
            // 进入创建好的频道
            Intent broadcastIntent = new Intent(getActivity(), BroadcastActivity.class);
            broadcastIntent.putExtra("compactId", cid)
                            .putExtra("cname", roomName)
                            .putExtra("type", "chat");
            startActivity(broadcastIntent);
        }
    }

    // 存储频道信息
    public static void saveChannel(int selfId, String cid, String cname){
        OkHttpClient client = new OkHttpClient();
        FormBody formBody = new FormBody.Builder()
                .add("userid", selfId + "")
                .add("cid", cid)
                .add("cname", cname)
                .build();
        Request request = new Request.Builder()
                .url("http://www.iotesta.com.cn/library/searchlist.action")     // 网址需要修改
                .post(formBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "服务器发送错误");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String result = response.body().string();
                Log.d(TAG, "保存成功：" + result);
            }
        });
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
