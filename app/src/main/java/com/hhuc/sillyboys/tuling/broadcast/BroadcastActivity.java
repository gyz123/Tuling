package com.hhuc.sillyboys.tuling.broadcast;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.algebra.sdk.API;
import com.algebra.sdk.AccountApi;
import com.algebra.sdk.DeviceApi;
import com.algebra.sdk.OnAccountListener;
import com.algebra.sdk.OnSessionListener;
import com.algebra.sdk.SessionApi;
import com.algebra.sdk.entity.AudioDev;
import com.algebra.sdk.entity.Channel;
import com.algebra.sdk.entity.CompactID;
import com.algebra.sdk.entity.Constant;
import com.algebra.sdk.entity.Contact;
import com.algebra.sdk.entity.IntStr;
import com.algebra.sdk.entity.UserProfile;
import com.algebra.sdk.entity.Utils;
import com.hhuc.sillyboys.tuling.MainActivity;
import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.entity.ChannelExt;
import com.hhuc.sillyboys.tuling.entity.MsgCode;
import com.hhuc.sillyboys.tuling.entity.ObservableScrollView;
import com.hhuc.sillyboys.tuling.entity.ScrollViewListener;
import com.hhuc.sillyboys.tuling.tl_demo.ChannelFragment;
import com.hhuc.sillyboys.tuling.tl_demo.RegisterUser;
import com.hhuc.sillyboys.tuling.tl_demo.TalkFragment;
import com.hhuc.sillyboys.tuling.util.DownloadMan;
import com.hhuc.sillyboys.tuling.util.ProgressCircleLite;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class BroadcastActivity extends AppCompatActivity implements OnAccountListener,OnSessionListener{
    private static final String TAG = "broadcast";
    private static Handler uiHandler = null;
    public static Handler getUiHandler() {
        return uiHandler;
    }
    private Context uiContext = null;
    private static int startStep = StartStage.INITIALIZING;
    private ChannelFragment channelFragment = null;
    private TalkFragment talkFragment = null;
    private DeviceApi deviceApi = null;
    private ProgressDialog processDialog = null;
    private ProgressCircleLite processCircle = null;
    private CompactID currSession = null;
    private int selfState = Constant.CONTACT_STATE_OFFLINE;
    private boolean isVisitor = false;
    private interface StartStage {
        public static final int INITIALIZING = 0;
    }
    private AccountApi accountApi = null;
    private boolean userBoundPhone = false;
    private String userAccount = null;
    private String userPass = null;
    private String userNick = "???";
    private String userPhone = null;
    private SessionApi sessionApi = null;
    private boolean newBind = true;
    private List<IntStr> sessPresences = null;
    private RegisterUser registerUser = null;

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private int selfId = 0;
    private int ctype = 0;
    private int cid = 0;
    private String cname = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate..");

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        editor = pref.edit();
        selfId = pref.getInt("selfid", 0);
        uiContext = this;
        uiHandler = new UiHandler(this);
        registerUser = new RegisterUser(BroadcastActivity.this, uiHandler);
        startStep = StartStage.INITIALIZING;
        newBind = API.init(this);
        Log.i(TAG, "开启对讲服务："+newBind);

        setContentView(R.layout.welcome);

        String compactId = getIntent().getStringExtra("compactId");
        String channelId = pref.getString(compactId + "id", "");
        String channelType = pref.getString(compactId + "type", "");
        cname = pref.getString(compactId + "name", "");
        ctype = Integer.parseInt(channelType);
        cid = Integer.parseInt(channelId);
        Log.d(TAG, "选择了频道:" + cname + ",频道id为:" + cid + ",频道类型:" + ctype);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume ..");
        if (startStep == StartStage.INITIALIZING)
            uiHandler.postDelayed(delayInitApi, 300);
    }
    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG, "onStop ..");
    }
    private boolean needUnbind = true;
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (needUnbind)
            API.leave();
        needUnbind = false;
        uiHandler = null;
        if (sessionApi != null) {
            sessionApi.setOnSessionListener(null);
            sessionApi = null;
        }
        Log.i(TAG, "onDestroy ..");
    }
    @Override
    public void onSaveInstanceState(Bundle outBu) {
        super.onSaveInstanceState(outBu);
        Log.i(TAG, "onSaveInstanceState ....");

        outBu.putString("StopByAndroid", "yes");
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toast.makeText(this, "landscape", Toast.LENGTH_SHORT).show();
        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Toast.makeText(this, "portrait", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(BroadcastActivity.this, MainActivity.class));
    }

    /**
     *  异步处理
     */
    private class UiHandler extends Handler {
        WeakReference<BroadcastActivity> wrActi;
        BroadcastActivity mActi = null;

        public UiHandler(BroadcastActivity act) {
            wrActi = new WeakReference<BroadcastActivity>(act);
        }

        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            mActi = wrActi.get();
            if (mActi == null)
                return;
            switch (msg.what) {
                //  退出
                case MsgCode.ASKFOREXIT:
                    mActi.setContentView(R.layout.welcome);
                    mActi.finish();
                    break;
                case MsgCode.MC_SDKISRUNNING:
                    mActi.selfId = selfId ;
                    mActi.start_channel_fragment(mActi.selfId, msg.arg2);
                    mActi.start_talk_fragment(mActi.selfId, msg.arg2);
//                    channelFragment.uiEnterChannel(ctype, cid);
                break;
                //  会话创建成功
                case MsgCode.MC_ONSESSIONESTABLISHED:
                    mActi.currSession = new CompactID(msg.arg1, msg.arg2);
                    if (mActi.talkFragment != null)
                        mActi.talkFragment.onCurrentSessionChanged(mActi.selfId,
                                mActi.currSession.getType(), mActi.currSession.getId());
                    if (mActi.channelFragment != null)
                        mActi.channelFragment.onStartTalkFragment(mActi.selfId,
                                mActi.currSession.getType(), mActi.currSession.getId());
                    mActi.selfId = selfId ;
                    mActi.start_channel_fragment(mActi.selfId, msg.arg2);
                    mActi.start_talk_fragment(mActi.selfId, msg.arg2);
                    break;
                case MsgCode.MC_ONSESSIONRELEASED:
                    mActi.currSession = null;
                    if (mActi.talkFragment != null)
                        mActi.talkFragment.onCurrentSessionChanged(mActi.selfId,
                                Constant.SESSION_TYPE_NONE, 0);
                    break;
                case MsgCode.MC_ONDISPLAYCHANGED:
                    if (mActi.talkFragment != null)
                        mActi.talkFragment.onDisplaySessionChanged(mActi.selfId, msg.arg1, msg.arg2);
                    break;
                case MsgCode.MC_SESSIONACTIVE:
                    if (mActi.channelFragment != null)
                        mActi.channelFragment.onSessionActive(msg.arg1, msg.arg2);
                    break;
                case MsgCode.MC_CHANNELMEMBERSGET:
                    break;
                case MsgCode.MC_SELECTEDMEMBERS:
//                    int[] ids = (int[]) msg.obj;
//                    mActi.showSelectedMemberDialog(msg.arg1, msg.arg2, ids);
                    break;
                case MsgCode.MC_OUTPUTINDREQ:
                    break;
                case MsgCode.MC_LINECONTROLBUTTON:
//                    if (msg.arg2 != 79 || mActi.talkFragment == null)
//                        break;
//                    if (!mActi.lineMuteDown && msg.arg1 == MotionEvent.ACTION_DOWN) {
//                        mActi.talkFragment.processPttAction(MotionEvent.ACTION_DOWN);
//                        mActi.lineMuteDown = true;
//                    } else if (mActi.lineMuteDown
//                            && msg.arg1 == MotionEvent.ACTION_UP) {
//                        mActi.talkFragment.processPttAction(MotionEvent.ACTION_UP);
//                        mActi.lineMuteDown = false;
//                    }
                    break;
                case MsgCode.MC_UPDATECLIENT:
                    mActi.showUpdateDialog();
                    break;
                case MsgCode.MC_DOWNLOADFAILURE:
                    Toast.makeText(mActi.uiContext, "Sorry. UPGRADE Failed!", Toast.LENGTH_SHORT)
                            .show();
                    break;
                default:
                    Log.e(TAG, "uiHandler unexpected msg: " + msg.what);
                    break;
            }
        }
    };


    private Runnable delayInitApi = new Runnable() {
        @Override
        public void run() {
            accountApi = API.getAccountApi();
            deviceApi = API.getDeviceApi();
            if (accountApi != null && deviceApi != null) {
                accountApi.setOnAccountListener(BroadcastActivity.this);
                Contact me = accountApi.whoAmI();
                if (me != null) {
                    userBoundPhone = !me.phone.equals("none");
                    isVisitor = me.visitor;
                    userNick = new String(me.name);
                    userAccount = registerUser.getUserAccount();
                    android.util.Log.d(TAG, "Poc sdk for uid: " + me.id
                            + " is running, self state:" + me.state
                            + ", link in.");
                    uiHandler.obtainMessage(MsgCode.MC_SDKISRUNNING, me.id,
                            me.state).sendToTarget();
                } else {
                    uiHandler.sendEmptyMessage(MsgCode.ASKFORSTARTSDK);
                }
            } else {
                if (uiHandler != null) {
                    android.util.Log.d(TAG, "start SDK and waiting another 300ms.");
                    uiHandler.postDelayed(delayInitApi, 300);
                }
            }

        }
    };
    private void installApk(File updFile) {
        Intent intent = new Intent();
        intent.setAction(intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(updFile),
                "application/vnd.android.package-archive");
        startActivity(intent);
    }
    private void showUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(BroadcastActivity.this,
                R.style.menuDialogStyle);
        builder.setTitle(getResources().getString(R.string.update_hint));
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                downLoadApk();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }
    private static final String DownloadApkUri = "http://121.199.44.69:8081/miniTL-dev.apk";
    private void downLoadApk() {
        final ProgressDialog pd;
        pd = new ProgressDialog(BroadcastActivity.this);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setMessage("download ...");
        pd.show();

        new Thread() {
            @Override
            public void run() {
                Log.i(TAG, "new thread created for download.");
                try {
                    File file = DownloadMan.getFileFromServer(DownloadApkUri,
                            pd);
                    sleep(1000);
                    installApk(file);
                } catch (Exception e) {
                    uiHandler.obtainMessage(MsgCode.MC_DOWNLOADFAILURE, 0, 0)
                            .sendToTarget();
                    e.printStackTrace();
                }
                pd.dismiss();
            }
        }.start();
    }

    /**
     * 创建菜单（那个工具一样的图标）
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        modiMenuStatus(menu);
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        modiMenuStatus(menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (onMainMenuItemClicked(item))
            return true;
        else
            return super.onOptionsItemSelected(item);
    }

    // 设置某些菜单项是否要显示
    public void modiMenuStatus(Menu menu) {
//        menu.getItem(0).setChecked(isBluetoothStarted());
//        if (talkFragment != null)
//            menu.getItem(1).setChecked(talkFragment.getPttTrigglable());
//        menu.getItem(4).setVisible(!isVisitor);
//        menu.getItem(5).setVisible(!isVisitor && !userBoundPhone);
    }
    public boolean isBluetoothStarted() {
        if (deviceApi != null) {
            AudioDev aDev = deviceApi.getCurrentAudioDevice();
            int curDev = aDev.type;
            bluetoothStarted = (curDev == Constant.AUDIO_BLUEHANDSET);
            return bluetoothStarted;
        }
        return false;
    }
    // 主菜单点击事件
    private boolean bluetoothStarted = false;
    public boolean onMainMenuItemClicked(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_change_pass:   // 修改密码
                showChangePassDialog();
                return true;
            case R.id.action_change_nick:   // 修改昵称
                showChangeNickDialog();
                return true;
            case R.id.action_bluetooth:     // 蓝牙手咪
                if (!isBluetoothStarted()) {
                    item.setChecked(true);
                    chooseAndStartBlt(deviceApi.getPairedBluetooths(), new SelectCallBack() {
                        @Override
                        public void onItem(String name, String addr) {
                            boolean isOK = deviceApi.setBluetoothOn(name, addr, true);
                            if (isOK) {
                                bluetoothStarted = true;
                                talkFragment.setPttTrigglableOn(false);
                                talkFragment.setOutputDeviceInd(TalkFragment.OUT_BLUETOOTH_DISC);
                            } else {
                                Toast.makeText(BroadcastActivity.this, getResources().getString(R.string.connect_bluetooth_failed1) + name + getResources().getString(R.string.connect_bluetooth_failed2), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {
                    item.setChecked(false);
                    bluetoothStarted = false;
                    deviceApi.setBluetoothOn(null, null, false);
                    talkFragment.setOutputDeviceInd(TalkFragment.OUT_PHONEBODY);
                }
                return true;
            case R.id.action_trig_ptt:      // PTT单次触发
                if (!item.isChecked()) {
                    item.setChecked(true);
                    talkFragment.setPttTrigglableOn(true);
                } else {
                    item.setChecked(false);
                    talkFragment.setPttTrigglableOn(false);
                }
                return true;
            case R.id.action_bind_phone:    // 绑定手机
                showBindingPhone();
                return true;
            case R.id.action_set_audio:     // 音频属性设置
                showAdjustAudioLevel();
                return true;
            case R.id.action_app_version:   // APP信息
                showAppVersion();
                return true;
            case R.id.action_sms:           // 发送验证短信
                showSendSmsDialog();
                return true;
            case R.id.action_exit:          // 完全退出
                userExit();
                userLogout();
                return true;
            default:
                return false;
        }
    }
    // 注销登录
    private void userLogout() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();
        if (selfId > 0) {
            accountApi.logout(selfId);
            selfId = 0;
        }
    }
    // 退出软件
    private void userExit() {
        stop_both_fragments();
        if (uiHandler != null) {
            uiHandler.sendEmptyMessage(MsgCode.ASKFOREXIT);
        } else {
            BroadcastActivity.this.finish();
        }
    }
    // 关闭界面
    private void stop_both_fragments() {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        if (channelFragment != null)
            transaction.remove(channelFragment);
        if (talkFragment != null)
            transaction.remove(talkFragment);
        try {
            transaction.commit();
        } catch (IllegalStateException e) {
        }
        channelFragment = null;
        talkFragment = null;
        return;
    }
    // 菜单：修改密码
    private void showChangePassDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.change_pass_dialog, null);
        final EditText oldPass = (EditText) promptsView
                .findViewById(R.id.old_pass);
        final EditText newPass = (EditText) promptsView
                .findViewById(R.id.new_pass1);
        final EditText newPass2 = (EditText) promptsView
                .findViewById(R.id.new_pass2);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this,
                R.style.menuDialogStyle);
        alertDialogBuilder.setView(promptsView);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String newp1 = newPass.getText().toString();
                        String newp2 = newPass2.getText().toString();
                        if (newp1 != null && newp1.equals(newp2)
                                && newp1.length() >= 4) {
                            String oldp = API.md5(oldPass.getText().toString());
                            accountApi.setPassWord(selfId, userAccount, oldp,
                                    API.md5(newp1));
                        } else {
                            Toast.makeText(uiContext, getResources().getString(R.string.input_format_error),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("CANCEL",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        return;
    }
    // 菜单：修改昵称
    private void showChangeNickDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_change_nick, null);
        TextView tvOld = (TextView) promptsView.findViewById(R.id.old_nick);
        tvOld.setText(API.uid2nick(selfId));
        final EditText newNick = (EditText) promptsView
                .findViewById(R.id.new_nick);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this,
                R.style.menuDialogStyle);
        alertDialogBuilder.setView(promptsView);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        userNick = newNick.getText().toString();
                        if (userNick != null && userNick.length() >= 2) {
                            accountApi.setNickName(selfId, userNick);
                        } else
                            Toast.makeText(uiContext, getResources().getString(R.string.input_format_error),
                                    Toast.LENGTH_SHORT).show();

                    }
                })
                .setNegativeButton("CANCEL",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        return;
    }

    private interface SelectCallBack {
        public void onItem(String name, String addr);
    }
    // 菜单：蓝牙手咪
    private void chooseAndStartBlt(final List<String[]> devices, final SelectCallBack cb) {
        if (devices != null && devices.size() > 0) {
            final CharSequence[] devs = new CharSequence[devices.size()];
            for (int i = 0; i < devices.size(); i++) {
                String[] dev = devices.get(i);
                String dname = dev[1];
                devs[i] = dname.concat("            ").subSequence(0, 12) + " <" + dev[0].substring(9) + ">";
            }
            new AlertDialog.Builder(BroadcastActivity.this, R.style.menuDialogStyle)
                    .setTitle(getResources().getString(R.string.select_bluetooth))
                    .setItems(devs, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String bltAddr = devices.get(which)[0];
                            String bltName = devices.get(which)[1];
                            cb.onItem(bltName, bltAddr);
                        }
                    }).show();
        } else {
            Toast.makeText(BroadcastActivity.this, getResources().getString(R.string.pair_hint),
                    Toast.LENGTH_SHORT).show();
        }
    }
    // 菜单：绑定手机
    private void showBindingPhone() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_bind_phone, null);
        final EditText etPno = (EditText) promptsView
                .findViewById(R.id.phone_no);
        etPno.requestFocus();

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this,
                R.style.menuDialogStyle);
        alertDialogBuilder.setView(promptsView);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String phoneNo = etPno.getText().toString();
                        if (phoneNo.length() == 11) {
                            accountApi.requestBindingPhone(selfId, userAccount,
                                    phoneNo);
                        } else {
                            Toast.makeText(uiContext, getResources().getString(R.string.input_format_error),
                                    Toast.LENGTH_SHORT).show();
                            showBindingPhone();
                        }
                    }
                })
                .setNegativeButton("CANCEL",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        return;
    }
    private void showBindingPhone2(String phNo) {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_bind_phone, null);
        EditText etPno = (EditText) promptsView.findViewById(R.id.phone_no);
        final String phoneNo = phNo;
        etPno.setText(phoneNo);
        LinearLayout lliac = (LinearLayout) promptsView
                .findViewById(R.id.input_auth_code);
        lliac.setVisibility(View.VISIBLE);
        final EditText etAc = (EditText) promptsView
                .findViewById(R.id.auth_code);
        etAc.requestFocus();

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this,
                R.style.menuDialogStyle);
        alertDialogBuilder.setView(promptsView);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String authCode = etAc.getText().toString();
                        if (authCode.length() == 6) {
                            accountApi.commandBindingPhone(selfId, phoneNo,
                                    authCode);
                        } else {
                            Toast.makeText(uiContext, getResources().getString(R.string.input_format_error),
                                    Toast.LENGTH_SHORT).show();
                            showBindingPhone2(phoneNo);
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    // 菜单：音频属性设置
    private void showAdjustAudioLevel() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_adjust_audiolevel, null);

        final SeekBar spkBar = (SeekBar) promptsView.findViewById(R.id.speaker_amplitude);
        int spkA = 0;
        if (talkFragment != null) {
            spkA = spkBar.getMax() * talkFragment.getAudioAmplitude() / Constant.MAXAUDIORATE;
        }
        spkBar.setProgress(spkA);
        final TextView spkTv = (TextView) promptsView.findViewById(R.id.speaker_level_text);
        final String spkTx = getResources().getString(R.string.spk_volume_hint);
        spkTv.setText(spkTx + getResources().getString(R.string.that_is) + (spkA == 0 ? getResources().getString(R.string.as_system_vol) : Utils.spkAmpR(spkA/10)));
        spkBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int prog, boolean fromUser) {
                prog += 5;
                spkTv.setText(spkTx + getResources().getString(R.string.that_is) + (prog/10 == 0 ? getResources().getString(R.string.as_system_vol) : Utils.spkAmpR(prog/10)));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        RadioGroup rgNS = (RadioGroup) promptsView.findViewById(R.id.notify_sound_hint);
        int nsType = Constant.NOTIFY_SOUND_NORMAL;
        if (deviceApi != null) {
            nsType = Constant.NOTIFY_SOUND_NORMAL;
            if (nsType == Constant.NOTIFY_SOUND_NORMAL)
                rgNS.check(R.id.notify_normally);
            if (nsType == Constant.NOTIFY_SOUND_SIMPLE)
                rgNS.check(R.id.notify_simply);
            if (nsType == Constant.NOTIFY_SOUND_NONE)
                rgNS.check(R.id.notify_none);
        }

        rgNS.setOnCheckedChangeListener(null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this,
                R.style.menuDialogStyle);
        alertDialogBuilder.setView(promptsView);
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int spkX = spkBar.getProgress() + 5;
                        if (talkFragment != null) {
                            talkFragment.setAudioAmplitude(spkX/10);
                        }
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    // 菜单：APP信息
    private void showAppVersion() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_show_version, null);

        TextView tvVer = (TextView) promptsView.findViewById(R.id.app_version);
        String appVer = "1.0";
        PackageManager pm = getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(getPackageName(), 0);
            appVer = pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }
        tvVer.setText("miniTL version: v" + appVer);

        TextView tvAccount = (TextView) promptsView
                .findViewById(R.id.tourling_account);
        Contact me = accountApi.whoAmI();
        tvAccount.setText("Tourling No: " + me.name);

        TextView tvStartTi = (TextView) promptsView
                .findViewById(R.id.app_start_date);
        TextView tvNetwork = (TextView) promptsView
                .findViewById(R.id.app_network_used);
        TextView tvSpeech = (TextView) promptsView
                .findViewById(R.id.app_speech_used);
        int broken = (int) API.runtimeInfo.getOfflineTimes();
        long offSec = API.runtimeInfo.getOfflineSeconds();
        long nSent = API.runtimeInfo.getNetworkSent();
        long nRcvd = API.runtimeInfo.getNetworkReceived();
        long msTalk = API.runtimeInfo.getTalkMSeconds();
        long msListen = API.runtimeInfo.getListenMSeconds();
        tvStartTi.setText("started: " + API.runtimeInfo.getAppStartTime()
                + " offline:" + offSec + "s (" + broken + "t)");
        tvNetwork.setText("network: " + bkm(nSent + nRcvd) + " (" + bkm(nSent)
                + "+" + bkm(nRcvd) + ") Bytes.");
        tvSpeech.setText("talking: " + getTime(msTalk) + " --- listen: "
                + getTime(msListen));

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this,
                R.style.menuDialogStyle);
        alertDialogBuilder.setView(promptsView);
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
					/*	HttpGetTLVersion getVer = new HttpGetTLVersion();
						getVer.setOnTLVersionGetListener(new OnTLVerGetListener() {
							@Override
							public void onTLVersionGet(int ver) {
								int ver1 = getVersionCode(TLActivity.this);
								Log.i(TAG, "local ver:" + ver1
										+ " app on server:" + ver);
								if (ver > ver1) {
									uiHandler.obtainMessage(
											MsgCode.MC_UPDATECLIENT)
											.sendToTarget();
								}
							}
						});
						getVer.execute(GetVerUri);
						dialog.cancel();	*/
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        TextView tvGW = (TextView) promptsView.findViewById(R.id.guanwang);
        tvGW.getPaint().setFlags(Paint.UNDERLINE_TEXT_FLAG);
        tvGW.getPaint().setAntiAlias(true);
        tvGW.setClickable(true);
        tvGW.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        return;
    }
    private String getTime(long mss) {
        long hours = (mss / (1000 * 60 * 60));
        long minutes = (mss - hours * (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (mss - hours * (1000 * 60 * 60) - minutes * (1000 * 60)) / 1000;
        if (hours == 0 && minutes == 0)
            return seconds + "s";
        else if (hours == 0)
            return minutes + "m" + seconds + "s";
        else
            return hours + "h" + minutes + "m" + seconds + "s";
    }
    private String bkm(long b) {
        if (b < 5000) {
            return Long.toString(b);
        } else if (b < 5000000) {
            return Long.toString(b / 1000) + "k";
        } else {
            long M = b / 1000000;
            long K100 = b / 100000;
            return Long.toString(M) + "." + Long.toString(K100) + "M";
        }
    }
    // 菜单：发验证短信
    private void showSendSmsDialog() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_send_sms, null);
        final EditText phoneNo = (EditText) promptsView.findViewById(R.id.phone_send_to);
        final EditText checkCode = (EditText) promptsView.findViewById(R.id.check_code);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, R.style.menuDialogStyle);
        alertDialogBuilder.setView(promptsView);
        alertDialogBuilder
                .setCancelable(true)
                .setPositiveButton("SEND",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                accountApi.requestSendSms(phoneNo.getText().toString(), checkCode.getText().toString());
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }


    // 频道板块
    private void start_channel_fragment(int uid, int uState) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        if (channelFragment == null) {
            Log.i(TAG, "create channels fragment");
            // 修改频道
            setContentView(R.layout.navi_talk);

            newChannelFragmentHori(uid, uState);
            transaction.add(R.id.id_content_channel, channelFragment);
            transaction.commitAllowingStateLoss();
        } else {
            Log.i(TAG, "channels fragment existed.");
        }
    }
    private void newChannelFragmentHori(int uid, int uState) {
        Bundle cFArgs = new Bundle();
        cFArgs.putInt("id.self", uid);
        cFArgs.putInt("state.self", uState);
        cFArgs.putBoolean("visitor.self", isVisitor);
        channelFragment = new ChannelFragment();
        channelFragment.setArguments(cFArgs);
    }
    //  对讲板块
    private void start_talk_fragment(int uid, int uState) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        if (talkFragment == null) {
            Log.i(TAG, "create talk fragment");
            newTalkFragment(uid, uState);
            transaction.replace(R.id.id_fragment_talk, talkFragment);
            transaction.commitAllowingStateLoss();
        } else {
            Log.i(TAG, "talk fragment existed. is:" + talkFragment.toString());
        }
    }
    private void newTalkFragment(int uid, int uState) {
        talkFragment = new TalkFragment();
        Bundle tFArgs = new Bundle();
        tFArgs.putInt("id.self", uid);
        tFArgs.putString("nick.self", userNick);
        tFArgs.putInt("state.self", uState);
        talkFragment.setArguments(tFArgs);
        Log.d(TAG, "make talkFragment return:" + talkFragment.toString());
    }

    // ChannelFragment所需方法
    public void stopDialogSession(int self, int dialog) {
        if (talkFragment != null)
            talkFragment.stopDialog(self, dialog);
    }
    public void showTourlinkChannelScroller(int maxIdx) {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_select_tlchannel, null);
        final ObservableScrollView osvSele = (ObservableScrollView) promptsView.findViewById(R.id.tlchannels_select);
        final TextView tvTLChPresCount = (TextView) promptsView.findViewById(R.id.tlchannel_prescount);
        tvTLChPresCount.setText("  ( 0 )");
        osvSele.setScrollViewListener(new ScrollViewListener() {
            @Override
            public void onScrollChanged(FrameLayout scrollView, int x, int y,
                                        int oldx, int oldy) {
            }

            @Override
            public void onScrollStopped(FrameLayout scrollView, int x, int y) {
                ObservableScrollView tView = (ObservableScrollView) scrollView;
                int presC = getTLChPresencesCount(tView.getTargetIdx());
                tvTLChPresCount.setText("  ( " + presC + " )");
            }
        });
        osvSele.start();

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this,
                R.style.menuDialogStyle);
        alertDialogBuilder.setView(promptsView);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.entry_channel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                int target = osvSele.stop();
                                channelFragment.onTourlinkChannelScrollerClosed(target);
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                osvSele.stop();
                                dialog.cancel();
                                channelFragment.onTourlinkChannelScrollerClosed(-1);
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

        return;
    }
    private int getTLChPresencesCount(int idx) {
        if (channelFragment != null)
            return channelFragment.getTLChPresencesCount(idx);
        return 0;
    }
    private Channel modifyChannel = null;
    public void showModifyChannelSpinner(List<Channel> allChs) {
        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View promptsView = li.inflate(R.layout.dialog_modify_channel, null);
        final EditText etNewName = (EditText) promptsView
                .findViewById(R.id.make_channel_name);

        Spinner mSpinner = (Spinner) promptsView
                .findViewById(R.id.channels_spinner);
        DfltChAdapter adapter = new DfltChAdapter(allChs);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                Channel che1 = (Channel) parent.getAdapter().getItem(position);
                modifyChannel = new ChannelExt(che1.cid.getType(), che1.cid
                        .getId(), che1.name);
                modifyChannel.isHome = che1.isHome;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        modifyChannel = null;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                uiContext, R.style.menuDialogStyle);
        alertDialogBuilder.setView(promptsView);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newName = etNewName.getText().toString();
                        if (modifyChannel != null
                                && !TextUtils.isEmpty(newName)) {
                            channelFragment.modifyChannelAttrs(modifyChannel,
                                    newName);
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    public class DfltChAdapter extends BaseAdapter {
        private List<Channel> allChannels = null;

        public DfltChAdapter(List<Channel> chs) {
            allChannels = chs;
        }

        @Override
        public int getCount() {
            if (allChannels != null)
                return allChannels.size();
            return 0;
        }

        @Override
        public Object getItem(int position) {
            if (allChannels != null)
                return allChannels.get(position);
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater _LayoutInflater = LayoutInflater.from(uiContext);
            convertView = _LayoutInflater.inflate(R.layout.default_channel_item, null);
            Channel theCh = allChannels.get(position);
            if (convertView != null && theCh != null) {
                TextView tv1 = (TextView) convertView
                        .findViewById(R.id.channel_list_name);
                tv1.setText(theCh.name);
                if (theCh.owner == null) {
                    if (theCh.isHome) {
                        Drawable dUndist = uiContext.getResources().getDrawable(R.drawable.channel_home);
                        tv1.setCompoundDrawablesWithIntrinsicBounds(dUndist, null, null, null);
                        tv1.setTextColor(0xFFEEEEEE);
                    }
                } else {
                    TextView tv2 = (TextView) convertView.findViewById(R.id.channel_list_owner);
                    tv2.setText("(by: " + theCh.owner.s + ")");
                    Drawable dPass = uiContext.getResources().getDrawable(theCh.needPassword ? R.drawable.channel_protected
                            : R.drawable.channel_unprotected);
                    tv1.setCompoundDrawablesWithIntrinsicBounds(dPass, null, null, null);
                }
            }
            return convertView;
        }
    }
    private Channel oldDefaultCh = null;
    private Channel newDefaultCh = null;
    // 设置默认频道
    public void showSetDefaultSpinner() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.default_channel_spinner, null);
        Spinner mSpinner = (Spinner) promptsView
                .findViewById(R.id.default_channel);

        List<Channel> allCHs = channelFragment.getDefaultChannelCandidates(selfId);
        DfltChAdapter adapter = new DfltChAdapter(allCHs);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                Channel che1 = (Channel) parent.getAdapter().getItem(position);
//                newDefaultCh = new Channel(che1.cid.getType(), che1.cid.getId(), che1.name);
                newDefaultCh = new Channel(ctype, cid, cname);
                Log.d(TAG, "default id selected:" + cname);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        oldDefaultCh = null;
        newDefaultCh = null;
        for (int i = 0; i < allCHs.size(); i++) {
            if (allCHs.get(i).isHome) {
                mSpinner.setSelection(i, true);
                oldDefaultCh = allCHs.get(i);
                break;
            }
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this,
                R.style.menuDialogStyle);
        alertDialogBuilder.setView(promptsView);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        if (newDefaultCh != null) {
//                            if (oldDefaultCh == null || !Channel.sameChannel(newDefaultCh, oldDefaultCh))
//                                channelFragment.setDefaultChannel(selfId, newDefaultCh.cid.getType(), newDefaultCh.cid.getId());
//                        }
                        channelFragment.setDefaultChannel(selfId, ctype, cid);
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
    private Channel deleteChannel = null;
    public void showDeleteChannelSpinner(List<Channel> allChs) {
        LayoutInflater li = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View promptsView = li.inflate(R.layout.default_channel_spinner, null);

        Spinner mSpinner = (Spinner) promptsView
                .findViewById(R.id.default_channel);
        DfltChAdapter adapter = new DfltChAdapter(allChs);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                Channel che1 = (Channel) parent.getAdapter().getItem(position);
                deleteChannel = new ChannelExt(che1.cid.getType(), che1.cid
                        .getId(), che1.name);
                deleteChannel.owner = new IntStr(che1.owner.i, che1.owner.s);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        deleteChannel = null;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                uiContext, R.style.menuDialogStyle);
        alertDialogBuilder.setView(promptsView);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (deleteChannel != null) {
                            if (selfId == deleteChannel.owner.i) {
                                channelFragment.deleteChannel(selfId,
                                        deleteChannel.cid.getType(),
                                        deleteChannel.cid.getId());
                            } else {
                                channelFragment.unfocusChannel(selfId,
                                        deleteChannel.cid.getType(),
                                        deleteChannel.cid.getId());
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }
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
            processCircle = ProgressCircleLite.show(BroadcastActivity.this, "", "",
                    false, true);
        }
    }
    public void dismessProgressCircle() {
        if (processCircle != null) {
            processCircle.dismiss();
            processCircle = null;
        }
    }
    private static final String TL_FLAG_ACCOUNT_PROPERITIES = "account.properties.minitl";
    private static final String TL_KEY_AD_CHANNELS = "USER.ADCHANNELS";
    public void setMyPreferedChannels(List<Integer> cids) {
        JSONArray jobj = new JSONArray();
        for (Integer cid : cids)
            jobj.put(cid);
        SharedPreferences sharedPreferences = getSharedPreferences(TL_FLAG_ACCOUNT_PROPERITIES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TL_KEY_AD_CHANNELS, jobj.toString());
        editor.commit();
    }
    public List<Integer> getMyPreferedChannels() {
        SharedPreferences sharedPreferences = getSharedPreferences(TL_FLAG_ACCOUNT_PROPERITIES, Context.MODE_PRIVATE);
        String rStr = sharedPreferences.getString(TL_KEY_AD_CHANNELS, null);
        if (rStr == null)
            return null;

        try {
            JSONArray jobj = new JSONArray(rStr);
            List<Integer> cids = new ArrayList<Integer>();
            for (int i=0; i<jobj.length();i++) {
                cids.add((Integer)jobj.getInt(i));
            }
            return cids;
        } catch (JSONException e) {
            return null;
        }
    }
    private Channel focusChannel = null;
    public void showFocusChannelSpinner(List<Channel> allCHs) {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.focus_channel_spinner, null);

        final EditText focusEC = (EditText) promptsView
                .findViewById(R.id.focus_entry_code);
        Spinner mSpinner = (Spinner) promptsView
                .findViewById(R.id.channels_spinner);
        DfltChAdapter adapter = new DfltChAdapter(allCHs);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                Channel che1 = (Channel) parent.getAdapter().getItem(position);
                focusChannel = new ChannelExt(che1.cid.getType(), che1.cid
                        .getId(), che1.name);
                focusChannel.needPassword = che1.needPassword;
                focusChannel.owner = new IntStr(che1.owner.i, che1.owner.s);
                if (che1.needPassword) {
                    focusEC.setText(null);
                    focusEC.setHint(R.string.focus_ec_hint);
                    focusEC.setVisibility(View.VISIBLE);
                    focusEC.requestFocus();
                } else {
                    focusEC.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        focusChannel = null;

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this,
                R.style.menuDialogStyle);
        alertDialogBuilder.setView(promptsView);
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (focusChannel != null) {
                            String focusEntryCode = focusEC.getText()
                                    .toString();
                            channelFragment.focusChannel(selfId, focusChannel,
                                    focusEntryCode);
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                                int which) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    // TalkFragment所需方法
    public boolean isUserBoundPhone() {
        return userBoundPhone;
    }



    /** OnAccoucntListener 回调
     *
     */
    @Override
    public void onLogin(int i, int i1, UserProfile userProfile) {

    }

    @Override
    public void onCreateUser(int i, int i1, String s) {

    }

    @Override
    public void onLogout() {

    }

    @Override
    public void onSetNickName(int i) {

    }

    @Override
    public void onChangePassWord(int i, boolean b) {

    }

    @Override
    public void onAskUnbind(int i) {

    }

    @Override
    public void onAuthRequestReply(int i, int i1, String s) {

    }

    @Override
    public void onAuthBindingReply(int i, int i1, String s) {

    }

    @Override
    public void onAuthRequestPassReply(int i, int i1, String s) {

    }

    @Override
    public void onAuthResetPassReply(int i, int i1) {

    }

    @Override
    public void onShakeScreenAck(int i, int i1, int i2) {

    }

    @Override
    public void onFriendsSectionGet(int i, int i1, int i2, int i3, List<Contact> list) {

    }

    @Override
    public void onFriendStatusUpdate(int i, int i1, int i2) {

    }

    @Override
    public void onShakeScreenReceived(int i, String s, String s1) {

    }

    @Override
    public void onSetStatusReturn(int i, boolean b) {

    }

    @Override
    public void onHearbeatLost(int i, int i1) {

    }

    @Override
    public void onKickedOut(int i, int i1) {

    }

    @Override
    public void onSelfStateChange(int i, int i1) {

    }

    @Override
    public void onSelfLocationAvailable(int i, Double aDouble, Double aDouble1, Double aDouble2) {

    }

    @Override
    public void onSelfLocationReported(int i) {

    }

    @Override
    public void onUserLocationNotify(int i, String s, Double aDouble, Double aDouble1) {

    }

    @Override
    public void onLogger(int i, String s) {

    }

    @Override
    public void onSmsRequestReply(int i) {

    }




    /** OnSessionListner 回调
     *
     */
    @Override   // 会话连接成功
    public void onSessionEstablished(int selfUserId, int type, int sessionId) {
        Log.i(TAG, "session type: " + type + " ,id: " + sessionId );
        if (selfUserId <= 0 || sessionId <= 0) {
            Toast.makeText(uiContext, "进入失败", Toast.LENGTH_SHORT).show();
            return;
        }
        currSession = new CompactID(type, sessionId);
        if (sessPresences == null) {
            sessPresences = new ArrayList<IntStr>();
        } else {
            sessPresences.clear();
        }
        sessPresences.add(new IntStr(selfId, API.uid2nick(selfId)));
        uiHandler.obtainMessage(MsgCode.MC_ONSESSIONESTABLISHED, type, sessionId).sendToTarget();

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
