package com.hhuc.sillyboys.tuling.tl_demo;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.algebra.sdk.API;
import com.algebra.sdk.DeviceApi;
import com.algebra.sdk.OnMediaListener;
import com.algebra.sdk.SessionApi;
import com.algebra.sdk.entity.Channel;
import com.algebra.sdk.entity.CompactID;
import com.algebra.sdk.entity.Constant;
import com.algebra.sdk.entity.HistoryRecord;
import com.hhuc.sillyboys.tuling.broadcast.BroadcastActivity;
import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.entity.MsgCode;
import com.hhuc.sillyboys.tuling.mzy.topic.TopicDialog;
import com.hhuc.sillyboys.tuling.util.MediaKeys;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.hhuc.sillyboys.tuling.tl_demo.TalkHistory.AMR475_PL_SIZE;

public class TalkFragment extends Fragment implements OnMediaListener,
		View.OnTouchListener, OnClickListener {
	private static final String TAG = "fragment.talk";
	private static final int FALSE = 0;
	private static final int TRUE = 1;
	// output_dev.xml Level:
	public static final int OUT_PHONEBODY = 0;
	public static final int OUT_BLUETOOTH_DISC = 1;
	public static final int OUT_BLUETOOTH_CONN = 2;
	public static final int OUT_BLUETOOTH_ERR = 4;
	public static final int OUT_BLUETOOTH_AERR = 5;

	private Context uiContext = null;
	private Handler uiHandler = null;
	private SessionApi sessionApi = null;
	private DeviceApi deviceApi = null;
	private AudioManager mAudioManager = null;
	private ComponentName mediaKeys = null;
	private TalkHistory talkHistory = null;

	private int selfId = 0;
	private CompactID currSession = null;
	private CompactID dispSession = null;
	private int selfStatus = Constant.CONTACT_STATE_ONLINE;
	private boolean isUndistube = false;
	private HistoryRecord newLastSpeaking = null;

	private ArrayList<Integer> waitList = new ArrayList<Integer>();
	private static boolean pttTriggerable = false;

	private TextView broadcast_me_item = null;
	private ToggleButton broadcast_ptt_button = null;
	private ImageView broadcast_playlast_button = null;
	private ImageView broadcast_ptt_mic_ind = null;
	private ImageView broadcast_ptt_spk_ind = null;
	private ImageView broadcast_level_ind = null;
	private TextView speaker_item = null;
	private TextView queuer_item = null;
	private ImageView record_ind = null;
	private TextView player_ind = null;
	private ImageView broadcast_configure_button = null;
	private ImageView output_ind = null;
	private ImageView hs_battery_ind = null;

	private ImageView topic_helper = null;
    private TextView broadcast_title, broadcast_creator, broadcast_status, broadcast_time, broadcast_school;
    private SharedPreferences pref;
	private String identity = "user";
	private String type = "broadcast";
	private TextView manage_broadcast, manage_talk, choose_user;

	@Override
	public void onAttach(Activity act) {
		super.onAttach(act);
		uiContext = (BroadcastActivity) act;
		Log.i(TAG, "onAttach ....");
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		selfId = getArguments().getInt("id.self");
		selfNick = getArguments().getString("nick.self");
		selfStatus = getArguments().getInt("state.self");
		Log.i(TAG, "onCreate with uid:" + selfId + " state:" + selfStatus);

		mAudioManager = ((AudioManager) uiContext.getSystemService(Context.AUDIO_SERVICE));
		talkHistory = new TalkHistory(selfId);

        pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.i(TAG, "onResume ....");

		uiHandler = BroadcastActivity.getUiHandler();
		if (sessionApi == null) {
			uiHandler.postDelayed(delayInitApi, 300);
		}
		if (broadcast_playlast_button != null) // crash catched button is null!
		{
			if (newLastSpeaking != null && !newLastSpeaking.played) {
				broadcast_playlast_button.setSelected(true);
			} else {
				broadcast_playlast_button.setSelected(false);
			}
			broadcast_playlast_button.setActivated(false);

			output_ind.getDrawable().setLevel(getOutputDev());

			boolean bound = ((BroadcastActivity) uiContext).isUserBoundPhone();
			if (bound) {
				// broadcast_me_item.setTextColor(Color.rgb(232, 237, 35));
				Drawable dBound = uiContext.getResources().getDrawable(
						R.drawable.member_bound);
				broadcast_me_item.setCompoundDrawablesWithIntrinsicBounds(dBound, null,
						null, null);
			}

			isUndistube = API.getAccountApi().isUndistubed();
			setUndistubeIcon(isUndistube);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.i(TAG, "onStop ....");

		autoReleasePtt();
	}

	private Runnable delayInitApi = new Runnable() {
		@Override
		public void run() {
			if ((sessionApi = API.getSessionApi()) != null) {
				deviceApi = API.getDeviceApi();
				sessionApi.setOnMediaListener(TalkFragment.this);
				isUndistube = API.getAccountApi().isUndistubed();
				setUndistubeIcon(isUndistube);
			} else {
				uiHandler.postDelayed(delayInitApi, 300);
			}
		}
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy ....");

		if (sessionApi != null) {
			sessionApi.setOnMediaListener(null);
			sessionApi = null;
		} else if (uiHandler != null) { // don't know why uiHandler can be null.
			uiHandler.removeCallbacks(delayInitApi);
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outBu) {
		super.onSaveInstanceState(outBu);
		Log.i(TAG, "onSaveInstanceState ....");

		outBu.putString("StopByAndroid", "yes");
	}

	private interface PLS {
		public int LASTSPEAKING_NEW = 7;
		public int LASTSPEAKING_PLAYING = 8;
		public int LASTSPEAKING_END = 9;
		public int LASTSPEAKING_GONE = 10;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		if (container != null) { // && ((BroadcastActivity)uiContext).isHorizonScreen()
			View view = inflater.inflate(R.layout.fragment_talk, container,
					false);

			topic_helper = (ImageView) view.findViewById(R.id.broadcast_topic_helper);
			topic_helper.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					Log.d(TAG, "话题帮助");
					TopicDialog topicDialog = new TopicDialog(getActivity());
					topicDialog.show();
				}
			});

			broadcast_title = (TextView) view.findViewById(R.id.broadcast_title);
            broadcast_creator = (TextView) view.findViewById(R.id.broadcast_user_creator);
            broadcast_status = (TextView) view.findViewById(R.id.broadcast_user_status);
            broadcast_time = (TextView) view.findViewById(R.id.broadcast_user_time);
            broadcast_school = (TextView) view.findViewById(R.id.broadcast_user_school);
			manage_broadcast = (TextView) view.findViewById(R.id.broadcast_manage_broadcast);
			manage_broadcast.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					Log.d("TAG", "开始/暂停广播");
					boolean wheatherBroadcast = false;
					if(wheatherBroadcast){
						manage_broadcast.setText("开始广播");
						wheatherBroadcast = false;
					}else{
						manage_broadcast.setText("暂停广播");
						wheatherBroadcast = true;
					}
				}
			});
			manage_talk = (TextView) view.findViewById(R.id.broadcast_manage_talk);
			manage_talk.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					Log.d(TAG,  "开启/关闭讨论");
					boolean wheatherChat = false;
					if(wheatherChat){
						manage_talk.setText("开启讨论");
						wheatherChat = false;
					}else{
						manage_talk.setText("关闭讨论");
						wheatherChat = true;
					}
				}
			});
			choose_user = (TextView) view.findViewById(R.id.broadcast_manage_choose_user);
			choose_user.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					Log.d(TAG, "选择幸运用户");
				}
			});

            type = pref.getString("type", "broadcast");
			// 好友面板:豆豆
			if(type.equals("chat")){
				Log.d(TAG, "会话类型为" + type);
				view.findViewById(R.id.broadcast_user_block).setVisibility(View.VISIBLE);
				view.findViewById(R.id.broadcast_admin_block).setVisibility(View.INVISIBLE);
				broadcast_title.setText("好友信息");
				broadcast_creator.setText("昵称：豆豆");
				broadcast_status.setText("性别：男");
				broadcast_time.setText("爱好：编程，打游戏，和朋友出去骑车");
				broadcast_school.setText("学校：XX大学");
			}
			// 悄悄话面板
			else if(type.equals("secret")){
				Log.d(TAG, "会话类型为" + type);
				view.findViewById(R.id.broadcast_user_block).setVisibility(View.VISIBLE);
				view.findViewById(R.id.broadcast_admin_block).setVisibility(View.INVISIBLE);
//				broadcast_title.setText("Ta的信息");
//				broadcast_creator.setText("对方是一名19岁的女生");
//				broadcast_status.setText("兴趣爱好是：读一些文学书籍，听音乐，偶尔逛街");
//				broadcast_time.setText("你们的话题：情感");
//				broadcast_school.setVisibility(View.INVISIBLE);
				broadcast_title.setText("Ta的信息");
				broadcast_creator.setText("对方是一名18岁的男生");
				broadcast_status.setText("兴趣爱好是：游戏，足球，骑车");
				broadcast_time.setText("你们的话题：情感");
				broadcast_school.setVisibility(View.INVISIBLE);
			}
			// 外面面板:宝食林
			else if(type.equals("shop")){
				Log.d(TAG, "会话类型为" + type);
				view.findViewById(R.id.broadcast_user_block).setVisibility(View.VISIBLE);
				view.findViewById(R.id.broadcast_admin_block).setVisibility(View.INVISIBLE);
				broadcast_title.setText("店家信息");
				broadcast_creator.setText("名称：宝食林");
				broadcast_status.setText("位置：xx市xx区xx路xx号");
				broadcast_time.setText("小提示：外卖正在路上~请保持语音畅通");
				broadcast_school.setVisibility(View.INVISIBLE);
			}
			// 群聊面板:g17
			else if(type.equals("group")){
				Log.d(TAG, "会话类型为" + type);
				view.findViewById(R.id.broadcast_user_block).setVisibility(View.VISIBLE);
				view.findViewById(R.id.broadcast_admin_block).setVisibility(View.INVISIBLE);
				broadcast_title.setText("群组信息");
				broadcast_creator.setText("名称：g17");
				broadcast_status.setText("创建时间：2017-07-02");
				broadcast_time.setText("成员：乐乐，豆豆，小柯，马哲，红哥");
				broadcast_school.setVisibility(View.INVISIBLE);
			}
			// 广播面板
			else if(type.equals("broadcast")){
				Log.d(TAG, "会话类型为" + type);
				identity = pref.getString("identity", "user");
				if(identity.equals("user")){
					Log.d(TAG, "身份类型为" + identity);
					view.findViewById(R.id.broadcast_user_block).setVisibility(View.VISIBLE);
					view.findViewById(R.id.broadcast_admin_block).setVisibility(View.INVISIBLE);
				}else if(identity.equals("admin")){
					Log.d(TAG, "身份类型为" + identity);
					view.findViewById(R.id.broadcast_admin_block).setVisibility(View.VISIBLE);
					view.findViewById(R.id.broadcast_user_block).setVisibility(View.INVISIBLE);
				}
			}


			broadcast_me_item = (TextView) view.findViewById(R.id.broadcast_me_item);
			setUndistubeIcon(isUndistube);
			broadcast_me_item.setText(selfNick != null ? selfNick : API.uid2nick(selfId));

			broadcast_ptt_button = (ToggleButton) view.findViewById(R.id.broadcast_ptt_button);
			if (selfStatus == Constant.CONTACT_STATE_ONLINE)
				broadcast_ptt_button.setSelected(true);
			broadcast_ptt_button.setOnTouchListener(this);

			broadcast_configure_button = (ImageView) view.findViewById(R.id.broadcast_configure_button);
			broadcast_configure_button.setOnClickListener(this);

			broadcast_ptt_mic_ind = (ImageView) view.findViewById(R.id.broadcast_ptt_mic_ind);
			broadcast_ptt_mic_ind.setSelected(true);
			broadcast_ptt_mic_ind.setPressed(false);
			broadcast_ptt_spk_ind = (ImageView) view.findViewById(R.id.broadcast_ptt_spk_ind);
			broadcast_ptt_spk_ind.setSelected(true);
			broadcast_ptt_spk_ind.setPressed(false);

			broadcast_level_ind = (ImageView) view.findViewById(R.id.broadcast_level_ind);
			broadcast_level_ind.getDrawable().setLevel(0);

			speaker_item = (TextView) view.findViewById(R.id.broadcast_speaker_ind);
			speaker_item.setVisibility(View.INVISIBLE);
			queuer_item = (TextView) view.findViewById(R.id.broadcast_queuer_ind);
			queuer_item.setVisibility(View.INVISIBLE);

			broadcast_playlast_button = (ImageView) view
					.findViewById(R.id.broadcast_playlast_button);
			broadcast_playlast_button.setOnClickListener(this);
			broadcast_playlast_button.setActivated(false);

			record_ind = (ImageView) view.findViewById(R.id.record_ind);
			record_ind.setVisibility(View.INVISIBLE);
			player_ind = (TextView) view.findViewById(R.id.player_ind);
			player_ind.setVisibility(View.INVISIBLE);

			initHisPlayers(view);

			output_ind = (ImageView) view.findViewById(R.id.output_select_button);
			output_ind.setLongClickable(true);
			output_ind.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View arg0) {
					if (uiHandler != null)
						uiHandler.sendEmptyMessage(MsgCode.MC_OUTPUTINDREQ);
					return false;
				}
			});
			hs_battery_ind = (ImageView) view.findViewById(R.id.handset_battery_level);
			hs_battery_ind.setVisibility(View.INVISIBLE);

			Log.i(TAG, "onCreateView OK ....");
			return view;
		} else {
			Log.i(TAG, "onCreateView IGNORE ....");
			return null;
		}

	}

	private void setUndistubeIcon(boolean on) {
		if (on) {
			Drawable dUndist = uiContext.getResources().getDrawable(
					R.drawable.member_undistube);
			broadcast_me_item.setCompoundDrawablesWithIntrinsicBounds(dUndist, null,
					null, null);
		}
	}

	private List<TextView> hisPlys = new ArrayList<TextView>();

	private void initHisPlayers(View view) {
		hisPlys.add(0, (TextView) view.findViewById(R.id.player_his1));
		hisPlys.add(1, (TextView) view.findViewById(R.id.player_his2));
		hisPlys.add(2, (TextView) view.findViewById(R.id.player_his3));
		clearHisPlayers();
	}

	private void clearHisPlayers() {
		for (int i = 0; i < 3; i++) {
			hisPlys.get(i).setText("");
			hisPlys.get(i).setVisibility(View.INVISIBLE);
		}
	}

	private int getOutputDev() {
		if (((BroadcastActivity) getActivity()).isBluetoothStarted())
			return OUT_BLUETOOTH_DISC;
		else
			return OUT_PHONEBODY;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Log.i(TAG, "onDestroyView ....");
	}

	/*
	 * 
	 * BroadcastActivity (BroadcastActivity) 'SET' functions:
	 */

	public void startDialog(int self, int ctype, int sid, int[] ids) {
		if (sessionApi != null)
			sessionApi.startDialog(self, ctype, sid, ids);
	}

	public void stopDialog(int self, int dialog) {
		if (sessionApi != null)
			sessionApi.stopDialog(self, dialog);
	}

	public void onCurrentSessionChanged(int self, int ctype, int cid) {
		Log.i(TAG, "onCurrentSessionChanged " + ctype + ":" + cid);
		if (ctype != Constant.SESSION_TYPE_NONE) { // enter session
			currSession = new CompactID(ctype, cid);
			talkHistory.openFiles4Write(selfId, currSession.getCompactId());
		} else { // leave session
			currSession = null;
			newLastSpeaking = null;
			talkHistory.closeFiles();
		}
		uiTalkSessionChange();
	}

	public void onDisplaySessionChanged(int self, int ctype, int cid) {
		Log.i(TAG, "onDisplaySessionChanged " + ctype + ":" + cid);
		if (ctype != Constant.SESSION_TYPE_NONE) { // enter session
			dispSession = new CompactID(ctype, cid);
		} else { // leave session
			dispSession = null;
		}
		uiTalkSessionChange();
	}

	private void uiTalkSessionChange() {
		if (Channel.sameCid(currSession, dispSession)) {
			asyncSetUiItem(R.id.broadcast_ptt_button, TRUE);
			flushPlaylastButton();
		} else {
			asyncSetUiItem(R.id.broadcast_ptt_button, FALSE);
			asyncSetUiItem(R.id.broadcast_playlast_button, PLS.LASTSPEAKING_END);
		}

		clearHisPlayers();
	}

	private void flushPlaylastButton() {
		if (newLastSpeaking != null && !newLastSpeaking.played) {
			asyncSetUiItem(R.id.broadcast_playlast_button, PLS.LASTSPEAKING_NEW);
		} else {
			asyncSetUiItem(R.id.broadcast_playlast_button, PLS.LASTSPEAKING_END);
		}
	}

	public void onSelfStatusChange(int state) {
		selfStatus = state;

		if (broadcast_ptt_button == null)
			return;

		if (state == Constant.CONTACT_STATE_ONLINE)
			asyncSetUiItem(R.id.broadcast_ptt_button, TRUE);
		else
			asyncSetUiItem(R.id.broadcast_ptt_button, FALSE);
	}

	public void setSelfUndistubeInd(boolean undist) {
		boolean bound = ((BroadcastActivity) uiContext).isUserBoundPhone();
		if (!undist) {
			if (bound)
				asyncSetUiItem(R.id.broadcast_me_item, 3);
			else
				asyncSetUiItem(R.id.broadcast_me_item, 2);
		} else {
			asyncSetUiItem(R.id.broadcast_me_item, 1);
		}
	}

	private String selfNick = null;

	public void onSetNickName(String nick) {
		selfNick = nick;
		if (uiHandler != null) {
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					broadcast_me_item.setText(selfNick);
				}
			});
		}
	}

	public void onBoundPhone() {
		((BroadcastActivity) uiContext).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Drawable dBound = uiContext.getResources().getDrawable(
						R.drawable.member_bound);
				broadcast_me_item.setCompoundDrawablesWithIntrinsicBounds(dBound, null,
						null, null);
			}
		});
	}

	public void setPttTrigglableOn(boolean on) {
		if (getOutputDev() == OUT_BLUETOOTH_DISC) {
			pttTriggerable = false;
		} else {
			if (!pttTriggerable && on) {
				pttTriggerable = true;
				if (mediaKeys == null) {
					mediaKeys = new ComponentName(uiContext, MediaKeys.class);
					mAudioManager.registerMediaButtonEventReceiver(mediaKeys);
					Log.i(TAG, "register Media Buttons Receiver.");
				}
			} else if (pttTriggerable && !on) {
				pttTriggerable = false;
				if (mediaKeys != null) {
					mAudioManager.unregisterMediaButtonEventReceiver(mediaKeys);
					mediaKeys = null;
				}
			}
		}

		autoReleasePtt();
	}

	private void autoReleasePtt() {
		if (isPttPressed && currSession != null) {
			talkRelease(currSession); // pttPressed <- false
		}
		isPttPressed = false;
	}

	public void setOutputDeviceInd(int lo) {
		asyncSetUiItem(R.id.output_select_button, lo);
		if (lo != OUT_BLUETOOTH_DISC && lo != OUT_BLUETOOTH_CONN) {
			asyncSetUiItem(R.id.handset_battery_level, 0);
		}
	}

	public boolean getPttTrigglable() {
		return pttTriggerable;
	}

	public int getAudioAmplitude() {
		if (deviceApi != null) {
			return deviceApi.getAudioAmpRate();
		}
		return 0;
	}

	public void setAudioAmplitude(int level) {
		if (deviceApi != null) {
			deviceApi.setAudioAmpRate(level);
		}
	}

	/*
	 * 
	 * OnMediaListener callbacks:
	 */

	@Override
	public void onPttButtonPressed(int uid, int state) {
		Log.i(TAG, "onPttButtonPressed state: " + state);
		if ((state & 0xff) == 0 || state == 0x8000) {
			asyncSetUiItem(R.id.broadcast_ptt_button, TRUE);
		} else {
			asyncSetUiItem(R.id.broadcast_ptt_button, FALSE);
			if (deviceApi != null) {
			//	deviceApi.setDefaultSpeakerOn(true);
			//	deviceApi.playNotifySound(Sound.PLAYER_MEDIA_ERROR);
			}
		}

		if (isMeInQueue && state == Constant.TALKREL_SUCCESSFUL) {
			isMeInQueue = false;
			asyncSetUiItem(R.id.broadcast_queuer_ind, -1 * selfId);
		}
	}

	@Override
	public void onTalkRequestConfirm(int uid, int ctype, int sid, int tag, boolean enRed) {
		Log.i(TAG, "onTalkRequestConfirm " + ctype + ":" + sid + " " + tag);
		int[] ids = { R.id.broadcast_ptt_mic_ind, R.id.record_ind };
		int[] sts = { enRed ? 2 : TRUE, TRUE };
		asyncSetUiItems(ids, sts);

		if (isMeInQueue) {
			isMeInQueue = false;
			asyncSetUiItem(R.id.broadcast_queuer_ind, -1 * selfId);
		}
	}

	@Override
	public void onTalkRequestDeny(int uid, int ctype, int sid) {
		int[] ids = { R.id.broadcast_ptt_button, R.id.record_ind };
		int[] sts = { FALSE, FALSE };
		asyncSetUiItems(ids, sts);
	}

	private boolean isMeInQueue = false;

	@Override
	public void onTalkRequestQueued(int uid, int ctype, int sid) {
		isMeInQueue = true;
		asyncSetUiItem(R.id.broadcast_queuer_ind, selfId);
	}

	@Override
	public void onTalkReleaseConfirm(int arg0, int arg1) {
		Log.i(TAG, "onTalkReleaseConfirm " + arg0 + " " + arg1);
		int[] ids = { R.id.broadcast_ptt_mic_ind, R.id.broadcast_level_ind, R.id.record_ind };
		int[] sts = { FALSE, 0, FALSE };
		asyncSetUiItems(ids, sts);
	}

	@Override
	public void onMediaInitializedEnd(int userId, int channelType, int sessionId) {
		Toast.makeText(uiContext,
				channelType + ":" + sessionId + " media initialized.",
				Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onTalkTransmitBroken(int arg0, int arg1) {
		if (deviceApi != null) {
	//		deviceApi.makeVibrate(300);
		}
		asyncSetUiItem(R.id.broadcast_ptt_button, FALSE);
	}

	@Override
	public void onMediaReceiverReport(long arg0, int arg1, int arg2, int arg3, int arg4) {
	}

	@Override
	public void onMediaSenderReport(long arg0, int arg1, int arg2, int arg3, int arg4) {
	}

	/*
	 * 
	 * 
	 */

	@Override
	public void onNewSpeakingCatched(final HistoryRecord hisRec) {
		if (hisRec.owner != selfId) {
			asyncSetUiItem(R.id.broadcast_playlast_button, PLS.LASTSPEAKING_NEW);
			newLastSpeaking = hisRec;
			((BroadcastActivity) uiContext).runOnUiThread(new Runnable(){
				@Override
				public void run() {
					if (popLastSpk != null) {
						popLastSpk.dismiss();
						popLastSpk = null;
					}
				}
			});
		}
		//
		((BroadcastActivity) uiContext).runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Log.i(TAG, "save new speech: stype:"+ CompactID.getType(hisRec.session)+" sid:"+ CompactID.getId(hisRec.session));
				talkHistory.dumpSpeechBuffer(hisRec.played, hisRec.session, hisRec.owner, hisRec.tag,
						hisRec.mediaData, hisRec.mediaData.length);
			}
		});
	}

	@Override
	public void onPlayLastSpeaking(int idx, int dur10ms) {
		Log.i(TAG, "onPlayLastSpeaking idx:" + idx + " dur:" + dur10ms);
		if (dur10ms > 0) {		// start successfully
			int[] ids = {R.id.broadcast_playlast_button, R.id.player_ind};
			int[] sts = {PLS.LASTSPEAKING_PLAYING, idx};
			asyncSetUiItems(ids, sts);
			showSpeakingLevel = true;
		}
	}

	@Override
	public void onPlayLastSpeakingEnd(int speaker) {
		int[] ids = { R.id.broadcast_playlast_button, R.id.player_ind, R.id.broadcast_level_ind };
		int[] sts = { PLS.LASTSPEAKING_END, 0, 0 };
		asyncSetUiItems(ids, sts);
		showSpeakingLevel = false;

		if (lastPlaying >= 0 && popLastSpk != null) {
			recsData.get(lastPlaying).put(ISPLAYED, deacPlHd);
			plDatAdapter.notifyDataSetChanged();
			lastPlaying = -1;
		}
	}

	/*
	 * 
	 * 
	 * 
	 */

	private boolean delayShowSpeaking = false;
	private int delaySpeaker = 0;
	private boolean showSpeakingLevel = false;

	@Override
	public void onSomeoneSpeaking(int speaker, int ctype, int sessionId, int tag, int dur10ms) {
		Log.i(TAG, "speaker " + speaker + " tag " + tag + " dur " + dur10ms + " ptt:" + isPttPressed);
		if (isPttPressed && dur10ms < 200) {
			Log.i(TAG, "set delay show speaker " + speaker);
			delayShowSpeaking = true;
			delaySpeaker = speaker;
		} else {
			delayShowSpeaking = false;
			delaySpeaker = 0;
			asyncSetUiItem(R.id.broadcast_speaker_ind, speaker);
		}
		asyncSetUiItem(R.id.broadcast_queuer_ind, -1 * speaker);
		uiHandler.obtainMessage(MsgCode.MC_SESSIONACTIVE, ctype, sessionId).sendToTarget();
	}

	@Override
	public void onThatoneSayOver(int speaker, int tag) {
		delayShowSpeaking = false;
		delaySpeaker = 0;
		asyncSetUiItem(R.id.broadcast_speaker_ind, 0);
	}

	@Override
	public void onStartPlaying(int speaker, int ctype, int session, int tag) {
		Log.i(TAG, "onStartPlaying " + speaker + " " + session + " " + tag);
		int[] ids = { R.id.broadcast_ptt_spk_ind, R.id.player_ind };
		int[] sts = { TRUE, speaker };
		asyncSetUiItems(ids, sts);

		showSpeakingLevel = true;
		if (delayShowSpeaking) {
			asyncSetUiItem(R.id.broadcast_speaker_ind, delaySpeaker);
			delayShowSpeaking = false;
			delaySpeaker = 0;
		}
	}

	@Override
	public void onPlayStopped(int tag) {
		Log.i(TAG, "onPlayStopped " + tag);
		int[] ids = { R.id.broadcast_ptt_spk_ind, R.id.broadcast_level_ind, R.id.player_ind };
		int[] sts = { FALSE, 0, 0 };
		asyncSetUiItems(ids, sts);
		showSpeakingLevel = false;
	}

	@Override
	public void onSomeoneAttempt(int userId, int ctype, int sessionId) {
		asyncSetUiItem(R.id.broadcast_queuer_ind, userId);
	}

	@Override
	public void onThatAttemptQuit(int userId, int ctype, int sessionId) {
		asyncSetUiItem(R.id.broadcast_queuer_ind, -1 * userId);
	}

	@Override
	public void onMediaSenderCutted(int userId, int tag) {
		((BroadcastActivity) uiContext).runOnUiThread(new ReleaseTrigglePTT());
	}

	/*
	 * 
	 * 
	 */

	private int lastLevel = 0;

	@Override
	public void onPlayerMeter(int uid, int level) {
		if (showSpeakingLevel) {
			if (level != lastLevel)
				asyncSetUiItem(R.id.broadcast_level_ind, level);
			lastLevel = level;
		} else
			asyncSetUiItem(R.id.broadcast_level_ind, 0);
	}

	@Override
	public void onRecorderMeter(int uid, int level) {
		if (level != lastLevel)
			asyncSetUiItem(R.id.broadcast_level_ind, level);
		lastLevel = level;
	}

	@Override
	public void onBluetoothBatteryGet(int level) {
		asyncSetUiItem(R.id.handset_battery_level, level + 1);
	}

	private boolean bluetoothConnected = false;
	@Override
	public void onBluetoothConnect(int status) {
		Log.i(TAG, "onBluetoothConnect "+status);
		bluetoothConnected = (status == Constant.ON);

		if (!((BroadcastActivity) uiContext).isBluetoothStarted()) {
			setOutputDeviceInd(TalkFragment.OUT_PHONEBODY);
			bluetoothConnected = false;
			return;
		}

		// bluetoothStarted:
		if (status == Constant.ON) {
			setOutputDeviceInd(TalkFragment.OUT_BLUETOOTH_CONN);
		} else if (status == Constant.OFF) {
			setOutputDeviceInd(TalkFragment.OUT_BLUETOOTH_DISC);
		} else if (status == Constant.ERR) {
			setOutputDeviceInd(TalkFragment.OUT_BLUETOOTH_ERR);
		} else if (status == Constant.AERR) {
			setOutputDeviceInd(TalkFragment.OUT_BLUETOOTH_AERR);
		}
	}

	/*
	 * 
	 * UI functions:
	 */

	private boolean isPttTriggered = false;
	private volatile boolean isPttPressed = false;

	@Override
	public boolean onTouch(View _v, MotionEvent event) {
		int theAct = event.getAction();

		if (sessionApi == null || currSession == null) {
			if (isPttPressed) {
				isPttPressed = false;
			}
			if (theAct == MotionEvent.ACTION_UP) {
				Toast.makeText(uiContext, getResources().getString(R.string.no_session), Toast.LENGTH_SHORT).show();
			}
			return false;
		}

		if (!Channel.sameCid(currSession, dispSession)) {
			if (theAct == MotionEvent.ACTION_UP)
				uiHandler.obtainMessage(MsgCode.MC_SESSIONACTIVE,
						currSession.getType(), currSession.getId())
						.sendToTarget();
		} else {
			processPttAction(theAct);
		}

		return false;
	}

	public void processPttAction(int theAct) {
		if (!pttTriggerable) {
			if (theAct == MotionEvent.ACTION_DOWN) {
				// Log.i(TAG,
				// "ptt down. uid:"+selfId+" session "+sessionType+":"+sessionId);
				talkRequest(currSession);
			} else if (theAct == MotionEvent.ACTION_UP) {
				// Log.i(TAG, "ptt stop.");
				talkRelease(currSession);
			}
		} else { // ptt is trigger mode
			if (theAct == MotionEvent.ACTION_DOWN) {
				if (!isPttPressed) {
					isPttTriggered = true;
					talkRequest(currSession);
				}
			} else if (theAct == MotionEvent.ACTION_UP) {
				if (isPttPressed && !isPttTriggered) {
					talkRelease(currSession);
				}
				isPttTriggered = false;
			}
		}
	}

	private class ReleaseTrigglePTT implements Runnable {

		@Override
		public void run() {
			if (pttTriggerable && isPttPressed && !isPttTriggered) {
				talkRelease(currSession);
			}
		}

	}

	private void talkRequest(CompactID session) {
		isPttPressed = true;
		if (session != null && sessionApi != null)
			sessionApi.talkRequest(selfId, session.getType(), session.getId());
	}

	private void talkRelease(CompactID session) {
		isPttPressed = false;
		if (session != null && sessionApi != null)
			sessionApi.talkRelease(selfId, session.getType(), session.getId());
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.broadcast_playlast_button:
			if (broadcast_playlast_button.isActivated()) { // is playing
				broadcast_playlast_button.setActivated(false);
                Log.d(TAG, "历史按钮1");
				if (sessionApi != null)
					sessionApi.stopPlayLastSpeaking(selfId);
			} else if (broadcast_playlast_button.isSelected()) {
				if (Channel.sameCid(dispSession, currSession)) {
                    Log.d(TAG, "历史按钮2");
                    if (sessionApi != null)
						sessionApi.playLastSpeaking(selfId, 0, newLastSpeaking.mediaData);
					if (newLastSpeaking != null && !newLastSpeaking.played)
						newLastSpeaking.played = true;
				} else {
                    Log.d(TAG, "历史按钮3");
					if (currSession != null)
						uiHandler.obtainMessage(MsgCode.MC_SESSIONACTIVE,
								currSession.getType(), currSession.getId())
								.sendToTarget();
				}
			} else {
				if (popLastSpk == null) {
                    Log.d(TAG, "历史按钮4：打开历史记录");
                    // 隐藏广播信息
                    getActivity().findViewById(R.id.broadcast_info).setVisibility(View.INVISIBLE);

					HistoryRecord[] hisRecs = null;
					if (sessionApi != null)
						hisRecs = talkHistory.getAllHistoryRecords(dispSession.getCompactId());

					if (hisRecs != null && hisRecs.length > 0)
						showHistoryRecords(hisRecs, dispSession.getType(),
								dispSession.getId());
					else {
						Log.e(TAG, "no hisRec found, disp stype:" + dispSession.getType() + " sid:" + dispSession.getId());
						Toast.makeText(uiContext, getResources().getString(R.string.no_history),
								Toast.LENGTH_SHORT).show();
					}
				} else {
                    Log.d(TAG, "历史按钮5");
					if (popLastSpk.isShowing())
						popLastSpk.dismiss();
					popLastSpk = null;
				}
			}
			break;
		case R.id.broadcast_configure_button:
			Log.i(TAG, "configure button clicked.");
			PopupMenu popCfg = new PopupMenu(uiContext, v);
			MenuInflater menuInft = popCfg.getMenuInflater();
			menuInft.inflate(R.menu.main, popCfg.getMenu());
			popCfg.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item) {
					return ((BroadcastActivity) getActivity())
							.onMainMenuItemClicked(item);
				}
			});

			((BroadcastActivity) getActivity()).modiMenuStatus(popCfg.getMenu());
			popCfg.show();
			break;
		}
	}

	private static final String ISPLAYED = "lstspk_isplayed";
	private static final String ATTIME = "lstspk_attime";
	private static final String BYNAME = "lstspk_byname";
	private static final String DURATION = "lstspk_duration";
	private static final String dfltPlHd = ">";
	private static final String unPyPlHd = "*";
	private static final String deacPlHd = "-";
	private static final String actiPlHd = " ";
	private PopupWindow popLastSpk = null;
	private int lastPlaying = -1;
	private List<Map<String, Object>> recsData = null;
	private SimpleAdapter plDatAdapter = null;

	private void showHistoryRecords(HistoryRecord[] hisRecs, int stype, int sid) {
		final int lssType = stype;
		final int lssId = sid;
		recsData = new ArrayList<Map<String, Object>>();
		if (hisRecs != null && hisRecs.length > 0)
			for (HistoryRecord hisR : hisRecs) {
				Map<String, Object> disR = new HashMap<String, Object>();
				disR.put(ISPLAYED, hisR.played ? dfltPlHd : unPyPlHd);
				disR.put(ATTIME, tag2time(hisR.tag));
				disR.put(BYNAME, API.uid2nick(hisR.owner));
				disR.put(DURATION, dur2str(hisR.duration));
				recsData.add(disR);
			}

		plDatAdapter = new SimpleAdapter(uiContext, recsData,
				R.layout.last_speaking_item, new String[] { ISPLAYED, ATTIME,
						BYNAME, DURATION }, new int[] { R.id.is_played,
						R.id.at_time, R.id.by_name, R.id.duration });

		LayoutInflater inflater = (LayoutInflater) uiContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View viewPLRs = inflater.inflate(R.layout.last_speaking_list, null);
		ListView lv_pl_list = (ListView) viewPLRs
				.findViewById(R.id.last_speakings);
		lv_pl_list.setAdapter(plDatAdapter);
		lv_pl_list.setOnItemClickListener(new ListView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> lvPLs, View view,
									int position, long id) {
				if (sessionApi != null) {
					if (lastPlaying < 0) {
						lastPlaying = (int) id + 1;
						HistoryRecord hisRec = talkHistory.getHistoryRecord(new CompactID(lssType, lssId).getCompactId(), lastPlaying);
						if (hisRec != null) {
							hisRec.mediaData = new byte[hisRec.duration / 2 * AMR475_PL_SIZE];
							talkHistory.readSpeechBuffer(new CompactID(lssType, lssId).getCompactId(), lastPlaying, hisRec.mediaData, 0);
							sessionApi.playLastSpeaking(selfId, hisRec.owner, hisRec.mediaData);
							recsData.get((int) id).put(ISPLAYED, actiPlHd);
							plDatAdapter.notifyDataSetChanged();
						}
					} else if (lastPlaying == (int) id) {
						lastPlaying = -1;
						sessionApi.stopPlayLastSpeaking(selfId);
						recsData.get((int) id).put(ISPLAYED, deacPlHd);
						plDatAdapter.notifyDataSetChanged();
					} else {
						recsData.get(lastPlaying).put(ISPLAYED, deacPlHd);
						lastPlaying = -1;
						sessionApi.stopPlayLastSpeaking(selfId);
						//
						uiHandler.postDelayed(new DelayPlayLast((int) id,
								lssType, lssId), 50);
					}
				}
			}
		});
		lastPlaying = -1;

		popLastSpk = new PopupWindow(viewPLRs, LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		popLastSpk.setBackgroundDrawable(getResources().getDrawable(
				R.drawable.w256h128));
		popLastSpk.setFocusable(true);
		popLastSpk.setTouchable(true);
		popLastSpk.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss() {
				Log.i(TAG, "关闭历史记录");
                // 显示广播信息
                getActivity().findViewById(R.id.broadcast_info).setVisibility(View.VISIBLE);

				popLastSpk = null;
				lastPlaying = -1;
				recsData = null;
				plDatAdapter = null;
			}
		});
		popLastSpk.showAsDropDown((View) broadcast_playlast_button, 0, 5);
	}

	private class DelayPlayLast implements Runnable {
		private int idx = 0;
		private int stype = Constant.SESSION_TYPE_NONE;
		private int sid = 0;

		public DelayPlayLast(int i, int st, int si) {
			idx = i;
			stype = st;
			sid = si;
		}

		@Override
		public void run() {
			recsData.get(idx).put(ISPLAYED, actiPlHd);
			plDatAdapter.notifyDataSetChanged();
			lastPlaying = (int) idx + 1;
			HistoryRecord hisRec = talkHistory.getHistoryRecord(new CompactID(stype, sid).getCompactId(), lastPlaying);
			if (hisRec != null) {
				hisRec.mediaData = new byte[hisRec.duration / 2 * AMR475_PL_SIZE];
				talkHistory.readSpeechBuffer(new CompactID(stype, sid).getCompactId(), lastPlaying, hisRec.mediaData, 0);
				sessionApi.playLastSpeaking(selfId, hisRec.owner, hisRec.mediaData);
			}
		}
	}

	private String tag2time(long tag) {
		Date today = new Date();
		Date date = new Date(100L * tag + 1407300000000L);
		java.text.SimpleDateFormat f = new java.text.SimpleDateFormat(
				"MM-dd HH:mm:ss", Locale.US); // ("yyyy-MM-dd hh:mm:ss");
		String str = f.format(date);
		if (f.format(today).substring(0, 5).equals(str.substring(0, 5))) {
			return getResources().getString(R.string.today) + str.substring(5);
		} else {
			return str;
		}
	}

	private String dur2str(int d10ms) {
		double sec = (double) d10ms * 0.01;
		java.text.DecimalFormat df = new java.text.DecimalFormat("###.#");
		return df.format(sec);
	}

	private void asyncSetUiItem(int id, int state) {
		if (uiHandler != null)
			uiHandler.post(new UiItem(id, state));
	}

	private void asyncSetUiItems(int[] ids, int[] states) {
		if (uiHandler != null)
			uiHandler.post(new UiItem(ids, states));
	}

	private class UiItem implements Runnable {
		private int[] rids;
		private int[] newStates;

		public UiItem(int i, int s) {
			rids = new int[1];
			newStates = new int[1];
			rids[0] = i;
			newStates[0] = s;
		}

		public UiItem(int[] ids, int[] sts) {
			rids = ids;
			newStates = sts;
		}

		@Override
		public void run() {
			synchronized (TalkFragment.this) {
				for (int i = 0; i < rids.length; i++)
					doUpdateUI(rids[i], newStates[i]);
			}
		}

		private void doUpdateUI(int id, int state) {
			switch (id) {
			case R.id.broadcast_ptt_button:
				broadcast_ptt_button.setSelected(state == TRUE);
				broadcast_ptt_mic_ind.setSelected(state == TRUE);
				broadcast_ptt_spk_ind.setSelected(state == TRUE);
				break;
			case R.id.broadcast_ptt_mic_ind:
				broadcast_ptt_mic_ind.setPressed(state >= TRUE);
				if (state > TRUE)
					broadcast_ptt_mic_ind.setColorFilter(0x44FF0000);
				else
					broadcast_ptt_mic_ind.setColorFilter(null);
				break;
			case R.id.broadcast_ptt_spk_ind:
				broadcast_ptt_spk_ind.setPressed(state == TRUE);
				break;
			case R.id.record_ind:
				if (state == FALSE) {
					shiftHisPlayers("< me");
				}
				record_ind.setVisibility(state == TRUE ? View.VISIBLE
						: View.INVISIBLE);
				break;
			case R.id.player_ind:
				if (state == 0) {
					shiftHisPlayers("> " + player_ind.getText().toString());
				}
				player_ind.setVisibility(state == 0 ? View.INVISIBLE
						: View.VISIBLE);
				player_ind.setText(API.uid2nick(state));
				break;
			case R.id.broadcast_level_ind:
				broadcast_level_ind.getDrawable().setLevel(state);
				break;
			case R.id.broadcast_playlast_button:
				if (state == PLS.LASTSPEAKING_GONE) {
					broadcast_playlast_button.setVisibility(View.INVISIBLE);
				} else if (state == PLS.LASTSPEAKING_NEW) {
					broadcast_playlast_button.setVisibility(View.VISIBLE);
					broadcast_playlast_button.setSelected(true);
					broadcast_playlast_button.setActivated(false);
				} else if (state == PLS.LASTSPEAKING_PLAYING) {
					broadcast_playlast_button.setSelected(false);
					broadcast_playlast_button.setActivated(true);
					broadcast_ptt_spk_ind.setPressed(true);
				} else if (state == PLS.LASTSPEAKING_END) {
					broadcast_playlast_button.setSelected(false);
					broadcast_playlast_button.setActivated(false);
					broadcast_ptt_spk_ind.setPressed(false);
				}
				break;
			case R.id.broadcast_speaker_ind:
				if (state == 0) {
					speaker_item.setText("");
					speaker_item.setVisibility(View.INVISIBLE);
					// broadcast_ptt_spk_ind.setPressed(false);
				} else {
					speaker_item.setText(API.uid2nick(state));
					speaker_item.setVisibility(View.VISIBLE);
					// broadcast_ptt_spk_ind.setPressed(true);
				}
				break;
			case R.id.broadcast_queuer_ind:
				// Log.d(TAG, "broadcast_queuer_ind state: "+state);
				if (state == 0) {
					queuer_item.setText("");
					queuer_item.setVisibility(View.INVISIBLE);
				} else if (state > 0) {
					add2WaitList(state);
					queuer_item.setText(API.uid2nick(state));
					queuer_item.setVisibility(View.VISIBLE);
				} else {
					int iq = rmFromWaitQueue(-1 * state);
					// Log.d(TAG, "rmFromWaitQueue ret: "+iq);
					if (iq > 0) {
						queuer_item.setText(API.uid2nick(iq));
						queuer_item.setVisibility(View.VISIBLE);
					} else if (iq == 0) {
						queuer_item.setText("");
						queuer_item.setVisibility(View.INVISIBLE);
					}
				}
				break;
			case R.id.output_select_button:
				output_ind.getDrawable().setLevel(state);
				break;
			case R.id.handset_battery_level:
				if (state > 0) {
					// hs_battery_ind.setText("[ "+Integer.toString(25*(state-1))+" % ]");
					hs_battery_ind.getDrawable().setLevel(state - 1);
					hs_battery_ind.setVisibility(View.VISIBLE);
				} else {
					hs_battery_ind.setVisibility(View.INVISIBLE);
				}
				break;
			case R.id.broadcast_me_item:
				Drawable dUndist = uiContext.getResources().getDrawable(
						R.drawable.member_normal);
				if (state == 1) {
					dUndist = uiContext.getResources().getDrawable(
							R.drawable.member_undistube);
				} else if (state == 3) {
					dUndist = uiContext.getResources().getDrawable(
							R.drawable.member_bound);
				}
				broadcast_me_item.setCompoundDrawablesWithIntrinsicBounds(dUndist, null,
						null, null);
				break;
			}
		}

		private void add2WaitList(int uid) {
			if (waitList.contains(uid)) {
				waitList.remove((Object) uid);
			}
			int qsz = waitList.size();
			waitList.add(qsz, uid);
			return;
		}

		private int rmFromWaitQueue(int uid) {
			int qsz = waitList.size();

			if (qsz > 0) {
				for (int i = 0; i < qsz; i++) {
					int id = waitList.get(i);
					if (id == uid) {
						waitList.remove(i);
						if (qsz == 1) {
							return 0;
						} else if (i == qsz - 1) {
							return waitList.get(i - 1);
						} else {
							return -1;
						}
					}
				}
				return -1;
			}
			return 0;
		}

		private void shiftHisPlayers(String info) {
			if (hisPlys.get(2).getVisibility() == View.VISIBLE) {
				hisPlys.get(2).setText(hisPlys.get(1).getText().toString());
				hisPlys.get(1).setText(hisPlys.get(0).getText().toString());
				hisPlys.get(0).setText(info);
			} else if (hisPlys.get(1).getVisibility() == View.VISIBLE) {
				hisPlys.get(2).setVisibility(View.VISIBLE);
				hisPlys.get(2).setText(hisPlys.get(1).getText().toString());
				hisPlys.get(1).setText(hisPlys.get(0).getText().toString());
				hisPlys.get(0).setText(info);
			} else if (hisPlys.get(0).getVisibility() == View.VISIBLE) {
				hisPlys.get(1).setVisibility(View.VISIBLE);
				hisPlys.get(1).setText(hisPlys.get(0).getText().toString());
				hisPlys.get(0).setText(info);
			} else {
				hisPlys.get(0).setVisibility(View.VISIBLE);
				hisPlys.get(0).setText(info);
			}
		}
	}
}