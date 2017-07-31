package com.hhuc.sillyboys.tuling;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
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
import com.algebra.sdk.entity.AudioDev;
import com.algebra.sdk.entity.Channel;
import com.algebra.sdk.entity.CompactID;
import com.algebra.sdk.entity.Constant;
import com.algebra.sdk.entity.Contact;
import com.algebra.sdk.entity.IntStr;
import com.algebra.sdk.entity.OEMToneGenerator;
import com.algebra.sdk.entity.OEMToneProgressListener;
import com.algebra.sdk.entity.UserProfile;
import com.algebra.sdk.entity.Utils;
import com.hhuc.sillyboys.tuling.entity.ChannelExt;
import com.hhuc.sillyboys.tuling.entity.MsgCode;
import com.hhuc.sillyboys.tuling.entity.ObservableScrollView;
import com.hhuc.sillyboys.tuling.entity.ScrollViewListener;
import com.hhuc.sillyboys.tuling.tl_demo.ChannelFragment;
import com.hhuc.sillyboys.tuling.tl_demo.NotificationActivity;
import com.hhuc.sillyboys.tuling.tl_demo.RegisterUser;
import com.hhuc.sillyboys.tuling.tl_demo.TalkFragment;
import com.hhuc.sillyboys.tuling.util.DownloadMan;
import com.hhuc.sillyboys.tuling.util.ProgressCircleLite;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class LoginActivity extends AppCompatActivity implements OnAccountListener{
    private static final String TAG = "login";
    private static Handler uiHandler = null;
    public static Handler getUiHandler() {
        return uiHandler;
    }
    private Context uiContext = null;
    private RegisterUser registerUser = null;
    private boolean horizonScreen = false;
    private static int startStep = StartStage.INITIALIZING;
    private ChannelFragment channelFragment = null;
    private TalkFragment talkFragment = null;

    private boolean newBind = true;
    private AccountApi accountApi = null;
    private DeviceApi deviceApi = null;
    private int selfId = 0;

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
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreate ..");
        super.onCreate(savedInstanceState);

        uiContext = this;
        uiHandler = new UiHandler(this);

        horizonScreen = isHorizon();
        if (horizonScreen)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        else
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        registerUser = new RegisterUser(LoginActivity.this, uiHandler);
        startStep = StartStage.INITIALIZING;

        newBind = API.init(this);
        Log.i(TAG, "onCreate init services, newBind = "+newBind);

        setContentView(R.layout.welcome);
    }




    @Override
    public void onResume() {
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
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (needUnbind)
            API.leave();
        needUnbind = false;

        if (unReadSms >= 1) {
            unregisterReceiver(missingDialogReceiver);
            unReadSms = 0;
        }

        uiHandler = null;

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




    private static final int KEYCODE_PTT = 260; //MF198:260 //HCT:0x16E; // jt400: 220;
    private long lastBackDown = 0;
    private boolean backDoublePressed = false;
    // 检测手机返回键按下
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown " + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (backDoublePressed) {
                dismissProcessing();
                userExit();
            } else {
                long timeNow = System.currentTimeMillis();
                if (timeNow - lastBackDown > 1500) {
                    Toast.makeText(this, getResources().getString(R.string.back_exit_ui_hint), Toast.LENGTH_SHORT)
                            .show();
                    backDoublePressed = false;
                    lastBackDown = timeNow;
                } else {
					/*
					 * if (selfId > 0) { backDoublePressed = true;
					 * showProcessing("logout"); userLogout(); } else {
					 * userExit(); }
					 */
                    userExit();
                }
            }
            return false;
        } else if (keyCode == KEYCODE_PTT) {
            Log.i(TAG, "ptt down;");
            if (talkFragment != null)
                talkFragment.processPttAction(MotionEvent.ACTION_DOWN);
            return false;
        } else {
            Log.i(TAG, "key " + keyCode + " down");
        }
        // other key pressed then clear last timer;
        backDoublePressed = false;
        lastBackDown = 0;

        return super.onKeyDown(keyCode, event);
    }
    // 检测手机返回键松开
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyUp " + keyCode);
        if (keyCode == KEYCODE_PTT) {
            Log.i(TAG, "ptt up;");
            if (talkFragment != null)
                talkFragment.processPttAction(MotionEvent.ACTION_UP);
            return false;
        } else {
            Log.i(TAG, "key " + keyCode + " up");
        }

        return super.onKeyUp(keyCode, event);
    }




    // 创建菜单（那个工具一样的图标）
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
        menu.getItem(0).setChecked(isBluetoothStarted());
        if (talkFragment != null)
            menu.getItem(1).setChecked(talkFragment.getPttTrigglable());
        menu.getItem(4).setVisible(!isVisitor);
        menu.getItem(5).setVisible(!isVisitor && !userBoundPhone);
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
                                Toast.makeText(LoginActivity.this, getResources().getString(R.string.connect_bluetooth_failed1) + name + getResources().getString(R.string.connect_bluetooth_failed2), Toast.LENGTH_SHORT).show();
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
            new AlertDialog.Builder(LoginActivity.this, R.style.menuDialogStyle)
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
            Toast.makeText(LoginActivity.this, getResources().getString(R.string.pair_hint),
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
        tvAccount.setText("Tourling No: " + userAccount);

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




    private String unReadSenderList = "";
    private BroadcastReceiver missingDialogReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            String sender = intent.getExtras().getString(ACTIONS.SMS_SENDER_NAME, "none");
            String content = intent.getExtras().getString(ACTIONS.SMS_CONTENT, "none");
            android.util.Log.d(TAG, "sms sender name: " + sender + "sms content: " + content);
            try {
                JSONObject obj = new JSONObject(content);
                String cname = getChannelNameById(Constant.SESSION_TYPE_CHANNEL, obj.getInt("id"));
                Toast.makeText(uiContext, sender + " @ " + cname, Toast.LENGTH_SHORT).show();
            } catch (JSONException e) {};

            unReadSms = 0;
            unReadSenderList = "";
            unregisterReceiver(this);
        }
    };
    public interface ACTIONS {
        public static final String UNREAD_SHORTMESSAGE = "algebra.sms.unread";
        public static final String SMS_SENDER_NAME = "algebra.sms.sender.name";
        public static final String SMS_CONTENT = "algebra.sms.content";

        public static final String MISSED_TL_CALL = "algebra.call.missed";
        public static final String CALLER_ID = "algebra.caller.id";
        public static final String CALLER_NAME = "algebra.caller.name";
    }
    public String getChannelNameById(int ctype, int sid) {
        if (channelFragment != null)
            return channelFragment.getChannelNameById(new CompactID(ctype, sid));
        else
            return "???";
    }



    private OEMToneGen oemToneGen = new OEMToneGen();
    private void setupToneGen() {
        deviceApi.setOemToneGenerator(oemToneGen);
        OEMToneProgressListener listener = deviceApi.getToneProgressListener();
        oemToneGen.setToneProgressListener(listener);
    }
    private class OEMToneGen implements OEMToneGenerator {
        private OEMToneProgressListener toneProgressListener = null;
        private ToneGenerator mToneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 15);

        void setToneProgressListener(OEMToneProgressListener l) {
            toneProgressListener = l;
        }

        @Override
        public void alertTone(final int type) {
            mToneGen.startTone(ToneGenerator.TONE_DTMF_5);
            uiHandler.postDelayed(new Runnable(){
                @Override
                public void run() {
                    mToneGen.stopTone();
                    if (toneProgressListener != null)
                        toneProgressListener.onToneStopped(type);
                }
            }, 100);
        }
    }


    // 处理最小化后再次打开界面
    private Runnable delayInitApi = new Runnable() {
        @Override
        public void run() {
            accountApi = API.getAccountApi();
            deviceApi = API.getDeviceApi();
            if (accountApi != null && deviceApi != null) {
                accountApi.setOnAccountListener(LoginActivity.this);
                setupToneGen();
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
                    android.util.Log.d(TAG,
                            "start SDK and waiting another 300ms.");
                    uiHandler.postDelayed(delayInitApi, 300);
                }
            }
        }
    };

    // 登录进度条
    private void loginProgressHint(String txt) {
        LoginFailureShow lfs = new LoginFailureShow(txt);
        runOnUiThread(lfs);
    }

    // 登录消息提示框
    private class LoginFailureShow implements Runnable {
        private String loginFailureTxt = null;

        public LoginFailureShow(String txt) {
            loginFailureTxt = txt;
        }

        @Override
        public void run() {
            Toast.makeText(LoginActivity.this, loginFailureTxt, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    // 处理登录结果
    @Override
    public void onLogin(int uid, int result, UserProfile uProfile) {
        uiHandler.obtainMessage(MsgCode.MC_LOGINFINISHED, 2, 0).sendToTarget();
        System.out.println("uid " + uid + " onlogin result: " + result);
        // 成功
        if (result == Constant.ACCOUNT_RESULT_OK || result == Constant.ACCOUNT_RESULT_ALREADY_LOGIN) {
            if (uProfile != null) {
                userBoundPhone = !uProfile.userPhone.equals("none");
                userAccount = new String(uProfile.userName);
                userNick = new String(uProfile.userNick);
                isVisitor = (uProfile.userType == Constant.USER_TYPE_VISITOR);
            }
            uiHandler.obtainMessage(MsgCode.MC_LOGINOK, uid,
                    Constant.CONTACT_STATE_ONLINE).sendToTarget();
            return;
        }

        if (result == Constant.ACCOUNT_RESULT_PREOK) {
            loginProgressHint("Login...");
            return;
        }

        if (result == Constant.ACCOUNT_RESULT_TIMEOUT) {
            loginProgressHint(getString(R.string.err_timeout));
            registerUser.removeUserInfo();
            uiHandler.sendEmptyMessage(MsgCode.ASKFOREXIT);
            return;
        }

        if (result == Constant.ACCOUNT_RESULT_ERR_USER_NOTEXIST) {
            registerUser.removeUserInfo();
            loginProgressHint(getString(R.string.err_account));
        }
        if (result == Constant.ACCOUNT_RESULT_ERR_USER_PWD) {
            registerUser.removeUserInfo();
            loginProgressHint(getString(R.string.err_userpass));
        }
        if (result == Constant.ACCOUNT_RESULT_ERR_SERVER_UNAVAILABLE) {
            loginProgressHint(getString(R.string.err_serverunava));
        }
        if (result == Constant.ACCOUNT_RESULT_ERR_NETWORK) {
            loginProgressHint(getString(R.string.err_network));
        }
        if (result == Constant.ACCOUNT_RESULT_OTHER_LOGIN) {
            registerUser.removeUserInfo();
            loginProgressHint(getString(R.string.err_nouser));
        }
        uiHandler.sendEmptyMessage(MsgCode.ASKFORSTARTSDK);
        return;
    }

    // 创建用户
    @Override
    public void onCreateUser(int selfId, int result, String account) {
        if (result == Constant.ACCOUNT_RESULT_OK) { // successful, auto login..
            userAccount = new String(account);
            return;
        }

        // 失败
        uiHandler.obtainMessage(MsgCode.MC_LOGINFINISHED, 1, 0).sendToTarget();
        uiHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, getString(R.string.register_failed), Toast.LENGTH_LONG)
                        .show();
            }
        });

        uiHandler.sendEmptyMessage(MsgCode.ASKFORSTARTSDK);
    }

    @Override
    public void onKickedOut(int arg0, int arg1) {
        Toast.makeText(this, "OnKickOut !", Toast.LENGTH_SHORT).show();
        stop_both_fragments();
        selfId = 0;
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                LoginActivity.this.finish();
            }
        }, 3000);
    }
    @Override
    public void onSelfStateChange(int userId, int state) {
        System.out.println("*** onSelfStateChange: " + state);

        selfState = state;
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (talkFragment != null)
                    talkFragment.onSelfStatusChange(selfState);
                if (channelFragment != null)
                    channelFragment.onSelfStatusChange(selfState);
            }
        }, 200);
    }
    @Override
    public void onChangePassWord(int uid, boolean successful) {
        if (successful)
            Toast.makeText(this, getString(R.string.change_pass_success), Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, getString(R.string.operate_failed), Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onLogout() {
        dismissProcessing();
        userExit();
    }
    @Override
    public void onSetNickName(int id) {
        if (id > 0) {
            Toast.makeText(this, getString(R.string.change_nick_succ) + userNick, Toast.LENGTH_SHORT)
                    .show();
            if (talkFragment != null && userNick != null)
                talkFragment.onSetNickName(userNick);
            if (channelFragment != null && userNick != null)
                channelFragment.onSetNickName(id, userNick);
            if (userNick != null) {
                registerUser.setNickName(userNick);
            }
        } else
            Toast.makeText(this, getString(R.string.change_nick_fail), Toast.LENGTH_SHORT).show();
    }
    @Override
    public void onShakeScreenReceived(int userId, String fromName, String txt) {
        uiHandler.post(new NotifySMS(new String(fromName), new String(txt)));
    }
    @Override
    public void onSetStatusReturn(int self, boolean undistube) {
        if (talkFragment != null)
            talkFragment.setSelfUndistubeInd(undistube);
    }
    @Override
    public void onFriendStatusUpdate(int selfId, int userId, int status) {
        Log.d(TAG, "onFriend:" + userId + " StatusUpdate to:" + status);
        if (channelFragment != null)
            channelFragment.onPeerStatusChange(userId, status);
    }
    @Override
    public void onShakeScreenAck(int result, int targetId, int tag) {
        if (channelFragment != null)
            channelFragment.onShakeScreenAck(result, targetId);
    }
    @Override
    public void onAuthRequestReply(int userId, int state, String phoneNo) {
        if (userId > 0 && state == Constant.E_AUTH_REQ_OK) {
            showBindingPhone2(phoneNo);
        } else {
            Toast.makeText(this, getString(R.string.bind_phone_failed) + authErr2Txt(state),
                    Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onAuthBindingReply(int userId, int state, String phoneNo) {
        if (userId > 0 && state == Constant.E_AUTH_REQ_OK) {
            Toast.makeText(this, getString(R.string.bind_phone_hint1) + phoneNo + getString(R.string.bind_phone_hint2), Toast.LENGTH_SHORT)
                    .show();
            userBoundPhone = true;
            talkFragment.onBoundPhone();
        } else {
            uiToast(getString(R.string.failed_reason) + authErr2Txt(state));
        }
    }
    private void uiToast(String txt) {
        runOnUiThread(new TextToast(txt));
    }
    private class TextToast implements Runnable {
        private String txt = null;

        public TextToast(String in) {
            txt = in;
        }

        @Override
        public void run() {
            Toast.makeText(uiContext, txt, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAskUnbind(int i) {

    }

    @Override
    public void onAuthRequestPassReply(int userId, int state, String uAccount) {
        if (userId > 0 && state == Constant.E_AUTH_REQ_OK) {
            uiToast(getString(R.string.sending_auth_hint1) + userPhone + getString(R.string.sending_auth_hint2));
            final int rpUid = userId;
            final String rpAccount = uAccount;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startStep = StartStage.RESET_PASS;
                    registerUser.startResetPass(R.layout.dialog_resetpw, rpUid,
                            rpAccount);
                }
            });
        } else {
            uiToast(getString(R.string.failed_reason) + authErr2Txt(state));
            uiHandler.obtainMessage(MsgCode.ASKFORSTARTSDK).sendToTarget();
        }
    }
    @Override
    public void onAuthResetPassReply(int userId, int state) {
        if (userId > 0 && state == Constant.E_AUTH_REQ_OK) {
            uiToast(getString(R.string.reset_pass_success));
        } else {
            uiToast(getString(R.string.failed_reason) + authErr2Txt(state));
        }
    }
    @Override
    public void onFriendsSectionGet(int i, int i1, int i2, int i3, List<Contact> list) {

    }

    @Override
    public void onHearbeatLost(int i, int i1) {

    }
    @Override
    public void onSelfLocationAvailable(int i, Double aDouble, Double aDouble1, Double aDouble2) {

    }
    @Override
    public void onSelfLocationReported(int i) {

    }
    @Override
    public void onUserLocationNotify(int userId, String userNick, Double dist,
                                     Double orit) {
        Log.i(TAG, "user " + userNick + " @ " + dist + "m, " + orit);
        Toast.makeText(uiContext, userNick + " @" + gpsShow(dist, orit),
                Toast.LENGTH_SHORT).show();
    }
    private String gpsShow(Double dist, Double du) {
        int mdi = (int) Math.round(dist);
        int idu = (int) Math.round(du);
        Log.i(TAG, "distence " + mdi + " degree" + idu);
        String outStr = "";
        if (mdi < 4999) {
            outStr = outStr + Integer.toString(mdi) + "m";
        } else {
            int m100di = (int) (mdi / 100);
            Float mkds = (float) (m100di / 10.0);
            outStr = outStr + Float.toString(mkds) + "km";
        }
        outStr += " ";

        if (idu == -180) {
            outStr += "O";
        } else if (idu % 90 == 0) {
            switch (idu) {
                case 0:
                    outStr += "N";
                case 90:
                    outStr += "E";
                case -90:
                    outStr += "W";
                case 180:
                    outStr += "S";
            }
        } else if (idu >= 0 && idu < 90) {
            outStr += "N" + Integer.toString(idu) + "E";
        } else if (idu >= 90) {
            outStr += "E" + Integer.toString(idu - 90) + "S";
        } else {
            int aidu = Math.abs(idu);
            if (aidu >= 0 && aidu < 90) {
                outStr += "N" + Integer.toString(aidu) + "W";
            } else {
                outStr += "W" + Integer.toString(aidu - 90) + "S";
            }
        }

        return outStr;
    }
    @Override
    public void onLogger(int i, String s) {

    }
    private String parseSmsResult(int result) {
        if (result == Constant.CMD_RESULT_NONETWORK)
            return "http request failed.";
        if (result == Constant.CMD_RESULT_INNER_ERROR)
            return "sms server error.";
        if (result == Constant.CMD_RESULT_OK)
            return "sms sending.";
        return "?";
    }
    @Override
    public void onSmsRequestReply(final int result) {
        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                Toast.makeText(LoginActivity.this, "sms send: "+parseSmsResult(result), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private String authErr2Txt(int r) {
        if (r == Constant.E_AUTH_CODE_MISMATCH)
            return getString(R.string.binderr_auth_code);
        if (r == Constant.E_AUTH_BOUND_EXIST)
            return getString(R.string.binderr_bound_exist);
        if (r == Constant.E_AUTH_INNER_ERR)
            return getString(R.string.binderr_inner);
        if (r == Constant.E_AUTH_NO_PHONE)
            return getString(R.string.binderr_no_phone);
        if (r == Constant.E_AUTH_NO_USER)
            return getString(R.string.binderr_no_account);
        if (r == Constant.E_AUTH_PHONE_MISMATCH)
            return getString(R.string.binderr_phoneNo);
        if (r == Constant.E_AUTH_PHONE_USED)
            return getString(R.string.binderr_phone_used);
        if (r == Constant.E_AUTH_SMS_SERVER)
            return getString(R.string.binderr_sms_server);
        if (r == Constant.E_AUTH_PHONE_INVALID)
            return getString(R.string.binderr_phone_unava);
        if (r == Constant.E_AUTH_TIMEOUT)
            return getString(R.string.binderr_network);
        return getString(R.string.binderr_unknown);
    }


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


    // 异步处理
    private boolean lineMuteDown = false;
    private boolean needUnbind = true;
    //  去掉了static ***********************************************************
    private  class UiHandler extends Handler {
        WeakReference<LoginActivity> wrActi;
        LoginActivity mActi = null;
        public UiHandler(LoginActivity act) {
            wrActi = new WeakReference<LoginActivity>(act);
        }
        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            mActi = wrActi.get();
            if (mActi == null)
                return;
            switch (msg.what) {
                // pre-login procedures
                case MsgCode.ASKFORSTARTSDK: // UI started here.
                    startStep = StartStage.LOGIN_VISITOR;
                    mActi.registerUser.startVisitor(R.layout.dialog_visitor);
                    break;
                case MsgCode.REGISTERED:        // 显示：注册
                    startStep = StartStage.REGISTER_USER;
                    mActi.registerUser.startRegister(R.layout.dialog_register);
                    break;
                case MsgCode.LOGINPAGE: // NEXT goes here.
                    startStep = StartStage.LOGIN_USER;
                    mActi.registerUser.startUserLogin(R.layout.dialog_login);
                    break;
                case MsgCode.RESETPASSWD:       // 显示：重置密码
                    mActi.userPhone = (String) msg.obj;
                    mActi.accountApi.requestResetPass("******", mActi.userPhone);
                    break;
                // login choices:
                case MsgCode.CREATEVISITOR:     // 创建游客
                    String devID = API.getDeviceID(mActi);
                    mActi.userNick = ((HashMap<String, String>) (msg.obj))
                            .get(RegisterUser.KeyNick);
                    mActi.userPass = "888888";
                    mActi.accountApi.createVisitor(devID, mActi.userNick);
                    mActi.isVisitor = true;
                    mActi.showProcessing("create visitor");
                    break;
                case MsgCode.ASKFORREGISTER:    // 注册
                    String invite = ((HashMap<String, String>) (msg.obj))
                            .get(RegisterUser.KeyInviteCode);
                    mActi.userNick = ((HashMap<String, String>) (msg.obj))
                            .get(RegisterUser.KeyNick);
                    mActi.userPass = ((HashMap<String, String>) (msg.obj))
                            .get(RegisterUser.KeyPassword);
                    mActi.accountApi.createUser(invite, mActi.userNick, mActi.userPass);
                    mActi.isVisitor = false;
                    mActi.showProcessing("register user");
                    break;
                case MsgCode.ASKFORLOGIN:       // 登录
                    mActi.isVisitor = (msg.arg1 == 2);
                    mActi.userAccount = ((HashMap<String, String>) (msg.obj))
                            .get(RegisterUser.KeyAccount);
                    mActi.userPass = ((HashMap<String, String>) (msg.obj))
                            .get(RegisterUser.KeyPassword);
                    if (mActi.isVisitor) {
                        mActi.accountApi.login(mActi.userAccount, mActi.userPass);
                    } else {
                        if (msg.arg2 == 1)
                            mActi.accountApi.loginPhone(mActi.userAccount, mActi.userPass);
                        else
                            mActi.accountApi.login(mActi.userAccount, mActi.userPass);
                    }
                    mActi.showProcessing("login ...");
                    break;
                case MsgCode.ASKFORRESETPW:     // 重置密码
                    mActi.userAccount = ((HashMap<String, String>) (msg.obj))
                            .get(RegisterUser.KeyAccount);
                    String authCode = ((HashMap<String, String>) (msg.obj))
                            .get(RegisterUser.KeyAuthCode);
                    String passwd = ((HashMap<String, String>) (msg.obj))
                            .get(RegisterUser.KeyPassword);
                    mActi.accountApi.commandResetPass(msg.arg2, mActi.userAccount,
                            mActi.userPhone, authCode, passwd);
                    break;
                case MsgCode.MC_LOGINFINISHED:
                    mActi.dismissProcessing();
                    if (msg.arg1 == 1) { // create user finished.
                        mActi.registerUser.removeUserInfo();
                    }
                    break;
                // login result:
                case MsgCode.ASKFOREXIT:
                    mActi.setContentView(R.layout.welcome);
                    if (mActi.newBind) {
                        mActi.newBind = false;
                        mActi.needUnbind = true;
                        mActi.finish();
                    } else {
                        mActi.needUnbind = false;
                        mActi.finish();
                    }
                    break;
                case MsgCode.MC_LOGINOK: // arg1: userId, arg2: userState
//                    mActi.registerUser.setVisitor(mActi.isVisitor);
//                    mActi.registerUser.setUserRegistered();
//                    mActi.registerUser.saveUserInfo(mActi.userAccount, mActi.userNick, mActi.userPass);
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("selfId", msg.arg1);
                    Log.d(TAG, "登录成功,id:" + msg.arg1);
                    startActivity(intent);
                    break;
                    // no break here.
                case MsgCode.MC_SDKISRUNNING:
                    mActi.selfId = msg.arg1;
                    mActi.initMyADChannels();
                    mActi.initNotificationLauncher();
                    mActi.start_channel_fragment(mActi.selfId, msg.arg2);
                    mActi.start_talk_fragment(mActi.selfId, msg.arg2);
                    break;
                //
                case MsgCode.MC_ONSESSIONESTABLISHED:
                    mActi.currSession = new CompactID(msg.arg1, msg.arg2);
                    if (mActi.talkFragment != null)
                        mActi.talkFragment.onCurrentSessionChanged(mActi.selfId,
                                mActi.currSession.getType(), mActi.currSession.getId());
                    if (mActi.channelFragment != null)
                        mActi.channelFragment.onStartTalkFragment(mActi.selfId,
                                mActi.currSession.getType(), mActi.currSession.getId());
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
                    int[] ids = (int[]) msg.obj;
                    mActi.showSelectedMemberDialog(msg.arg1, msg.arg2, ids);
                    break;
                case MsgCode.MC_OUTPUTINDREQ:
                    break;
                case MsgCode.MC_LINECONTROLBUTTON:
                    if (msg.arg2 != 79 || mActi.talkFragment == null)
                        break;
                    if (!mActi.lineMuteDown && msg.arg1 == MotionEvent.ACTION_DOWN) {
                        mActi.talkFragment.processPttAction(MotionEvent.ACTION_DOWN);
                        mActi.lineMuteDown = true;
                    } else if (mActi.lineMuteDown
                            && msg.arg1 == MotionEvent.ACTION_UP) {
                        mActi.talkFragment.processPttAction(MotionEvent.ACTION_UP);
                        mActi.lineMuteDown = false;
                    }
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
    private void initMyADChannels() {
        List<Integer> cids = getMyPreferedChannels();
        if (cids == null) {
            cids = new ArrayList<Integer>();
            cids.add(1);
            cids.add(5);
            setMyPreferedChannels(cids);
        }
    }
    private void initNotificationLauncher() {
        final int NOTIFICATION_BASE_NUMBER = 110;
        Notification.Builder builder = null;
        Notification notify = null;

        // Intent intent = new Intent(this, TLActivity.class);
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setClass(this, NotificationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(LoginActivity.this, 0, intent, 0);

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Resources res = getResources();
        builder = new Notification.Builder(LoginActivity.this);
        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.tourlink_icon_small)
                .setLargeIcon(
                        BitmapFactory.decodeResource(res,
                                R.drawable.tourlink_icon_big)).setTicker(getResources().getString(R.string.tourling))
                .setWhen(System.currentTimeMillis()).setAutoCancel(false)
                .setContentTitle(getResources().getString(R.string.tourling)+"TourLink").setContentText(getResources().getString(R.string.jzone))
                .setOngoing(true);
        notify = builder.getNotification();
        // n.defaults =Notification.DEFAULT_SOUND;
        nm.notify(NOTIFICATION_BASE_NUMBER, notify);
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


    private void showSelectedMemberDialog(int cid, int seleType, int[] ids) {
        if (seleType == ChannelFragment.SELE_TYPE_SUMMON)
            showSendSmsDialog(CompactID.getType(cid), CompactID.getId(cid), ids);
        else if (seleType == ChannelFragment.SELE_TYPE_CALL)
            showCallMembersDialog(CompactID.getType(cid), CompactID.getId(cid), ids);
    }
    private void showCallMembersDialog(final int ctype, final int sid, int[] ids) {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_4_dialog_session, null);
        TextView tvMembers = (TextView) promptsView
                .findViewById(R.id.dialog_members);
        tvMembers.setText(makeDialogDispNames(ids));
        TextView tvDiaHint = (TextView) promptsView
                .findViewById(R.id.dialog_type);
        tvDiaHint.setText(R.string.end_dialogsession);

        final int[] memberIds = ids;
        Dialog dialog = new AlertDialog.Builder(this, R.style.menuDialogStyle)
                .setView(promptsView).setCancelable(true)
                .setNegativeButton(getResources().getString(R.string.dialog), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (talkFragment != null)
                            talkFragment.startDialog(selfId, ctype, sid,
                                    memberIds);
                    }
                }).setPositiveButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create();
        dialog.show();
    }
    private static final int MAXDISPNAMES = 3;
    private String makeDialogDispNames(int[] ids) {
        String dispNames = "";
        int i = 0;
        for (; i < (ids.length > MAXDISPNAMES ? MAXDISPNAMES : (ids.length - 1)); i++) {
            dispNames += API.uid2nick(ids[i]) + ",";
        }
        dispNames += API.uid2nick(ids[i]);
        if (ids.length > MAXDISPNAMES + 1) {
            dispNames += getResources().getString(R.string.dengdeng) + ids.length + getResources().getString(R.string.ren);
        } else if (ids.length > 1) {
            dispNames +=  + ids.length + getResources().getString(R.string.ren);
        }
        return dispNames;
    }





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
			/*
			 * LinearLayout llayout = new LinearLayout(LoginActivity.this);
			 * llayout.setOrientation(LinearLayout.HORIZONTAL); ImageView imgv =
			 * new ImageView(LoginActivity.this);
			 * imgv.setImageDrawable((getResources
			 * ().getDrawable(getDefaultChDrawId(position))));
			 * llayout.addView(imgv); TextView tv = new
			 * TextView(LoginActivity.this);
			 * tv.setText(allChannels.get(position).name);
			 * tv.setTextColor(0xFF999999); tv.setTextSize(20);
			 * llayout.addView(tv); return llayout;
			 */
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
    public void showSetDefaultSpinner() {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.default_channel_spinner, null);
        Spinner mSpinner = (Spinner) promptsView
                .findViewById(R.id.default_channel);

        List<Channel> allCHs = channelFragment
                .getDefaultChannelCandidates(selfId);
        DfltChAdapter adapter = new DfltChAdapter(allCHs);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                Channel che1 = (Channel) parent.getAdapter().getItem(position);
                newDefaultCh = new Channel(che1.cid.getType(),
                        che1.cid.getId(), che1.name);
                Log.d(TAG, "default id selected:" + che1.name);
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
                        if (newDefaultCh != null) {
                            if (oldDefaultCh == null
                                    || !Channel.sameChannel(newDefaultCh,
                                    oldDefaultCh))
                                channelFragment.setDefaultChannel(selfId,
                                        newDefaultCh.cid.getType(),
                                        newDefaultCh.cid.getId());
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
            processCircle = ProgressCircleLite.show(LoginActivity.this, "", "",
                    false, true);
        }
    }
    private void uiShowProgressCircle() {
        runOnUiThread(new Runnable(){
            @Override
            public void run() {
                showProgressCircle();
            }
        });
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


    public boolean isBluetoothStarted() {
        if (deviceApi != null) {
            AudioDev aDev = deviceApi.getCurrentAudioDevice();
            int curDev = aDev.type;
            bluetoothStarted = (curDev == Constant.AUDIO_BLUEHANDSET);
            return bluetoothStarted;
        }
        return false;
    }
    public boolean isUserBoundPhone() {
        return userBoundPhone;
    }




    private static final String DownloadApkUri = "http://121.199.44.69:8081/miniTL-dev.apk";
    private void downLoadApk() {
        final ProgressDialog pd;
        pd = new ProgressDialog(LoginActivity.this);
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
    @SuppressWarnings("static-access")
    private void installApk(File updFile) {
        Intent intent = new Intent();
        intent.setAction(intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(updFile),
                "application/vnd.android.package-archive");
        startActivity(intent);
    }
    private void showUpdateDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this,
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
    private void userLogout() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancelAll();

        if (selfId > 0) {
            accountApi.logout(selfId);
            selfId = 0;
        }
    }
    private void userExit() {
        stop_both_fragments();
        if (uiHandler != null) {
            uiHandler.sendEmptyMessage(MsgCode.ASKFOREXIT);
        } else {
            LoginActivity.this.finish();
        }
    }
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
    private void showSendSmsDialog(int ctype, int sid, int[] ids) {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_4_dialog_session, null);
        TextView tvMembers = (TextView) promptsView.findViewById(R.id.dialog_members);
        tvMembers.setText(makeDialogDispNames(ids));
        TextView tvDiaHint = (TextView) promptsView.findViewById(R.id.dialog_type);
        tvDiaHint.setText(getResources().getString(R.string.sending_sms));

        final JSONObject jsms = new JSONObject();
        try {
            jsms.put("type", "channel_summon");
            jsms.put("id", Integer.toString(sid));
        } catch (JSONException e) {
        }
        ;
        final int[] memberIds = ids;
        Dialog dialog = new AlertDialog.Builder(this, R.style.menuDialogStyle)
                .setView(promptsView).setCancelable(true)
                .setNegativeButton(getResources().getString(R.string.sms_seg), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        for (int i = 0; i < memberIds.length; i++)
                            accountApi.shakeScreen(selfId, memberIds[i],
                                    jsms.toString());
                    }
                }).setPositiveButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).create();
        dialog.show();
    }







    private List<String[]> bleDevs = new ArrayList<>();
    private AlertDialog bleAD = null;
    private void addDevice2BleList(String name, BluetoothDevice dev, final SelectCallBack cb) {
        if (bleAD != null) {
            bleAD.dismiss();
            bleAD = null;
        } else {
            bleDevs.clear();
        }
        String[] newDev = new String[2];
        newDev[0] = dev.getAddress();
        newDev[1] = name;
        bleDevs.add(newDev);

        final CharSequence[] devs = new CharSequence[bleDevs.size()];
        for (int i = 0; i < bleDevs.size(); i++) {
            String[] strs = bleDevs.get(i);
            String dname = strs[1];
            devs[i] = dname.concat("            ").subSequence(0, 12) + " <" + strs[0].substring(9) + ">";
            Log.i(TAG, i+" = "+devs[i]);
        }

        bleAD = new AlertDialog.Builder(LoginActivity.this, R.style.menuDialogStyle)
                .setTitle(getResources().getString(R.string.select_bluetooth))
                .setItems(devs, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String bltAddr = bleDevs.get(which)[0];
                        String bltName = bleDevs.get(which)[1];
                        cb.onItem(bltName, bltAddr);
                        bleDevs.clear();
                        bleAD = null;
                    }
                })
                .show();
    }



    public abstract class OnTLVerGetListener {
        abstract public void onTLVersionGet(int ver);
    }
    private static final String GetVerUri = "http://121.199.44.69:8081/rest/appver/get";
    public interface NOTIFICATION_ID {
        public static int UNREAD_SMS = 102;
        public static int MISSED_CALL = 103;
    }

    private int unReadSms = 0;
    private String smsSender = null;
    private class NotifySMS implements Runnable {
        private String sms;

        public NotifySMS(String name, String txt) {
            smsSender = name;
            this.sms = txt;
        }

        @Override
        public void run() {
            int sid = 0;
            try {
                JSONObject obj = new JSONObject(this.sms);
                sid = obj.getInt("id");
            } catch (JSONException e) {}
            setUnReadSmsNotification(smsSender, sms, sid);
        }
    }
    @TargetApi(16)
    private void setUnReadSmsNotification(String sender, String sms, int sid) {
        Intent nIntent = new Intent(ACTIONS.UNREAD_SHORTMESSAGE);
        Log.d(TAG, "put extra name:" + sender + " sms:" + sms);
        nIntent.putExtra(ACTIONS.SMS_SENDER_NAME, sender);
        nIntent.putExtra(ACTIONS.SMS_CONTENT, sms);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, nIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        unReadSms++;

        String newSndr = smsSender + "@" + getChannelNameById(Constant.SESSION_TYPE_CHANNEL, sid);
        unReadSenderList = unReadSenderList.equals("") ? newSndr : newSndr
                + " / " + unReadSenderList;
        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.drawable.unread_sms);
        String cTitle = (unReadSms == 1) ? getResources().getString(R.string.tl_sms_hint) : getResources().getString(R.string.tl_sms_hint1) + unReadSms
                + getString(R.string.tl_sms_hint2);
        String cText = (unReadSenderList.length() <= 21) ? getString(R.string.comes_from)
                + unReadSenderList : unReadSenderList.substring(0, 20) + " ...";
        SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss",
                Locale.US);
        String cTime = format.format(new Date());

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        Notification.Builder nBuilder = new Notification.Builder(this);
        nBuilder.setContentTitle(cTitle).setContentText(cText + " " + cTime)
                .setContentIntent(pendingIntent).setTicker(getString(R.string.tl_new_sms))
                .setWhen(System.currentTimeMillis()).setAutoCancel(true)
                .setOngoing(false).setDefaults(Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.unread_sms2)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 64, 64, false));
        Notification notification = nBuilder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;

        mNotificationManager.notify(NOTIFICATION_ID.UNREAD_SMS, notification);

        if (unReadSms == 1)
            registerReceiver(missingDialogReceiver, new IntentFilter(
                    ACTIONS.UNREAD_SHORTMESSAGE));
    }

}


