package com.hhuc.sillyboys.tuling.tl_demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.PopupWindow.OnDismissListener;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.algebra.sdk.API;
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
import com.algebra.sdk.entity.Room;
import com.algebra.sdk.entity.Session;
import com.hhuc.sillyboys.tuling.LoginActivity;
import com.hhuc.sillyboys.tuling.broadcast.BroadcastActivity;
import com.hhuc.sillyboys.tuling.broadcast.ShareDialog;
import com.hhuc.sillyboys.tuling.entity.ChannelExt;
import com.hhuc.sillyboys.tuling.entity.ContactExt;
import com.hhuc.sillyboys.tuling.entity.MsgCode;
import com.hhuc.sillyboys.tuling.entity.ObservableHoriScrollView;
import com.hhuc.sillyboys.tuling.entity.ScrollViewListener;
import com.hhuc.sillyboys.tuling.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelFragment extends Fragment implements OnChannelListener, OnSessionListener {
	public static final String TAG = "fragment.channel";
	private Context uiContext = null;
	private Handler uiHandler = null;
	private View mView = null;

	private boolean simpleChannelMode = false;
	private boolean fragmentForeground = false;

	private ChannelApi channelApi = null;
	private SessionApi sessionApi = null;
	private DeviceApi deviceApi = null;

	private ArrayList<ChannelExt> myChannels = null;
	private ArrayList<ChannelExt> tlChannels = null;
	private static List<IntStr> friends = new ArrayList<IntStr>();

	private int selfId;
	private boolean selfOnline = true;
	private boolean isVisitor = true;
	private Channel defaultCh = null;
	private CompactID currSession = null;
	private List<IntStr> sessPresences = null;
	private SharedPreferences pref;


	@Override
	public void onAttach(Activity act) {
		super.onAttach(act);
		uiContext = act;
		Log.i(TAG, "onAttach ...");
	}

	@SuppressLint("HandlerLeak")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		selfId = getArguments().getInt("id.self");
		pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		selfId = pref.getInt("selfid", 0);

		selfOnline = (getArguments().getInt("state.self") == Constant.CONTACT_STATE_ONLINE);
		isVisitor = getArguments().getBoolean("visitor.self");

		Log.i(TAG, "onCreate with uid " + selfId + " user online:" + selfOnline);

	}

	@Override
	public void onResume() {
		super.onResume();
		Log.i(TAG, "onResume ...");

		fragmentForeground = true;

		uiHandler = BroadcastActivity.getUiHandler();
		if (channelApi == null) {
			// 修改demo，发生异常直接返回登录
			try{
				uiHandler.postDelayed(delayInitApi, 100);
			}catch(Exception e){
				e.printStackTrace();
//				startActivity(new Intent(getActivity(), LoginActivity.class));
			}
		} else {
			resumeChannelFragment();
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		Log.i(TAG, "onStop ...");
		fragmentForeground = false;
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
	}

	@Override
	public void onSaveInstanceState(Bundle outBu) {
		super.onSaveInstanceState(outBu);
		Log.i(TAG, "onSaveInstanceState ....");

		outBu.putString("StopByAndroid", "yes");
	}

	private Runnable delayInitApi = new Runnable() {
		@Override
		public void run() {
			channelApi = API.getChannelApi();
			sessionApi = API.getSessionApi();
			if (channelApi != null && sessionApi != null) {
				channelApi.setOnChannelListener(ChannelFragment.this);
				sessionApi.setOnSessionListener(ChannelFragment.this);
				deviceApi = API.getDeviceApi();

				resumeChannelFragment();
			} else {
				uiHandler.postDelayed(delayInitApi, 300);
			}
		}
	};

	private void resumeChannelFragment() {
		if (myChannels == null) {
			funcGetAdverChannels();
		} else {
			asyncUpdateChannelUI();
			asyncInitPopupMemberList();
		}
	}

	private void add2Friends(List<Contact> membs) {
		IntStr ris = null;
		for (Contact m1 : membs) {
			ris = null;
			for (IntStr is1 : friends) {
				if (is1.i == m1.id) {
					ris = is1;
					break;
				}
			}
			if (ris != null)
				friends.remove(ris);
			friends.add(new IntStr(m1.id, m1.name));
		}
	}

	private ObservableHoriScrollView myChannelsScrollor = null;
	private int channelBackgroundImgWidth = 0;
	private int lastDisplayChannelIdx = 0;
	private TextView tvInitCh = null;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container != null) { // && ((TLActivity)uiContext).isHorizonScreen()
			mView = inflater.inflate(R.layout.fragment_channel, container, false);

			TextView broadcastName = (TextView) mView.findViewById(R.id.broadcast_name);
			String name = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString("broadcastname", "");
			Log.d(TAG, "cname:" + name);
			if(name == null || name.isEmpty()){
				name = "途聆体验频道";
			}else if(name.equals("ECHO")){
				name = "XX大学广播站";
			}
			broadcastName.setText(name);
//			getActivity().findViewById(R.id.tl_channel_fragment).setVisibility(View.INVISIBLE);

			// 分享按钮
			// http://182.254.220.217/Weixin/erweicode.png
			ImageView shareButton = (ImageView) mView.findViewById(R.id.broadcast_share);
			shareButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					Log.d(TAG, "分享二维码");
//					getActivity().findViewById(R.id.share_img).setVisibility(View.VISIBLE);
//					getActivity().findViewById(R.id.share_back).setEnabled(true);
					ShareDialog dialog = new ShareDialog(getActivity());
					dialog.getWindow().setBackgroundDrawable(new ColorDrawable());
					dialog.show();
				}
			});
			// 关闭分享
//			ImageView shareBack = (ImageView)getActivity().findViewById(R.id.share_back);
//			shareBack.setOnClickListener(new OnClickListener() {
//				@Override
//				public void onClick(View view) {
//					Log.d(TAG, "关闭二维码");
//					getActivity().findViewById(R.id.share_img).setVisibility(View.INVISIBLE);
//					getActivity().findViewById(R.id.share_back).setEnabled(false);
//				}
//			});




			ImageView addButton = (ImageView) mView.findViewById(R.id.broadcast_channel_config);
			if (!isVisitor) {
				addButton.setOnClickListener(new OnChannelConfigButtonPressed());
			} else {
				addButton.setBackgroundResource(R.drawable.talk_menu_white);
				addButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						Toast.makeText(uiContext, "not auth for Visitors", Toast.LENGTH_SHORT).show();
					}
				});
			}

			// 频道列表
			LinearLayout llChs = (LinearLayout) mView.findViewById(R.id.channel_list);
			tvInitCh = new TextView(uiContext);
			tvInitCh.setText(getResources().getString(R.string.init_channel));
			tvInitCh.setTextColor(colorActivedChannel);
			tvInitCh.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.channel_name_size));
			tvInitCh.setGravity(Gravity.CENTER_HORIZONTAL);

			ImageView chBg = (ImageView) mView.findViewById(R.id.channel_bimg);
			chBg.measure(ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT);
			channelBackgroundImgWidth = chBg.getMeasuredWidth();
			// int wpx = (int)
			// getResources().getDimension(R.dimen.channel_name_width);
			llChs.addView(tvInitCh, new ViewGroup.LayoutParams(channelBackgroundImgWidth, ViewGroup.LayoutParams.MATCH_PARENT));

			llChs.setLongClickable(true);
			llChs.setOnTouchListener(new OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					int action = event.getAction();
					Log.i(TAG, "llChs.setOnTouchListener " + action);
					TextView tvShiftHint = (TextView) mView.findViewById(R.id.channels_scroll_hint);
					tvShiftHint.setText(R.string.text_shift_hint);
					tvShiftHint.setTextColor(0x88AAAAAA);
					if (tvShiftHint != null) {
						if (action == MotionEvent.ACTION_DOWN) {
							tvShiftHint.setVisibility(View.VISIBLE);
						} else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
							tvShiftHint.setVisibility(View.INVISIBLE);
						}
					}
					return false;
				}
			});

			myChannelsScrollor = (ObservableHoriScrollView) mView
					.findViewById(R.id.channels_scroll);
			myChannelsScrollor.setScrollViewListener(new ScrollViewListener() {
				@Override
				public void onScrollChanged(FrameLayout scrollView, int x,
											int y, int oldx, int oldy) {
					stopChannelsShiftTimer();
					ObservableHoriScrollView view = (ObservableHoriScrollView) scrollView;
					int dispIdx = view.getTargetIdx();
					if (dispIdx != lastDisplayChannelIdx) {
						lastDisplayChannelIdx = dispIdx;
						ChannelExt che1 = myChannels.get(dispIdx);
						uiHandler.obtainMessage(MsgCode.MC_ONDISPLAYCHANGED,
								che1.cid.getType(), che1.cid.getId())
								.sendToTarget();
					}
				}

				@Override
				public void onScrollStopped(FrameLayout scrollView, int x, int y) {
					ObservableHoriScrollView view = (ObservableHoriScrollView) scrollView;
					int stopIdx = view.getTargetIdx();
					showPresencesCount(stopIdx);
					int currIdx = getCurrentDispChIdx();
					if (stopIdx != currIdx) {
						startChannelsShiftTimer();
					}
				}
			});
			myChannelsScrollor.start(channelBackgroundImgWidth);

			ImageView ivSessionPower = (ImageView) mView
					.findViewById(R.id.broadcast_session_power);
			ivSessionPower.setPressed(false);
			ivSessionPower.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View ivPwr) {
					onSessionPowerClicked(ivPwr.isSelected(), ivPwr.isActivated(), myChannelsScrollor.getTargetIdx());
				}
			});
			ivSessionPower.setOnLongClickListener(new OnLongClickListener() {
				@Override
				public boolean onLongClick(View ivPwr) {
					onSessionPowerLongClicked(ivPwr.isSelected(), ivPwr.isActivated(), myChannelsScrollor.getTargetIdx());
					return true;
				}
			});

			Log.i(TAG, "onCreateView OK ...");
			return mView;
		} else {
			Log.i(TAG, "onCreateView IGNORE ... null-container:"
					+ (container == null));
			return null;
		}
	}

	private void updateChannelsHint(final int count) {
		((BroadcastActivity)uiContext).runOnUiThread(new Runnable(){
			@Override
			public void run() {
				if (tvInitCh != null)
					tvInitCh.setText(getResources().getString(R.string.init_channel_end) + count);
			}});
	}
	
	private List<Integer> dialogParties = null;
	private void showDialogHint(final int dialog, int owner, List<Integer> presences) {
		myChannelsScrollor = (ObservableHoriScrollView) mView.findViewById(R.id.channels_scroll);
		myChannelsScrollor.setVisibility(View.INVISIBLE);
		TextView tvShiftHint = (TextView) mView.findViewById(R.id.channels_scroll_hint);
		tvShiftHint.setTextColor(0x88EEEEEE);
		tvShiftHint.setVisibility(View.VISIBLE);
		
		dialogParties = new ArrayList<Integer>();
		String members = API.uid2nick(owner);
		dialogParties.add(owner);
		for (Integer m1 : presences) if (m1 != owner) {
				members += ", " + API.uid2nick(m1);
				dialogParties.add(m1);
		}
		tvShiftHint.setText(members);
		tvShiftHint.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (dialogParties.size() > 1) {
					int first = dialogParties.get(0);
					dialogParties.remove(0);
					dialogParties.add((Integer)first);
					String newDisp = API.uid2nick(dialogParties.get(0));
					for (int j=1; j<dialogParties.size();j++)
						newDisp += ", " + API.uid2nick(dialogParties.get(j));
					((TextView)v).setText(newDisp);
				}
			}
		});
	}

	private void updateDialogHint(boolean add, int dialog, List<Integer> ids) {
		if (dialogParties == null) 
			return;
		
		if (add) {
			for (Integer m1 : ids) dialogParties.add((Integer)m1);
		} else {
			for (Integer m1 : ids) dialogParties.remove((Integer)m1);
		}
		
		TextView tvShiftHint = (TextView) mView.findViewById(R.id.channels_scroll_hint);
		if (dialogParties.size() > 0) {
			String members = API.uid2nick(dialogParties.get(0));
			for (int i = 1; i < dialogParties.size(); i++) {
				members += ", " + API.uid2nick(dialogParties.get(i));
			}
			tvShiftHint.setText(members);
		} else {
			tvShiftHint.setText("----");
		}
	}

	private void closeDialogHint(int dialog) {
		myChannelsScrollor = (ObservableHoriScrollView) mView.findViewById(R.id.channels_scroll);
		myChannelsScrollor.setVisibility(View.VISIBLE);
		TextView tvShiftHint = (TextView) mView.findViewById(R.id.channels_scroll_hint);
		tvShiftHint.setVisibility(View.INVISIBLE);
		dialogParties = null;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Log.i(TAG, "onDestroyView ... ");
	}

	/*
     * 
     * 
     * 
     */

	private void asyncUpdateChannelUI() {
		if (fragmentForeground && uiHandler != null)
			uiHandler.post(new AsyncUpdateChannelState());
	}

	private class AsyncUpdateChannelState implements Runnable {
		@Override
		public void run() {
			LinearLayout llChs = (LinearLayout) mView.findViewById(R.id.channel_list);
			llChs.removeAllViews();
			if (myChannels == null || myChannels.size() == 0 || mView == null) {
				Log.e(TAG, "channelList invalid. is null:" + (myChannels == null));
				if (tvInitCh != null) {
					tvInitCh.setText(getResources().getString(R.string.init_channel_end) + 0);
					llChs.addView(tvInitCh, new ViewGroup.LayoutParams(channelBackgroundImgWidth, ViewGroup.LayoutParams.MATCH_PARENT));
				}
				return;
			}

			for (ChannelExt che1 : myChannels) {
				TextView tvNewCh = new TextView(uiContext);
				tvNewCh.setText(che1.name);
				if (che1.isCurrent)
					tvNewCh.setTextColor(colorActivedChannel);
				else
					tvNewCh.setTextColor(colorPassiveChannel);
				tvNewCh.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.channel_name_size));
				tvNewCh.setGravity(Gravity.CENTER_HORIZONTAL);
				tvNewCh.setSingleLine();
				tvNewCh.setEllipsize(android.text.TextUtils.TruncateAt.END);

				// int wpx = (int)
				// getResources().getDimension(R.dimen.channel_name_width);
				llChs.addView(tvNewCh, new ViewGroup.LayoutParams(channelBackgroundImgWidth, ViewGroup.LayoutParams.MATCH_PARENT));
			}

			int currChIdx = getCurrentDispChIdx();
			myChannelsScrollor.setTargetIdx(currChIdx);
			showPresencesCount(currChIdx);
		}
	}

	private void showPresencesCount(int idx) {
		if (myChannels != null && myChannels.size() > idx) {
			ChannelExt che1 = myChannels.get(idx);
			TextView memberButton = (TextView) mView
					.findViewById(R.id.broadcast_presences);
			memberButton.setText("" + che1.presenceCount);
			ImageView ivPower = (ImageView) mView
					.findViewById(R.id.broadcast_session_power);
			if (che1.isCurrent) {
				memberButton.setBackgroundResource(R.drawable.members_actived);
				memberButton
						.setOnClickListener(new OnChannelMemberButtonPressed());
				ivPower.setSelected(true);
			} else {
				memberButton
						.setBackgroundResource(R.drawable.members_deactived);
				memberButton.setOnClickListener(null);
				ivPower.setSelected(false);
			}
		}
	}

	private boolean channelSelectIsBusy = false;

	private void sessionChanging(boolean y) {
		channelSelectIsBusy = y;
		ImageView ivPower = (ImageView) mView.findViewById(R.id.broadcast_session_power);
		ivPower.setActivated(y);
	}

	public void onSessionActive(int ctype, int cid) {
		asyncUpdateChannelUI();
	}

	private static final int CH_SEL_TIMER = 30000;

	private void onSessionPowerClicked(boolean wasOn, boolean act, int idx) {
		if (myChannels == null || (myChannels != null && myChannels.size() == 0))
			return;
		if (!wasOn && !channelSelectIsBusy && selfOnline && currSession == null) {
			stopChannelsShiftTimer();

			ChannelExt che1 = myChannels.get(idx);
			uiEnterChannel(che1);
		} else if (wasOn && dialogParties != null) {
			((BroadcastActivity) uiContext).stopDialogSession(selfId, 0);
		} else {
			if (!selfOnline)
				Toast.makeText(uiContext, "is offline !", Toast.LENGTH_SHORT).show();
			if (channelSelectIsBusy)
				Toast.makeText(uiContext, "exit UI and try again", Toast.LENGTH_SHORT).show();
		}
	}

	private void onSessionPowerLongClicked(boolean wasOn, boolean act, int idx) {
		if (myChannels == null || (myChannels != null && myChannels.size() == 0))
			return;
		if (!act) {
			if (!channelSelectIsBusy && selfOnline) {
				stopChannelsShiftTimer();

				ChannelExt che1 = myChannels.get(idx);
				if (che1.isCurrent) {
					sessionChanging(true);
					sessionApi.sessionBye(selfId, che1.cid.getType(), che1.cid.getId());
				} else {
					uiEnterChannel(che1);
				}
			} else {
				if (!selfOnline)
					Toast.makeText(uiContext, getResources().getString(R.string.offline_hint), Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(uiContext, getResources().getString(R.string.exit_ui_hint), Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void uiEnterChannel(ChannelExt che1) {
		if (che1.cid.getType() != Constant.SESSION_TYPE_TOURLINK) {
			sessionChanging(true);
			sessionApi.sessionCall(selfId, che1.cid.getType(), che1.cid.getId());
			if (currSession != null
					&& currSession.getType() == Constant.SESSION_TYPE_TOURLINK) {
				int curChIdx = findCurrentChannel();
				ChannelExt che2 = myChannels.get(curChIdx);
				che2.isCurrent = false;
				che2.cid = new CompactID(Constant.SESSION_TYPE_TOURLINK, 0);
				che2.name = makeTourlinkChName(0);
			}
		} else {
			((BroadcastActivity) uiContext).showTourlinkChannelScroller(tlChannels
					.size());
		}
	}
	public void uiEnterChannel(int ctype, int cid) {
		if (ctype != Constant.SESSION_TYPE_TOURLINK) {
			sessionChanging(true);
			sessionApi.sessionCall(selfId, ctype, cid);
			if (currSession != null && currSession.getType() == Constant.SESSION_TYPE_TOURLINK) {
				int curChIdx = findCurrentChannel();
				ChannelExt che2 = myChannels.get(curChIdx);
				che2.isCurrent = false;
				che2.cid = new CompactID(Constant.SESSION_TYPE_TOURLINK, 0);
				che2.name = makeTourlinkChName(0);
			}
		} else {
			((BroadcastActivity) uiContext).showTourlinkChannelScroller(tlChannels.size());
		}
	}

	public void onTourlinkChannelScrollerClosed(int sele) {
		if (sele < 0 || sele > tlChannels.size()) { // cancel
			asyncUpdateChannelUI();
		} else {
			sessionChanging(true);
			ChannelExt che1 = tlChannels.get(sele);
			sessionApi
					.sessionCall(selfId, che1.cid.getType(), che1.cid.getId());
			uiHandler.obtainMessage(MsgCode.MC_ONDISPLAYCHANGED,
					che1.cid.getType(), che1.cid.getId()).sendToTarget();
		}
	}

	private void startChannelsShiftTimer() {
		if (shiftToCurrent == null && CH_SEL_TIMER != 0) {
			// Log.d(TAG, "startChannelsShiftTimer()");
			shiftToCurrent = new DisplayShiftToCurrent();
			uiHandler.postDelayed(shiftToCurrent, CH_SEL_TIMER);
		}
	}

	private void stopChannelsShiftTimer() {
		if (shiftToCurrent != null) {
			// Log.d(TAG, "stopChannelsShiftTimer()");
			uiHandler.removeCallbacks(shiftToCurrent);
			shiftToCurrent = null;
		}
	}

	private static final int colorActivedChannel = 0xFFE0E0E0;
	private static final int colorPassiveChannel = 0xFF808080;
	private DisplayShiftToCurrent shiftToCurrent = null;

	private class DisplayShiftToCurrent implements Runnable {
		@Override
		public void run() {
			ObservableHoriScrollView channelsScroll = (ObservableHoriScrollView) mView
					.findViewById(R.id.channels_scroll);
			channelsScroll.setTargetIdx(getCurrentDispChIdx());
		}
	}

	private int findCurrentChannel() {
		int i = 0;
		boolean found = false;

		for (; i < myChannels.size(); i++) {
			ChannelExt che1 = myChannels.get(i);
			if (che1.isCurrent) {
				found = true;
				break;
			}
		}
		if (found)
			return i;
		else
			return -1;
	}

	private int findHomeChannel() {
		int i = 0;
		boolean found = false;

		if (myChannels == null)
			return -1;

		for (; i < myChannels.size(); i++) {
			ChannelExt che1 = myChannels.get(i);
			if (che1.isHome) {
				found = true;
				break;
			}
		}
		if (found)
			return i;
		else
			return -1;
	}

	public class ChMemberInfo {
		public int id;
		public String name;
		public int state;
		public boolean presence = false;
		public boolean selected = false;

		public ChMemberInfo(ContactExt ce1) {
			this.id = ce1.id;
			this.name = ce1.name != null ? new String(ce1.name) : "???";
			this.state = ce1.state;
		}

		public ChMemberInfo(int i, String s) {
			this.id = i;
			this.name = s != null ? new String(s) : "???";
			this.state = Constant.CONTACT_STATE_ONLINE;
		}
	}

	private static final int SELE_TYPE_NONE = 0;
	public static final int SELE_TYPE_CALL = 1;
	public static final int SELE_TYPE_SUMMON = 2;
	private static final String USERSELE = "grid_user_selected";
	private static final String USERICON = "grid_user_icon";
	private static final String USERNAME = "grid_user_name";
	private PopupWindow popMembers = null;
	private List<ChMemberInfo> channelMembers = null;
	private int selectedType = SELE_TYPE_NONE;
	private int selectedCount = 0;

	private void initPopupMemberList() {
		GridView gv_member_list = null;
		boolean isShowing = false;
		if (popMembers != null && popMembers.isShowing()) {
			for (ChMemberInfo chm1 : channelMembers) {
				chm1.selected = false;
			}
			popMembers.dismiss();
			popMembers = null;
			isShowing = true;
		}

		List<ContactExt> sessionMembers = getCurrSessionMembers();  // no current sessioon will return null
		if (sessionMembers != null) {
			channelMembers = new ArrayList<ChMemberInfo>();
			for (ContactExt ce1 : sessionMembers) {
				ChMemberInfo chm1 = new ChMemberInfo(ce1);
				channelMembers.add(chm1);
			}
		}
		if (sessPresences != null) {
			if (channelMembers == null)
				channelMembers = new ArrayList<ChMemberInfo>();
			for (IntStr is1 : sessPresences) {
				boolean has = false;
				for (ChMemberInfo chm2 : channelMembers) {
					if (is1.i == chm2.id) {
						has = true;
						chm2.presence = true;
						if (chm2.state == Constant.CONTACT_STATE_OFFLINE)	// new member (by focus) may be un-sync state
							chm2.state = Constant.CONTACT_STATE_ONLINE;
						break;
					}
				}
				if (!has) {
					ChMemberInfo chm1 = new ChMemberInfo(is1.i, is1.s);
					chm1.state = is1.b ? Constant.CONTACT_STATE_BUSY : Constant.CONTACT_STATE_ONLINE;
					chm1.presence = true;
					channelMembers.add(chm1);
				}
			}
		}

		if (channelMembers != null) {
			List<Map<String, Object>> membsData = new ArrayList<Map<String, Object>>();
			for (ChMemberInfo chm1 : channelMembers) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put(USERNAME, chm1.name);
				map.put(USERICON, getUserIconDrawable(chm1));
				map.put(USERSELE, R.drawable.sele_unava);
				membsData.add(map);
			}

			SimpleAdapter adapter = new SimpleAdapter(uiContext, membsData,
					R.layout.member_item, new String[] { USERICON, USERNAME,
							USERSELE }, new int[] { R.id.member_icon,
							R.id.member_name, R.id.member_sele });

			LayoutInflater inflater = (LayoutInflater) uiContext
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View viewMembs = inflater.inflate(R.layout.member_list, null);
			gv_member_list = (GridView) viewMembs
					.findViewById(R.id.member_grid);
			gv_member_list.setAdapter(adapter);
			selectedType = SELE_TYPE_NONE;
			selectedCount = 0;
			gv_member_list
					.setOnItemClickListener(new GridView.OnItemClickListener() {
						@Override
						public void onItemClick(AdapterView<?> aView,
												View view, int pos, long row) {
							if (currSession.getType() != Constant.SESSION_TYPE_CHANNEL) {
								Toast.makeText(uiContext, "null operation.",
										Toast.LENGTH_SHORT).show();
								return;
							} else {
								ImageView vsele = (ImageView) view
										.findViewById(R.id.member_sele);
								ChMemberInfo chm1 = channelMembers
										.get((int) row);
								if (!chm1.selected) {
									if (chm1.id != selfId
											&& (chm1.state == Constant.CONTACT_STATE_ONLINE || chm1.state == Constant.CONTACT_STATE_BUSY)) {
										int newSeleType = chm1.presence ? SELE_TYPE_CALL
												: SELE_TYPE_SUMMON;
										if (selectedType == SELE_TYPE_NONE
												|| selectedType == newSeleType) {
											selectedCount++;
											selectedType = newSeleType;
											chm1.selected = true;
											vsele.setImageDrawable(getResources()
													.getDrawable(
															R.drawable.sele_checked));
											Toast.makeText(
													uiContext,
													(selectedType == SELE_TYPE_CALL) ? getResources().getString(R.string.member_dialog)
															: getResources().getString(R.string.member_sms),
													Toast.LENGTH_SHORT).show();
										} else {
											Toast.makeText(uiContext, getResources().getString(R.string.member_select_hint),
													Toast.LENGTH_SHORT).show();
										}
									} else {
										chm1.selected = false;
										vsele.setImageDrawable(getResources()
												.getDrawable(
														R.drawable.sele_unava));
									}
								} else {
									chm1.selected = false;
									vsele.setImageDrawable(getResources()
											.getDrawable(R.drawable.sele_unava));
									if (--selectedCount == 0)
										selectedType = SELE_TYPE_NONE;
								}
							}
						}
					});

			popMembers = new PopupWindow(viewMembs, LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);
			popMembers.setBackgroundDrawable(getResources().getDrawable(
					R.drawable.w256h128));
			// popMembers.setOutsideTouchable(true);
			// popMembers.update();
			popMembers.setTouchable(true);
			popMembers.setFocusable(true);
			popMembers.setOnDismissListener(new OnDismissListener() {
				@Override
				public void onDismiss() {
					List<Integer> selects = new ArrayList<Integer>();
					for (ChMemberInfo chm1 : channelMembers)
						if (chm1.selected)
							selects.add((Integer) chm1.id);
					if (selects.size() > 0) {
						// Log.d(TAG,
						// "popWin onDismiss "+selects.size()+" members selected");
						int[] sMembs = new int[selects.size()];
						for (int i = 0; i < selects.size(); i++)
							sMembs[i] = selects.get(i);
						uiHandler.obtainMessage(MsgCode.MC_SELECTEDMEMBERS,
								currSession.getCompactId(), selectedType,
								sMembs).sendToTarget();
						selectedType = SELE_TYPE_NONE;
						selectedCount = 0;
					}
				}
			});
		} else {
			popMembers = null;
			Log.e(TAG, "allMembers is null");
		}

		if (isShowing && popMembers != null) {
			for (ChMemberInfo chm1 : channelMembers)
				chm1.selected = false;
			TextView memberButton = (TextView) mView
					.findViewById(R.id.broadcast_presences);
			popMembers.showAsDropDown((View) memberButton, 0, 5);
		}

		return;
	}

	private List<ContactExt> getCurrSessionMembers() {
		if (myChannels != null)
			for (ChannelExt che1 : myChannels) {
				if (Channel.sameCid(che1.cid, currSession)) {
					return che1.members;
				}
			}
		return null;
	}

	private int getUserIconDrawable(ChMemberInfo chm1) {
		if (chm1.id == selfId)
			return R.drawable.user_self;

		switch (chm1.state) {
		case Constant.CONTACT_STATE_ONLINE:
			if (chm1.presence)
				return R.drawable.user_presence;
			else
				return R.drawable.user_online;
		case Constant.CONTACT_STATE_BUSY:
			return R.drawable.user_busy;
		case Constant.CONTACT_STATE_OFFLINE:
			return R.drawable.user_offline;
		default:
			return R.drawable.user_error;
		}
	}

	private void asyncInitPopupMemberList() {
		if (uiHandler != null) {
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					initPopupMemberList();
				}
			});
		} else {
			Log.e(TAG, "init popupMemberList canceled. uiHandler is null: "
					+ (uiHandler == null));
		}
	}

	/*
	 * 
	 * Fragment - Activity Interfaces
	 */

	public List<Channel> getDefaultChannelCandidates(int uid) {
		List<Channel> allChs = new ArrayList<Channel>();
		if (myChannels != null)
			for (int i = 0; i < myChannels.size(); i++) {
				ChannelExt che1 = myChannels.get(i);
				if (che1.cid.getType() != Constant.SESSION_TYPE_CHANNEL)
					continue;

				Channel ch1 = new Channel(che1.cid.getType(), che1.cid.getId(),
						che1.name);
				ch1.isHome = che1.isHome;
				allChs.add(ch1);
			}
		return allChs;
	}

	private List<Channel> getDeleteCandidates() {
		List<Channel> allChs = new ArrayList<Channel>();
		if (myChannels != null)
			for (int i = 0; i < myChannels.size(); i++) {
				ChannelExt che1 = myChannels.get(i);
				if (che1.cid.getType() != Constant.SESSION_TYPE_CHANNEL)
					continue;

				Channel ch1 = new Channel(che1.cid.getType(), che1.cid.getId(),
						che1.name);
				ch1.isHome = che1.isHome;
				ch1.needPassword = che1.needPassword;
				ch1.owner = new IntStr(che1.owner.i, che1.owner.s);
				allChs.add(ch1);
			}
		return allChs;
	}

	private List<Channel> getModifyCandidates() {
		List<Channel> allChs = new ArrayList<Channel>();
		if (myChannels != null)
			for (int i = 0; i < myChannels.size(); i++) {
				ChannelExt che1 = myChannels.get(i);
				if (che1.cid.getType() != Constant.SESSION_TYPE_CHANNEL)
					continue;
				if (che1.owner.i != selfId)
					continue;

				Channel ch1 = new Channel(che1.cid.getType(), che1.cid.getId(),
						che1.name);
				ch1.isHome = che1.isHome;
				ch1.needPassword = che1.needPassword;
				ch1.owner = null;
				allChs.add(ch1);
			}
		return allChs;
	}

	public int getTLChPresencesCount(int idx) {
		if (tlChannels != null && tlChannels.size() > idx) {
			ChannelExt che1 = tlChannels.get(idx);
			return che1.presenceCount;
		}
		return 0;
	}

	public void setTLChPresencesCount(int idx, int pCount) {
		if (tlChannels != null && tlChannels.size() > idx) {
			ChannelExt che1 = tlChannels.get(idx);
			che1.presenceCount = pCount;
		}
	}

	public void setDefaultChannel(int uid, int ctype, int cid) {
		if (channelApi != null)
			channelApi.setDefaultChannel(uid, ctype, cid);
	}

	public void onSelfStatusChange(int st) {
		Log.d(TAG, "self status => " + st);
		boolean wasOnline = selfOnline;
		selfOnline = (st == Constant.CONTACT_STATE_ONLINE);

		if (st == Constant.CONTACT_STATE_OFFLINE) {
			if (currSession != null) {
				boolean found = false;
				if (myChannels != null)
					for (ChannelExt che1 : myChannels)
						if (Channel.sameCid(currSession, che1.cid)) {
							found = true;
						}
				sessPresences = null;
				if (found)
					asyncInitPopupMemberList();
			}
			sessionChanging(false);
			asyncUpdateChannelUI();
		} else if (st == Constant.CONTACT_STATE_ONLINE) {
			if (myChannels == null && !wasOnline)
				funcGetAdverChannels();
			else
				asyncUpdateChannelUI();
		}
	}

	public void onPeerStatusChange(int uid, int st) {
		boolean changeCurr = saveUserStatus(uid, st);
		if (changeCurr)
			asyncInitPopupMemberList();
	}

	private boolean saveUserStatus(int uid, int st) {
		boolean found = false;
		if (myChannels != null)
			for (ChannelExt che1 : myChannels) { // all channels
				if (che1.members != null)
					for (ContactExt ce1 : che1.members) {
						if (ce1.id == uid) {
							ce1.state = st;
							if (Channel.sameCid(che1.cid, currSession))
								found = true;
						}
					}
			}

		if (!found) {
			if (sessPresences != null)
				for (IntStr is1 : sessPresences) {
					if (is1.i == uid) {
						found = true;
						if (st == Constant.CONTACT_STATE_BUSY) {
							is1.b = true;
						} else if (st == Constant.CONTACT_STATE_ONLINE) {
							is1.b = false;
						}
						break;
					}
				}
		}
		return found;
	}

	// onSessionEstablished => startTalkFragment => here.
	public void onStartTalkFragment(int uid, int type, int sid) {
		if (myChannels == null) { // reconnect caused session-establish, and
									// then no channels gotten.
			Log.e(TAG, "myChannels is null, when onStartTalkFragment for session:" + sid);
			return;
		}

		funcGetChannelMemberList(type, sid);
	}

	public void onSetNickName(int uid, String nick) {
		ContactExt ce1 = getMemberById(uid);
		if (ce1 != null) {
			ce1.name = nick;
			asyncInitPopupMemberList();
		}
	}

	public String getChannelNameById(CompactID cid) {
		String chName = "???";
		for (ChannelExt che1 : myChannels) {
			if (Channel.sameCid(che1.cid, cid)) {
				chName = che1.name;
				break;
			}
		}

		return chName;
	}

	private ContactExt getMemberById(int uid) {
		if (myChannels != null) {
			for (ChannelExt che1 : myChannels) {
				if (che1.members != null)
					for (ContactExt ce1 : che1.members) {
						if (ce1.id == uid) {
							return ce1;
						}
					}
			}
		}
		return null;
	}

	private class OnChannelConfigButtonPressed implements OnClickListener {
		@Override
		public void onClick(View v) {
			showChannelConfigDialog();
		}
	}

	private AlertDialog channelsConfigChoiseDialog = null;

	private void showChannelConfigDialog() {
		LayoutInflater li = (LayoutInflater) uiContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View promptsView = li.inflate(R.layout.dialog_channels_config, null);
		Button create = (Button) promptsView.findViewById(R.id.create_channel);
		create.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (channelsConfigChoiseDialog != null
						&& channelsConfigChoiseDialog.isShowing())
					channelsConfigChoiseDialog.dismiss();
				showChannelCreateDialog();
			}
		});
		Button search = (Button) promptsView.findViewById(R.id.search_channel);
		search.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (channelsConfigChoiseDialog != null
						&& channelsConfigChoiseDialog.isShowing())
					channelsConfigChoiseDialog.dismiss();
				showChannelSearchDialog();
			}
		});
		Button modify = (Button) promptsView.findViewById(R.id.modify_channel);
		modify.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (channelsConfigChoiseDialog != null
						&& channelsConfigChoiseDialog.isShowing())
					channelsConfigChoiseDialog.dismiss();
				showChannelModifyDialog();
			}
		});
		Button defaul = (Button) promptsView.findViewById(R.id.default_channel);
		defaul.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (channelsConfigChoiseDialog != null
						&& channelsConfigChoiseDialog.isShowing())
					channelsConfigChoiseDialog.dismiss();
				showChannelDefaultDialog();
			}
		});
		Button delete = (Button) promptsView.findViewById(R.id.delete_channel);
		delete.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (channelsConfigChoiseDialog != null
						&& channelsConfigChoiseDialog.isShowing())
					channelsConfigChoiseDialog.dismiss();
				showChannelDeleteDialog();
			}
		});

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				uiContext, R.style.DialogLite);
		alertDialogBuilder.setView(promptsView);
		alertDialogBuilder.setCancelable(false).setNegativeButton("Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
		channelsConfigChoiseDialog = alertDialogBuilder.create();
		channelsConfigChoiseDialog.show();

		return;
	}

	private void showChannelCreateDialog() {
		LayoutInflater li = (LayoutInflater) uiContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View promptsView = li.inflate(R.layout.dialog_create_channel, null);
		final EditText etChName = (EditText) promptsView
				.findViewById(R.id.make_channel_name);
		final EditText etChEC = (EditText) promptsView
				.findViewById(R.id.channel_entry_code);
		final TextView tvEnEC = (TextView) promptsView
				.findViewById(R.id.entry_code_hint);
		tvEnEC.setTag(1);
		tvEnEC.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if ((Integer) tvEnEC.getTag() == 1) {
					tvEnEC.setTag(2);
					tvEnEC.setText(R.string.entry_code_yes);
					etChEC.setVisibility(View.VISIBLE);
					etChEC.requestFocus();
				} else {
					tvEnEC.setTag(1);
					tvEnEC.setText(R.string.entry_code_no);
					etChEC.setVisibility(View.INVISIBLE);
					etChName.requestFocus();
				}
			}
		});

		createChannel = new Channel(Constant.SESSION_TYPE_CHANNEL, 0, "");

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				uiContext, R.style.menuDialogStyle);
		alertDialogBuilder.setView(promptsView);
		alertDialogBuilder
				.setCancelable(false)
				.setPositiveButton(R.string.ch_create_seg, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						createChannel.name = etChName.getText().toString();
						if (TextUtils.isEmpty(createChannel.name)) {
							createChannel.name = API.uid2nick(selfId) + getResources().getString(R.string.de_channel_seg);
						}
						String newEC = null;
						if ((Integer) tvEnEC.getTag() == 2) {
							newEC = etChEC.getText().toString();
						}
						createChannel.needPassword = !TextUtils.isEmpty(newEC);
						channelApi.createPublicChannel(selfId, createChannel.name, newEC);
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

		return;
	}

	private void showChannelSearchDialog() {
		LayoutInflater li = (LayoutInflater) uiContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View promptsView = li.inflate(R.layout.dialog_add_channel, null);
		final EditText etCh = (EditText) promptsView
				.findViewById(R.id.new_channel);

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				uiContext, R.style.menuDialogStyle);
		alertDialogBuilder.setView(promptsView);
		alertDialogBuilder
				.setCancelable(false)
				.setPositiveButton("List ALL",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								String newCh = etCh.getText().toString();
								if (newCh != null && newCh.length() >= 1) {
									channelApi.searchPublicChannel(selfId,
											newCh);
								} else {
									channelApi.searchPublicChannel(selfId, "*");
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

	private void showChannelModifyDialog() {
		List<Channel> mChs = getModifyCandidates();
		if (mChs == null || mChs.size() < 1) {
			Toast.makeText(uiContext, getResources().getString(R.string.no_channel_hint), Toast.LENGTH_SHORT).show();
		} else {
			((BroadcastActivity) uiContext).showModifyChannelSpinner(mChs);
		}
	}

	private void showChannelDefaultDialog() {
		((BroadcastActivity) uiContext).showSetDefaultSpinner();
	}

	private void showChannelDeleteDialog() {
		((BroadcastActivity) uiContext)
				.showDeleteChannelSpinner(getDeleteCandidates());
	}

	private class OnChannelMemberButtonPressed implements OnClickListener {
		@Override
		public void onClick(View v) {
			int vid = v.getId();
			if (vid == R.id.broadcast_presences) {
				if (popMembers != null && !popMembers.isShowing()) {
					for (ChMemberInfo chm1 : channelMembers)
						chm1.selected = false;
					popMembers.showAsDropDown(v, 0, 5);
				} else if (popMembers == null) {
					initPopupMemberList();
					if (popMembers != null) {
						for (ChMemberInfo chm1 : channelMembers)
							chm1.selected = false;
						popMembers.showAsDropDown(v, 0, 5);
					}
				}
				return;
			}
		}
	}

	public void onShakeScreenAck(int result, int target) {
		String res = result > 0 ? getResources().getString(R.string.success_hint) : getResources().getString(R.string.fail_hint);
		Toast.makeText(uiContext, getResources().getString(R.string.sms_seg) + API.uid2nick(target) + getResources().getString(R.string.deliver_seg) + res,
				Toast.LENGTH_SHORT).show();
	}

	/*
	 * 
	 * TourLink SDK callbacks:
	 */

	@Override
	public void onDefaultChannelSet(int userId, int chType, int defaultChId) {
		String dfltName = "???";
		if (defaultChId > 0)
			for (ChannelExt che1 : myChannels) {
				if (Channel.sameCid(che1.cid, chType, defaultChId)) {
					che1.isHome = true;
					dfltName = che1.name;
				} else
					che1.isHome = false;
			}

		if (defaultChId > 0) {
			defaultCh = new Channel(chType, defaultChId, dfltName);
		}
		Toast.makeText(uiContext, getResources().getString(R.string.default_ch_set) + dfltName, Toast.LENGTH_SHORT)
				.show();
	}

	private String sessionCallFailedReason(int type) {
		switch (type) {
			case Constant.SESSION_RELEASE_REASON_CHANNEL_REMOVED:
				return " CH_REMV";
			case Constant.SESSION_RELEASE_REASON_ALREADY_EXISTED:
				return " IN_CH";
			case Constant.SESSION_RELEASE_REASON_TIMEOUT:
				return " T_OUT";
			default:
				return "";
		}
	}

	@Override
	public void onSessionEstablished(int selfUserId, int type, int sessionId) {
		Log.i(TAG, "session type: " + type + " id " + sessionId + " established.");
		if (channelSelectIsBusy) {
			sessionChanging(false);
		}

		if (selfUserId <= 0 || sessionId <= 0) {
			String reason = sessionCallFailedReason(type);
			Toast.makeText(uiContext, getResources().getString(R.string.no_entry_channel)+reason, Toast.LENGTH_SHORT).show();
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

		if (myChannels != null)
			for (ChannelExt che1 : myChannels) {
				if (che1.isCurrent) {
					che1.isCurrent = false;
				}
				if (Channel.sameCid(che1.cid, currSession)) {
					che1.isCurrent = true;
					che1.presenceCount = 1;
				}
				if (type == Constant.SESSION_TYPE_TOURLINK
						&& che1.cid.getType() == Constant.SESSION_TYPE_TOURLINK) {
					che1.name = makeTourlinkChName(sessionId);
					che1.cid = new CompactID(Constant.SESSION_TYPE_TOURLINK,
							sessionId);
					che1.isCurrent = true;
					che1.presenceCount = 1;
				}
			}

		asyncUpdateChannelUI();
		asyncInitPopupMemberList();
		return;
	}

	@Override
	public void onSessionReleased(int selfUserId, int sessionType, int sessionId) {
		Log.i(TAG, "session:  " + sessionType + ":" + sessionId + " released.");

		if (channelSelectIsBusy) {
			sessionChanging(false);
		}

		// session stop may failure (when network broken), use current
		// session
		uiHandler.obtainMessage(MsgCode.MC_ONSESSIONRELEASED, currSession.getType(), currSession.getId()).sendToTarget();
		currSession = null;
		sessPresences = null;

		for (ChannelExt che1 : myChannels) {
			if (che1.isCurrent) {
				che1.isCurrent = false;
				if (che1.cid.getType() == Constant.SESSION_TYPE_TOURLINK) {
					che1.cid = new CompactID(Constant.SESSION_TYPE_TOURLINK, 0);
					che1.presenceCount = 0;
					che1.name = makeTourlinkChName(0);
				}
			}
		}

		asyncUpdateChannelUI();
		asyncInitPopupMemberList();
	}

	private void syncCurrentSessionDelayed(int delayMs) {
		uiHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (sessionApi != null) {
					((BroadcastActivity) uiContext).showProgressCircle();
					sessionApi.getCurrentSession(selfId);
				}
			}
		}, delayMs);
	}

	@Override
	public void onSessionGet(int selfUserId, int type, int sessionId, int initiator) {
		((BroadcastActivity) uiContext).dismessProgressCircle();
		Log.d(TAG, "onSessionGet " + type + ":" + sessionId+" initiator:"+initiator);
	}

	@Override
	public void onSessionPresenceAdded(int ctype, int sid, List<Contact> members) {
		add2Friends(members);

		if (!Channel.sameCid(currSession, ctype, sid))
			return;

		if (sessPresences == null) {
			sessPresences = new ArrayList<IntStr>();
			for (Contact memb2 : members)
				sessPresences.add(new IntStr(memb2.id, memb2.name));
		} else {
			for (Contact memb2 : members) {
				boolean has = false;
				for (IntStr is : sessPresences) {
					if (IntStr.isSame(is, memb2.id, memb2.name)) {
						has = true;
						break;
					}
				}
				if (!has)
					sessPresences.add(new IntStr(memb2.id, memb2.name));
			}
		}

		boolean found = syncChannelPresenceCount(sessPresences.size());

		if (found)
			asyncUpdateChannelUI();
		asyncInitPopupMemberList();
	}

	private boolean syncChannelPresenceCount(int cp) {
		boolean found = false;
		if (myChannels != null)
			for (ChannelExt che1 : myChannels) {
				if (Channel.sameCid(che1.cid, currSession)) {
					che1.presenceCount = cp;
					found = true;
					break;
				}
			}
		if (!found && tlChannels != null)
			for (ChannelExt che2 : tlChannels) {
				if (Channel.sameCid(che2.cid, currSession)) {
					che2.presenceCount = cp;
					found = true;
					break;
				}
			}
		return found;
	}

	@Override
	public void onSessionPresenceRemoved(int ctype, int sid, List<Integer> ids) {
		boolean found = false;
		if (myChannels != null)
			for (ChannelExt che1 : myChannels) {
				if (Channel.sameCid(che1.cid, ctype, sid)) {
					for (int rid : ids) {
						for (ContactExt ce1 : che1.members) {
							if (ce1.id == rid) {
								if (ce1.isPresent) {
									ce1.isPresent = false;
									che1.presenceCount--;
								}
								break;
							}
						}
					}
					found = true;
					break;
				}
			}

		if (!Channel.sameCid(currSession, ctype, sid))
			return;

		if (sessPresences != null) {
			for (int j : ids) {
				IntStr ris = null;
				for (IntStr is : sessPresences) {
					if (is.i == j) {
						ris = is;
						break;
					}
				}
				if (ris != null)
					sessPresences.remove(ris);
			}
		}

		found = syncChannelPresenceCount(sessPresences.size());

		if (found)
			asyncUpdateChannelUI();
		asyncInitPopupMemberList();
	}

	private void funcGetAdverChannels() {
		if (selfOnline && myChannels == null) {
			if (!simpleChannelMode) {
				((BroadcastActivity) uiContext).showProcessing(getResources().getString(R.string.getting_pub_channel));
				if (channelApi != null)
					channelApi.adverChannelsGet(selfId);
			} else {
				funcGetChannelList();
			}
		}
	}

	public void onAdverChannelsGet(int userId, Channel dfltChannel,
			@SuppressWarnings("rawtypes") List chs) {
		@SuppressWarnings("unchecked")
		List<Channel> channels = (List<Channel>) chs;
		((BroadcastActivity) uiContext).dismissProcessing();

		if (userId <= 0 || channels == null) {
			Log.e(TAG, "get adver channels error!");
			uiHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					funcGetAdverChannels();
				}
			}, selfOnline ? 2000 : 3000);
			return;
		}

		mergeChannelList(dfltChannel, channels);

		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				funcGetChannelList();
			}
		});
	}

	private void funcGetChannelList() {
		if (selfOnline) {
			((BroadcastActivity) uiContext).showProcessing(getResources().getString(R.string.getting_channels));
			if (channelApi != null)
				channelApi.channelListGet(selfId);
		} else {
			uiHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					funcGetChannelList();
				}
			}, 3000);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onChannelListGet(int userId, Channel dfltChannel, @SuppressWarnings("rawtypes") List chs) {
		List<Channel> channels = (List<Channel>) chs;
		((BroadcastActivity) uiContext).dismissProcessing();

		if (userId <= 0 || channels == null) {
			Log.e(TAG, "get channel lists error!");
			uiHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					funcGetChannelList();
				}
			}, selfOnline ? 2000 : 3000);
			return;
		}

		updateChannelsHint(channels.size());
		Log.i(TAG, "on channel list get " + channels.size() + " channels.");
		mergeChannelList(dfltChannel, channels);

		// sync SDK session state
		Session currSess = sessionApi.getSessionState(selfId);
		Room dialog = (currSess != null) ? currSess.getDialog() : null;
		final List<Integer> dialogers = dialog == null ? null : dialog.parties;
		if (currSess != null) {
			Log.i(TAG, "channel fragment already in session " + currSess.cid.getCompactId());
			if (!Channel.sameCid(currSess.cid, currSession)) {
				Log.d(TAG, "onGetChannelList with currentSession: " + currSession + " SDK currentSess: " + currSess.cid.getCompactId());
				currSession = new CompactID(currSess.cid.getType(), currSess.cid.getId());
			}
			boolean found = false;
			for (ChannelExt ch1 : myChannels) {
				if (Channel.sameCid(currSession, ch1.cid)) {
					found = true;
					ch1.isCurrent = true;
					break;
				}
				if (currSession.getType() == Constant.SESSION_TYPE_TOURLINK
						&& ch1.cid.getType() == Constant.SESSION_TYPE_TOURLINK) {
					found = true;
					ch1.isCurrent = true;
					ch1.cid = new CompactID(Constant.SESSION_TYPE_TOURLINK, currSession.getId());
					ch1.name = makeTourlinkChName(currSession.getId());
					break;
				}
			}
			if (!found)
				Log.e(TAG, "current channel (" + currSession.getCompactId() + ") has no myChannels index recorded.");
			uiHandler.obtainMessage(MsgCode.MC_ONSESSIONESTABLISHED, currSession.getType(), currSession.getId()).sendToTarget();
		} else {
			// make channel-session call
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					if (defaultCh != null)
						sessionApi.sessionCall(selfId, defaultCh.cid.getType(),
								defaultCh.cid.getId());
					else
						// no default channel set, enter 1st channel
						if (myChannels.size() > 0)
							sessionApi.sessionCall(selfId, myChannels.get(0).cid.getType(), myChannels.get(0).cid.getId());
				}
			});
		}

		// this is the 1st time flush UI
		if (myChannels.size() > 0 && getCurrentDispChIdx() == 0) {
			ChannelExt che1 = myChannels.get(0);
			uiHandler.obtainMessage(MsgCode.MC_ONDISPLAYCHANGED, che1.cid.getType(), che1.cid.getId()).sendToTarget();
		}
		asyncUpdateChannelUI();
		// patch for display in-dialog state.
		if (dialogers != null) {
			((BroadcastActivity)uiContext).runOnUiThread(new Runnable(){
				@Override
				public void run() {
					showDialogHint(1, dialogers.get(0), dialogers);
				}});
		}
	}

	private void mergeChannelList(Channel dfltCh, List<Channel> chs) {
		if (myChannels == null)
			myChannels = new ArrayList<ChannelExt>();

		List<Integer> myADCs = ((BroadcastActivity)uiContext).getMyPreferedChannels();
		
		for (Channel ch : chs) {
			if (ch.cid.getType() == Constant.SESSION_TYPE_TOURLINK) {
				int tlIdx = mergeTourlinkChannel(ch);
				if (tlIdx == 1) {
					ChannelExt tlCh = new ChannelExt(
							Constant.SESSION_TYPE_TOURLINK, 0,
							makeTourlinkChName(0));
					myChannels.add(tlCh);
				}

				continue;
			}

			Log.i(TAG, "add pub channel "+ch.cid.getType()+":"+ch.cid.getId());
			boolean discast = false;
//			if (ch.cid.getType() == Constant.SESSION_TYPE_ADV && ! myADCs.contains(ch.cid.getId())) {
//				discast = true;
//			}

			boolean has = false;
			for (ChannelExt che2 : myChannels)
				if (Channel.sameCid(ch.cid, che2.cid)) {
					has = true;
					break;
				}

			if (!has && !discast) {
				ChannelExt che1 = new ChannelExt(ch.cid.getType(),
						ch.cid.getId(), ch.name);
				che1.copyAttrs(ch);
				if (Channel.sameChannel(dfltCh, ch)) {
					defaultCh = new Channel(dfltCh.cid.getType(),
							dfltCh.cid.getId(), ch.name);
					che1.isHome = true;
				} else {
					che1.isHome = false;
				}
				myChannels.add(che1);
			}
		}
	}

	/*
	 * private String channelDisplayName(Channel ch1) { int type =
	 * ch1.cid.getType(); int idx = ch1.cid.getId();
	 * 
	 * if (type == Constant.SESSION_TYPE_ECHO) return "echo channel"; if (type ==
	 * Constant.SESSION_TYPE_LOCAL) return "local channel"; if (type ==
	 * Constant.SESSION_TYPE_TOURLINK) return "tourling "+idx+" channel";
	 * 
	 * return ch1.name; }
	 */

	private String makeTourlinkChName(int chIdx) {
		if (chIdx <= 0)
			return getResources().getString(R.string.tourling_ch_hint);
		return getResources().getString(R.string.tourling_ch_seg) + chIdx + getResources().getString(R.string.ch_hint_seg);
	}

	private int mergeTourlinkChannel(Channel ch1) {
		if (tlChannels == null)
			tlChannels = new ArrayList<ChannelExt>();

		int chIdx = ch1.cid.getId();
		ChannelExt tlCh1 = new ChannelExt(Constant.SESSION_TYPE_TOURLINK,
				chIdx, makeTourlinkChName(chIdx));
		tlCh1.presenceCount = ch1.presenceCount;
		if (chIdx > tlChannels.size()) {
			tlChannels.add(tlCh1);
		}

		return chIdx;
	}

	private void funcGetChannelMemberList(int ctype, int cid) {
		if (channelApi != null) {
			((BroadcastActivity) uiContext).showProgressCircle();
			channelApi.channelMemberGet(selfId, ctype, cid);
		}
	}

	@Override
	public void onChannelMemberListGet(int userId, int ctype, int channelId,
			@SuppressWarnings("rawtypes") List mems) {
		@SuppressWarnings("unchecked")
		List<Contact> members = (List<Contact>) mems;
		// 统计房间人数
//		int peoplenum = mems.size();

		((BroadcastActivity) uiContext).dismessProgressCircle();
		String selfNick = API.uid2nick(selfId);

		if (userId <= 0) {
			Log.e(TAG, "get channel members error.");
			return;
		}

		add2Friends(members);

		if (Channel.sameCid(currSession, ctype, channelId)) {
			if (mems.size() == 0) {
				if (sessPresences == null) {
					sessPresences = new ArrayList<IntStr>();
					sessPresences.add(new IntStr(selfId, selfNick));
					// no session presences till now.
					syncCurrentSessionDelayed(200);
				} else {
					boolean has = false;
					for (IntStr is1 : sessPresences) {
						Log.d(TAG, "session presence " + is1.s + " id:" + is1.i);
						if (is1.i == selfId) {
							has = true;
							break;
						}
					}
					if (!has) {
						sessPresences.add(new IntStr(selfId, selfNick));
						Log.d(TAG, "session presence add self " + selfId + ":" + selfNick);
					}
				}
				Contact me = new Contact(selfId, selfNick, Constant.CONTACT_STATE_ONLINE);
				List<Contact> mel = new ArrayList<Contact>();
				mel.add(me);
				add2Friends(mel);
			} else {
				sessPresences = new ArrayList<IntStr>();
				sessPresences.add(new IntStr(selfId, selfNick));
				for (Contact memb1 : members) {
					if (memb1.isPresent && memb1.id != selfId)
						sessPresences.add(new IntStr(memb1.id, memb1.name));
				}
			}
		}

		boolean found = false;
		for (ChannelExt che1 : myChannels) {
			if (Channel.sameCid(che1.cid, ctype, channelId)) {
				che1.members.clear(); // members is initialized in Constructor
				for (Contact c1 : members) {
					ContactExt ce2 = new ContactExt(c1);
					che1.members.add(ce2);
				}
				if (sessPresences != null)
					che1.presenceCount = sessPresences.size();
				found = true;
				break;
			}
		}

		if (found) {
			uiHandler.obtainMessage(MsgCode.MC_CHANNELMEMBERSGET, userId, ctype, channelId).sendToTarget();
			if (members.size() > 0) {
				asyncInitPopupMemberList();
			}
		}

		if (mems.size() > 0)
			asyncUpdateChannelUI();
	}

	@Override
	public void onChannelMemberAdded(int ctype, int cid, List<Contact> members) {
		boolean found = false;
		if (myChannels != null)
			for (ChannelExt che1 : myChannels) {
				if (Channel.sameCid(che1.cid, ctype, cid)) {
					for (Contact memb1 : members) {
						ContactExt ce2 = new ContactExt(memb1);
						ce2.state = Constant.CONTACT_STATE_ONLINE;	// member add = focus channel, so online.
						che1.members.add(ce2);
					}
					found = true;
					break;
				}
			}

		add2Friends(members);

		if (found & Channel.sameCid(currSession, ctype, cid))
			asyncInitPopupMemberList();
	}

	@Override
	public void onChannelMemberRemoved(int ctype, int cid, List<Integer> ids) {
		boolean found = false;
		if (myChannels != null)
			for (ChannelExt che1 : myChannels) {
				if (Channel.sameCid(che1.cid, ctype, cid)) {
					for (int j : ids) {
						ContactExt rce = null;
						for (ContactExt ce1 : che1.members) {
							if (ce1.id == j) {
								rce = ce1;
								break;
							}
						}
						if (rce != null) {
							if (rce.isPresent)
								che1.presenceCount--;
							che1.members.remove(rce);
						}
					}
					found = true;
					break;
				}
			}

		if (found & Channel.sameCid(currSession, ctype, cid))
			asyncInitPopupMemberList();
	}

	@Override
	public void onChannelAdded(int userId, int ctype, int channelId, String channelName) {
		boolean found = false;
		for (ChannelExt che1 : myChannels) {
			if (Channel.sameCid(che1.cid, ctype, channelId)) {
				che1.name = new String(channelName);
				found = true;
				break;
			}
		}

		if (!found) {
			ChannelExt che2 = new ChannelExt(ctype, channelId, channelName);
			myChannels.add(che2);
			if (myChannels.size() == 1)
				uiHandler.obtainMessage(MsgCode.MC_ONDISPLAYCHANGED, che2.cid.getType(), che2.cid.getId()).sendToTarget();
			asyncUpdateChannelUI();
		}
	}

	@Override
	public void onChannelRemoved(int userId, int ctype, int channelId) {
		boolean found = false;
		ChannelExt rChe = null;
		for (ChannelExt che1 : myChannels) {
			if (Channel.sameCid(che1.cid, ctype, channelId)) {
				found = true;
				rChe = che1;
				break;
			}
		}

		if (found) {
			myChannels.remove(rChe);
			asyncUpdateChannelUI();
		}
	}

	@Override
	public void onChannelNameChanged(int uid, int ctype, int cid, String name) {
		boolean found = false;
		for (ChannelExt che1 : myChannels) {
			if (Channel.sameCid(che1.cid, ctype, cid)) {
				found = true;
				che1.name = new String(name);
				break;
			}
		}

		if (found)
			asyncUpdateChannelUI();
	}

	/*
	 * 
	 * public channel callbacks
	 */

	private Channel createChannel = null;

	@Override
	public void onPubChannelCreate(int uid, int reason, int cid) {
		if (uid > 0) {
			Toast.makeText(uiContext, getResources().getString(R.string.create_success), Toast.LENGTH_SHORT).show();
			if (myChannels != null) {
				ChannelExt che1 = new ChannelExt(Constant.SESSION_TYPE_CHANNEL, cid, createChannel.name);
				che1.needPassword = createChannel.needPassword;
				che1.owner = new IntStr(selfId, API.uid2nick(selfId));
				boolean has = false;
				for (ChannelExt che2 : myChannels) {
					if (Channel.sameCid(che1.cid, che2.cid)) {
						has = true;
						break;
					}
				}
				if (!has)
					myChannels.add(che1);
				if (myChannels.size() == 1)
					uiHandler.obtainMessage(MsgCode.MC_ONDISPLAYCHANGED, che1.cid.getType(), che1.cid.getId()).sendToTarget();
				asyncUpdateChannelUI();
			}
		} else {
			Toast.makeText(uiContext, getResources().getString(R.string.create_failed) + pubChErrorString(reason),
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onPubChannelSearchResult(int uid, @SuppressWarnings("rawtypes") List channels) {
		@SuppressWarnings("unchecked")
		List<Channel> foundChs = (List<Channel>) channels;
		if (uid > 0 && foundChs.size() > 0) {
			List<Channel> diffChs = removeFocused(foundChs);
			if (diffChs.size() > 0)
				((BroadcastActivity) uiContext).showFocusChannelSpinner(diffChs);
			else
				Toast.makeText(uiContext, "no new", Toast.LENGTH_SHORT).show();
		} else
			Toast.makeText(uiContext, "no no no", Toast.LENGTH_SHORT).show();
	}

	private List<Channel> removeFocused(List<Channel> Chs) {
		List<Channel> rChs = new ArrayList<Channel>();
		for (Channel ch1 : Chs) {
			boolean has = false;
			for (ChannelExt che1 : myChannels) {
				if (Channel.sameCid(che1.cid, ch1.cid)) {
					has = true;
					break;
				}
			}
			if (!has)
				rChs.add(ch1);
		}
		return rChs;
	}

	@Override
	public void onPubChannelFocusResult(int uid, int reason) {
		if (uid > 0) {
			Toast.makeText(uiContext, getResources().getString(R.string.focuse_success), Toast.LENGTH_SHORT).show();
			if (myChannels != null) {
				ChannelExt che1 = new ChannelExt(focusChannel.cid.getType(),
						focusChannel.cid.getId(), focusChannel.name);
				che1.needPassword = focusChannel.needPassword;
				che1.owner = new IntStr(focusChannel.owner.i,
						focusChannel.owner.s);
				boolean has = false;
				for (ChannelExt che2 : myChannels) {
					if (Channel.sameCid(che1.cid, che2.cid)) {
						has = true;
						break;
					}
				}
				if (!has)
					myChannels.add(che1);
				if (myChannels.size() == 1)
					uiHandler.obtainMessage(MsgCode.MC_ONDISPLAYCHANGED, che1.cid.getType(), che1.cid.getId()).sendToTarget();
				asyncUpdateChannelUI();
			}
		} else {
			Toast.makeText(uiContext, getResources().getString(R.string.focuse_failed) + pubChErrorString(reason),
					Toast.LENGTH_SHORT).show();
		}
		focusChannel = null;
	}

	@Override
	public void onPubChannelUnfocusResult(int uid, int reason) {
		if (uid > 0) {
			Toast.makeText(uiContext, getResources().getString(R.string.unfocuse_success), Toast.LENGTH_SHORT).show();
			if (myChannels != null) {
				ChannelExt rche = null;
				for (ChannelExt che2 : myChannels) {
					if (Channel.sameCid(che2.cid, unfocusChannel.cid)) {
						rche = che2;
						break;
					}
				}
				if (rche != null)
					myChannels.remove(rche);
			}
			asyncUpdateChannelUI();
		} else {
			Toast.makeText(uiContext, getResources().getString(R.string.unfocuse_failed) + pubChErrorString(reason),
					Toast.LENGTH_SHORT).show();
		}
		unfocusChannel = null;
	}

	@Override
	public void onPubChannelRenamed(int uid, int reason) {
		if (uid > 0) {
			Toast.makeText(uiContext, getResources().getString(R.string.rename_success), Toast.LENGTH_SHORT).show();
			boolean has = false;
			if (myChannels != null)
				for (ChannelExt che1 : myChannels) {
					if (Channel.sameCid(che1.cid, modifyChannel.cid)) {
						che1.name = new String(modifyChannel.name);
						has = true;
						break;
					}
				}
			if (has)
				asyncUpdateChannelUI();
		} else {
			Toast.makeText(uiContext, getResources().getString(R.string.rename_failed) + pubChErrorString(reason),
					Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onPubChannelDeleted(int uid, int reason) {
		if (uid > 0) {
			Toast.makeText(uiContext, getResources().getString(R.string.delete_success), Toast.LENGTH_SHORT).show();
			if (myChannels != null) {
				ChannelExt rche = null;
				for (ChannelExt che2 : myChannels) {
					if (Channel.sameCid(che2.cid, deleteChannel.cid)) {
						rche = che2;
						break;
					}
				}
				if (rche != null)
					myChannels.remove(rche);
				asyncUpdateChannelUI();
			}
		} else {
			Toast.makeText(uiContext, getResources().getString(R.string.delete_failed) + pubChErrorString(reason),
					Toast.LENGTH_SHORT).show();
		}
		deleteChannel = null;
	}

	@Override
	public void onCallMeetingStarted(final int meetingType, final int meetingId, int initiator, List<Contact> parties) {
		String cmName = getResources().getString(R.string.callmeeting_head) + API.uid2nick(initiator);
		boolean found = false;
		for (ChannelExt che1 : myChannels) {
			if (Channel.sameCid(che1.cid, meetingType, meetingId)) {
				che1.name = cmName;
				found = true;
				break;
			}
		}

		if (!found) {
			ChannelExt che2 = new ChannelExt(meetingType, meetingId, cmName);
			for (Contact is1 : parties) {
				ContactExt ce1 = new ContactExt(is1);
				che2.members.add(ce1);
			}
			myChannels.add(che2);
			if (myChannels.size() == 1)
				uiHandler.obtainMessage(MsgCode.MC_ONDISPLAYCHANGED, che2.cid.getType(), che2.cid.getId()).sendToTarget();
			asyncUpdateChannelUI();
		}

		// if there is not in any session, join the meeting.
		if (currSession == null) {
			((BroadcastActivity)uiContext).runOnUiThread(new Runnable(){
				@Override
				public void run() {
					sessionApi.sessionCall(selfId, meetingType, meetingId);
				}
			});
		}
	}

	@Override
	public void onCallMeetingStopped(int meetingType, int meetingId) {
		boolean found = false;
		ChannelExt rChe = null;
		for (ChannelExt che1 : myChannels) {
			if (Channel.sameCid(che1.cid, meetingType, meetingId)) {
				found = true;
				rChe = che1;
				break;
			}
		}

		if (found) {
			myChannels.remove(rChe);
			asyncUpdateChannelUI();
		}
	}

	private Channel focusChannel = null;

	public void focusChannel(int uid, Channel focusCh, String chEC) {
		if (channelApi != null) {
			focusChannel = focusCh;
			channelApi.focusPublicChannel(uid, focusCh.cid.getType(),
					focusCh.cid.getId(), chEC);
		}
	}

	private Channel deleteChannel = null;

	public void deleteChannel(int uid, int type, int channelId) {
		if (channelApi != null) {
			deleteChannel = new Channel(type, channelId, "");
			channelApi.deletePublicChannel(uid, type, channelId);
		}
	}

	private Channel unfocusChannel = null;

	public void unfocusChannel(int uid, int type, int channelId) {
		if (channelApi != null) {
			unfocusChannel = new Channel(type, channelId, "");
			channelApi.unfocusPublicChannel(uid, type, channelId);
		}
	}

	private Channel modifyChannel = null;

	public void modifyChannelAttrs(Channel ch, String n) {
		if (channelApi != null) {
			channelApi.changePublicChannelName(selfId, ch.cid.getType(),
					ch.cid.getId(), n);
			modifyChannel = new Channel(ch.cid.getType(), ch.cid.getId(), n);
		}
	}

	private String pubChErrorString(int reason) {
		if (reason == Constant.E_PUBCH_OVERLIMIT)
			return getResources().getString(R.string.cherr_toomuch);
		if (reason == Constant.E_PUBCH_PASSWORD)
			return getResources().getString(R.string.cherr_psss);

		return getResources().getString(R.string.cherr_unknown);
	}

	private int getCurrentDispChIdx() {
		int currChIdx = findCurrentChannel();
		if (currChIdx < 0) {
			currChIdx = findHomeChannel();
			if (currChIdx < 0)
				currChIdx = 0; // ECHO channel is default.
		}
		return currChIdx;
	}

	@Override
	public void onDialogEstablished(int selfId, final int dialogId, final int owner, final List<Integer> members) {
		((BroadcastActivity) uiContext).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				showDialogHint(dialogId, owner, members);
			}
		});
	}

	@Override
	public void onDialogLeaved(int selfId, final int dialogId) {
		((BroadcastActivity) uiContext).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				closeDialogHint(dialogId);
			}
		});
	}

	@Override
	public void onDialogPresenceAdded(int self, final int dialogId, final List<Integer> added) {
		((BroadcastActivity) uiContext).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updateDialogHint(true, dialogId, added);
			}
		});
	}

	@Override
	public void onDialogPresenceRemoved(int self, final int dialogId, final List<Integer> removed) {
		((BroadcastActivity) uiContext).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				updateDialogHint(false, dialogId, removed);
			}
		});
	}
}
