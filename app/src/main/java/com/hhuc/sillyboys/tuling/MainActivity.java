package com.hhuc.sillyboys.tuling;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.algebra.sdk.API;
import com.algebra.sdk.AccountApi;
import com.algebra.sdk.ChannelApi;
import com.algebra.sdk.DeviceApi;
import com.algebra.sdk.OnChannelListener;
import com.algebra.sdk.OnSessionListener;
import com.algebra.sdk.SessionApi;
import com.algebra.sdk.entity.Channel;
import com.algebra.sdk.entity.CompactID;
import com.algebra.sdk.entity.Constant;
import com.algebra.sdk.entity.Contact;
import com.algebra.sdk.entity.IntStr;
import com.hhuc.sillyboys.tuling.chat.ChatAcitivity;
import com.hhuc.sillyboys.tuling.chat.SecretActivity;
import com.hhuc.sillyboys.tuling.entity.ChannelExt;
import com.hhuc.sillyboys.tuling.entity.MsgCode;
import com.hhuc.sillyboys.tuling.navi_fragment.FirstFragment;
import com.hhuc.sillyboys.tuling.navi_fragment.FourthFragment;
import com.hhuc.sillyboys.tuling.navi_fragment.SecondFragment;
import com.hhuc.sillyboys.tuling.navi_fragment.ThirdFragment;
import com.hhuc.sillyboys.tuling.tl_demo.RegisterUser;
import com.hhuc.sillyboys.tuling.util.ProgressCircleLite;
import com.hhuc.sillyboys.tuling.util.StatusBarCompat;
import com.hhuc.sillyboys.tuling.zxing.activity.CaptureActivity;
import com.luseen.luseenbottomnavigation.BottomNavigation.BottomNavigationItem;
import com.luseen.luseenbottomnavigation.BottomNavigation.BottomNavigationView;
import com.luseen.luseenbottomnavigation.BottomNavigation.OnBottomNavigationItemClickListener;
import com.yalantis.contextmenu.lib.ContextMenuDialogFragment;
import com.yalantis.contextmenu.lib.MenuObject;
import com.yalantis.contextmenu.lib.MenuParams;
import com.yalantis.contextmenu.lib.interfaces.OnMenuItemClickListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMenuItemClickListener,OnChannelListener,
        OnSessionListener {
    private static final String TAG = "main";
    private BottomNavigationView bottomNavigationView;
    private android.support.v4.app.FragmentManager manager;
    private ContextMenuDialogFragment mMenuDialogFragment;

    private static SharedPreferences pref;
    private static SharedPreferences.Editor editor;

    // tl_demo
    // ui线程与context
    private static Handler uiHandler = null;
    public static Handler getUiHandler() {
        return uiHandler;
    }
    private Context uiContext = null;
    // 其他
    private RegisterUser registerUser = null;
    private boolean horizonScreen = false;
    private static int startStep = StartStage.INITIALIZING;
    private boolean newBind = true;
    private AccountApi accountApi = null;
    private DeviceApi deviceApi = null;
    private ChannelApi channelApi = null;
    private SessionApi sessionApi = null;
    private int selfId ;
    private ProgressDialog processDialog = null;
    private ProgressCircleLite processCircle = null;
    private boolean userBoundPhone = false;
    private String userAccount = null;
    private String userPass = null;
    private String userNick = "???";
    private String userPhone = null;
    private boolean isVisitor = true;
    private CompactID currSession = null;
    private int selfState = Constant.CONTACT_STATE_OFFLINE;
    private interface StartStage {
        public static final int INITIALIZING = 0;
        public static final int LOGIN_VISITOR = 2;
        public static final int REGISTER_USER = 3;
        public static final int LOGIN_USER = 4;
        public static final int RESET_PASS = 5;
    }
    private ArrayList<ChannelExt> myChannels = null;
    private ArrayList<ChannelExt> tlChannels = null;
    private boolean simpleChannelMode = false;
    private Channel defaultCh = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 状态栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        StatusBarCompat.compat(this, getResources().getColor(R.color.status_bar_color));
        setContentView(R.layout.activity_main);
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
        // 工具栏
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("");
        setSupportActionBar(toolbar);
        // 菜单栏
        manager = getSupportFragmentManager();
        setContextMenu();
        // 导航栏
        setBottomNavi();
        // 持久化
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();

        // tl_demo
        uiContext = this;
        uiHandler = new UiHandler(this);
        horizonScreen = isHorizon();
        if (horizonScreen)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        registerUser = new RegisterUser(MainActivity.this, uiHandler);
        startStep = StartStage.INITIALIZING;
        newBind = API.init(this);
        Log.i(TAG, "onCreate init services, newBind = "+newBind);
        selfId = getIntent().getIntExtra("selfId", 0);  // 可以获取用户的ID
        Log.d(TAG, "selfid = " + selfId);
        editor.putInt("selfid", selfId);
        editor.commit();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.conext_main, menu);
        return true;
    }


    // 菜单选项点击事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // 点击"+"后的操作（显示下拉菜单）
            case R.id.context_menu:
                if (manager.findFragmentByTag(ContextMenuDialogFragment.TAG) == null) {
                    mMenuDialogFragment.show(manager, ContextMenuDialogFragment.TAG);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    // 加载菜单栏1
    private void setContextMenu() {
        MenuParams menuParams = new MenuParams();
        menuParams.setAnimationDelay(200);  // 菜单显示延迟时间
        menuParams.setAnimationDuration(20);  // 子项显示间隔时间
        menuParams.setActionBarSize((int) getResources().getDimension(R.dimen.tool_bar_height));  // 子项高度
        menuParams.setMenuObjects(getMenuObjects());
        menuParams.setClosableOutside(false);
        mMenuDialogFragment = ContextMenuDialogFragment.newInstance(menuParams);  // 创建下拉菜单
        mMenuDialogFragment.setItemClickListener(this);  // 点击事件
    }


    // 加载菜单栏2
    private List<MenuObject> getMenuObjects() {
        List<MenuObject> menuObjects = new ArrayList<>();

        MenuObject close = new MenuObject();
        close.setResource(R.drawable.menu_close_24dp);
        //        close.setResource(R.drawable.icn_close);
        close.setDividerColor(R.color.sub_subject_color);

        MenuObject bottle = new MenuObject("漂流瓶");
        bottle.setResource(R.drawable.menu_bottle_24dp);
//        bottle.setResource(R.drawable.icn_1);

        MenuObject secret = new MenuObject("悄悄话");
        secret.setResource(R.drawable.menu_secret_24dp);
//        Bitmap b = BitmapFactory.decodeResource(getResources(), R.drawable.icn_2);
//        secret.setBitmap(b);

        MenuObject addFriend = new MenuObject("加好友");
        addFriend.setResource(R.drawable.menu_addfriend_bold_24dp);
//        BitmapDrawable bd = new BitmapDrawable(getResources(),
//                BitmapFactory.decodeResource(getResources(), R.drawable.icn_3));
//        addFriend.setDrawable(bd);


        MenuObject scan = new MenuObject("扫一扫");
        scan.setResource(R.drawable.menu_scan_bold_24dp);
//        block.setResource(R.drawable.icn_5);

        menuObjects.add(close);
        menuObjects.add(bottle);
        menuObjects.add(secret);
        menuObjects.add(addFriend);
        menuObjects.add(scan);
        return menuObjects;
    }


    // 弹出菜单
    @Override
    public void onMenuItemClick(View clickedView, int position) {
        switch(position){
            case 1:Log.d(TAG, "漂流瓶");
                break;
            case 2: Log.d(TAG, "悄悄话");
                startActivity(new Intent(MainActivity.this, SecretActivity.class));
                break;
            case 3: Log.d(TAG, "加好友");

                break;
            case 4: Log.d(TAG, "扫码");
                Intent scanCodeIntent = new Intent(MainActivity.this, CaptureActivity.class);
                startActivity(scanCodeIntent);
                break;
            default:
                break;
        }
    }


    // 导航栏
    private void setBottomNavi() {
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavigation);

        // 从xml文件中获取图标与颜色信息
        int[] image = {R.drawable.bottom_broadcast_radio_24dp, R.drawable.bottom_chat_24dp,
                R.drawable.bottom_shop_24dp, R.drawable.bottom_me_24dp};
        int[] color = {ContextCompat.getColor(this, R.color.firstColor), ContextCompat.getColor(this, R.color.secondColor),
                ContextCompat.getColor(this, R.color.thirdColor), ContextCompat.getColor(this, R.color.fourthColor)};

        if (bottomNavigationView != null) {
            // 设置显示提示文字
            bottomNavigationView.isWithText(true);
            // 设置使用背景颜色
            bottomNavigationView.isColoredBackground(false);
            // 设置动态字体大小
            bottomNavigationView.setTextActiveSize(getResources().getDimension(R.dimen.text_active));
            bottomNavigationView.setTextInactiveSize(getResources().getDimension(R.dimen.text_inactive));
            // 设置点击图标时，图标与文字的颜色变化 （需要未使用背景颜色才会生效）
            bottomNavigationView.setItemActiveColorWithoutColoredBackground(ContextCompat.getColor(this, R.color.firstColor));
            // 取消滑动效果
            bottomNavigationView.disableViewPagerSlide();
            // 不新生成activity界面
            bottomNavigationView.willNotRecreate(true);
            // 去除灰色影子*
            bottomNavigationView.disableShadow();
        }

        // 设置导航栏各版块信息
        BottomNavigationItem bottomNavigationItem = new BottomNavigationItem
                ("广播", color[0], image[0]);
        BottomNavigationItem bottomNavigationItem1 = new BottomNavigationItem
                ("聊天", color[1], image[1]);
        BottomNavigationItem bottomNavigationItem2 = new BottomNavigationItem
                ("商家", color[2], image[2]);
        BottomNavigationItem bottomNavigationItem3 = new BottomNavigationItem
                ("我", color[3], image[3]);

        // 将组件添加到底部导航栏中
        bottomNavigationView.addTab(bottomNavigationItem);
        bottomNavigationView.addTab(bottomNavigationItem1);
        bottomNavigationView.addTab(bottomNavigationItem2);
        bottomNavigationView.addTab(bottomNavigationItem3);

        // 设置响应事件
        bottomNavigationView.setOnBottomNavigationItemClickListener(new OnBottomNavigationItemClickListener() {
            @Override
            public void onNavigationItemClick(int index) {
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                switch (index) {
                    case 0:
                        resumeChannelFragment();       // 更新频道信息
                        FirstFragment firstFragment = new FirstFragment();
                        transaction.replace(R.id.main_fragment,firstFragment);
                        transaction.commit();
                        break;
                    case 1:
                        SecondFragment secondFragment = new SecondFragment();
                        transaction.replace(R.id.main_fragment,secondFragment);
                        transaction.commit();
                        break;
                    case 2:
                        ThirdFragment thirdFragment = new ThirdFragment();
                        transaction.replace(R.id.main_fragment,thirdFragment);
                        transaction.commit();
                        break;
                    case 3:
                        FourthFragment fourthFragment = new FourthFragment();
                        transaction.replace(R.id.main_fragment,fourthFragment);
                        transaction.commit();
                        break;
                }
            }
        });

        // 初始化碎片
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        String lastActivity = getIntent().getStringExtra("lastAc");
        if(lastActivity == null){
            FirstFragment firstFragment = new FirstFragment();
            transaction.replace(R.id.main_fragment, firstFragment);
            transaction.commit();
        }else if(lastActivity.equals("fourth")){
            FourthFragment fourthFragment = new FourthFragment();
            transaction.replace(R.id.main_fragment, fourthFragment);
            transaction.commit();
        }
    }





    // tl_demo
    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume ...");
        uiHandler = MainActivity.getUiHandler();
        if (channelApi == null) {
            uiHandler.postDelayed(delayInitApi, 100);
        } else {
            resumeChannelFragment();
        }
    }
    private Runnable delayInitApi = new Runnable() {
        @Override
        public void run() {
            channelApi = API.getChannelApi();
            sessionApi = API.getSessionApi();
            if (channelApi != null && sessionApi != null) {
                channelApi.setOnChannelListener(MainActivity.this);
                sessionApi.setOnSessionListener(MainActivity.this);
                deviceApi = API.getDeviceApi();
                resumeChannelFragment();
            } else {
                uiHandler.postDelayed(delayInitApi, 300);
            }
        }
    };
    private void resumeChannelFragment() {
        if (myChannels == null) {
            channelApi.adverChannelsGet(selfId);    // 系统频道
            channelApi.channelListGet(selfId);  // 用户创建的频道
        } else {
            asyncUpdateChannelUI();
            asyncInitPopupMemberList();
        }
    }


    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop ...");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy ...");
        if (channelApi != null) {
            channelApi.setOnChannelListener(null);
            channelApi = null;
        }
        if (sessionApi != null) {
            sessionApi.setOnSessionListener(null);
            sessionApi = null;
        } else if (uiHandler != null) {
            uiHandler.removeCallbacks(delayInitApi);
        }
        uiHandler = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outBu) {
        super.onSaveInstanceState(outBu);
        Log.i(TAG, "onSaveInstanceState ....");

        outBu.putString("StopByAndroid", "yes");
    }




    // 异步处理
    private static class UiHandler extends Handler {
        WeakReference<MainActivity> wrActi;
        MainActivity mActi = null;
        public UiHandler(MainActivity act) {
            wrActi = new WeakReference<MainActivity>(act);
        }
        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            mActi = wrActi.get();
            if (mActi == null)
                return;
            switch (msg.what) {
                // MainActivity
                case MsgCode.MC_ADDADVERCHANNEL:
                    // 把频道放入数据库中，之后fragment从数据库读取频道信息
                    Log.d(TAG, "保存系统频道:" + msg.obj.toString());
                    editor.putString("adverChannel",msg.obj.toString());
                    editor.commit();
                    break;
                case MsgCode.MC_ADDUSERCHANNEL:
                    Log.d(TAG, "保存用户频道" + msg.obj.toString());
                    editor.putString("userChannel",msg.obj.toString());
                    editor.commit();
                    break;
                case MsgCode.MC_SAVECHANNELNAME:
                    String[] info1 = msg.obj.toString().split("-");
                    editor.putString(info1[0] + "name", info1[1]);
                    editor.commit();
                    break;
                case MsgCode.MC_SAVECHANNELTYPE:
                    String[] info2 = msg.obj.toString().split("-");
                    editor.putString(info2[0] + "type", info2[1]);
                    editor.commit();
                    break;
                case MsgCode.MC_SAVECHANNELID:
                    String[] info3 = msg.obj.toString().split("-");
                    editor.putString(info3[0] + "id", info3[1]);
                    editor.commit();
                    break;


                default:
                    Log.e(TAG, "uiHandler unexpected msg: " + msg.what);
                    break;
            }
        }
    };

    // 检测横竖屏
    private boolean isHorizon() {
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;
        int height = metric.heightPixels;
        float density = metric.density;
        int densityDpi = metric.densityDpi;
        Log.i(TAG, "width:" + width + " height:" + height + " density:"
                + density + " dpi:" + densityDpi);
        return (width > height);
    }

    // 同步频道UI
    private void asyncUpdateChannelUI() {
        if (uiHandler != null)
            uiHandler.post(new AsyncUpdateChannelState());
    }
    private class AsyncUpdateChannelState implements Runnable {
        @Override
        public void run() {

        }
    }
    // 同步频道成员
    private void asyncInitPopupMemberList() {
        if (uiHandler != null) {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    initPopupMemberList();
                }
            });
        } else {
            Log.e(TAG, "init popupMemberList canceled. uiHandler is null: " + (uiHandler == null));
        }
    }
    private void initPopupMemberList() {

    }


    // 进度条操作
    public void showProcessing(String hintText) {
        if (processDialog == null) {
            processDialog = new ProgressDialog(this);
        }
        processDialog.setMessage(hintText);
        processDialog.setCancelable(true);
        processDialog.show();
    }
    public void dismissProcessing() {
        if (processDialog != null && processDialog.isShowing()) {
            processDialog.dismiss();
            processDialog = null;
        }
    }
    public void showProgressCircle() {
        if (processCircle == null) {
            processCircle = ProgressCircleLite.show(MainActivity.this, "", "",
                    false, true);
        }
    }


    /**
     *  接口回调
     */
    @Override   // 默认频道（不设置）
    public void onDefaultChannelSet(int userId, int chType, int defaultChId) {
    }

    @Override   // 获取系统频道
    public void onAdverChannelsGet(int userId, Channel dfltChannel, @SuppressWarnings("rawtypes") List chs) {
        @SuppressWarnings("unchecked")
        List<Channel> channels = (List<Channel>) chs;
        ((MainActivity) uiContext).dismissProcessing();
        // 获取失败，则重新获取
        if (userId <= 0 || channels == null) {
            Log.e(TAG, "get adver channels error!");
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    channelApi.adverChannelsGet(selfId);    // 获取系统频道
                }
            }, 3000);
            return;
        }
        // 获取成功，则暂存
        StringBuffer sb = new StringBuffer();
        Log.d(TAG, "系统频道:");
        for(Channel ch: channels){
            String str1 = ch.cid + "-" + ch.name;
            sb.append(str1 + ";");
            uiHandler.obtainMessage(MsgCode.MC_SAVECHANNELNAME, 0, 0, str1).sendToTarget();
            String str2 = ch.cid + "-" + ch.cid.getType();
            uiHandler.obtainMessage(MsgCode.MC_SAVECHANNELTYPE, 0, 0, str2).sendToTarget();
            String str3 = ch.cid + "-" + ch.cid.getId();
            uiHandler.obtainMessage(MsgCode.MC_SAVECHANNELID, 0, 0, str3).sendToTarget();
        }
        Log.d(TAG, sb.toString());
        // 键值对：cid - cname;
        uiHandler.obtainMessage(MsgCode.MC_ADDADVERCHANNEL, 0, 0, sb.toString()).sendToTarget();
    }


    @Override   // 获取用户频道
    public void onChannelListGet(int userId, Channel dfltChannel, @SuppressWarnings("rawtypes") List chs) {
        List<Channel> channels = (List<Channel>) chs;
        ((MainActivity) uiContext).dismissProcessing();
        // 获取失败，则重新获取
        if (userId <= 0 || channels == null) {
            Log.e(TAG, "get channel lists error!");
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    channelApi.channelListGet(selfId);    //  用户频道
                }
            },  3000);
            return;
        }
        // 获取成功，则暂存
        StringBuffer sb = new StringBuffer();
        Log.d(TAG, "用户频道:");
        for(Channel ch: channels){
            String str1 = ch.cid + "-" + ch.name;
            sb.append(str1 + ";");
            uiHandler.obtainMessage(MsgCode.MC_SAVECHANNELNAME, 0, 0, str1).sendToTarget();
            String str2 = ch.cid + "-" + ch.cid.getType();
            uiHandler.obtainMessage(MsgCode.MC_SAVECHANNELTYPE, 0, 0, str2).sendToTarget();
            String str3 = ch.cid + "-" + ch.cid.getId();
            uiHandler.obtainMessage(MsgCode.MC_SAVECHANNELID, 0, 0, str3).sendToTarget();
        }
        Log.d(TAG, sb.toString());
        uiHandler.obtainMessage(MsgCode.MC_ADDUSERCHANNEL, 0, 0, sb.toString()).sendToTarget();
    }


    private Channel createChannel = null;
    @Override   // 频道创建
    public void onPubChannelCreate(int uid, int reason, int cid) {
//        if (uid > 0) {
//            Log.d(TAG, "新建的频道cid为：" + cid);
//            Toast.makeText(uiContext, getResources().getString(R.string.create_success), Toast.LENGTH_SHORT).show();
//            String roomName = pref.getString(uid + "", "");
//            editor.putString(uid + "", roomName + ";" + cid);
//            editor.commit();
//            Log.d(TAG, "创建的房间为" + pref.getString(uid + "", ""));
//            if (myChannels != null) {
//                ChannelExt che1 = new ChannelExt(Constant.SESSION_TYPE_CHANNEL, cid, createChannel.name);
//                che1.needPassword = createChannel.needPassword;
//                che1.owner = new IntStr(selfId, API.uid2nick(selfId));
//                boolean has = false;
//                for (ChannelExt che2 : myChannels) {
//                    if (Channel.sameCid(che1.cid, che2.cid)) {
//                        has = true;
//                        break;
//                    }
//                }
//                if (!has)
//                    myChannels.add(che1);
//                if (myChannels.size() == 1)
//                    uiHandler.obtainMessage(MsgCode.MC_ONDISPLAYCHANGED, che1.cid.getType(), che1.cid.getId()).sendToTarget();
//                asyncUpdateChannelUI();
//            }
//        } else {
//            Toast.makeText(uiContext, "创建失败", Toast.LENGTH_SHORT).show();
//        }
    }


    // 以下回掉暂时不需要使用
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

    @Override
    public void onSessionEstablished(int i, int i1, int i2) {

    }

    @Override
    public void onSessionReleased(int i, int i1, int i2) {

    }

    @Override
    public void onSessionGet(int i, int i1, int i2, int i3) {

    }

    @Override
    public void onSessionPresenceAdded(int i, int i1, List<Contact> list) {

    }

    @Override
    public void onSessionPresenceRemoved(int i, int i1, List<Integer> list) {

    }

    @Override
    public void onDialogEstablished(int i, int i1, int i2, List<Integer> list) {

    }

    @Override
    public void onDialogLeaved(int i, int i1) {

    }

    @Override
    public void onDialogPresenceAdded(int i, int i1, List<Integer> list) {

    }

    @Override
    public void onDialogPresenceRemoved(int i, int i1, List<Integer> list) {

    }
}
