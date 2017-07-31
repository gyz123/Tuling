package com.hhuc.sillyboys.tuling.entity;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.algebra.sdk.API;
import com.algebra.sdk.AccountApi;
import com.algebra.sdk.SessionApi;
import com.algebra.sdk.entity.Contact;
import com.algebra.sdk.entity.Session;
import com.hhuc.sillyboys.tuling.LoginActivity;

import java.util.List;

public class PttReceiver extends BroadcastReceiver {
	private static final String TAG = "key.receiver";
	private static final String PTT_KEY =  "android.intent.action.PTT";
	public static final String PTT_KEY_ON =  "android.intent.action.PTT.down";
	public static final String PTT_KEY_OFF = "android.intent.action.PTT.up";

	private static boolean pttPressed = false;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.contains(PTT_KEY)) {
			ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
			List<RunningTaskInfo> runTasks = am.getRunningTasks(10);
			boolean found = false;
			for (RunningTaskInfo info : runTasks) {
				if (info.topActivity.getPackageName().equals("com.jzone.tourlink") &&
					info.baseActivity.getPackageName().equals("com.jzone.tourlink")) {
					// bring to foreground
					am.moveTaskToFront(info.id, ActivityManager.MOVE_TASK_WITH_HOME);
					found = true;
					break;
				}
			}

			if (!found) {
				final Intent it = new Intent(context, LoginActivity.class);
				it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(it);
			} else {
				AccountApi accountApi = API.getAccountApi();
				SessionApi sessionApi = API.getSessionApi();
				if (accountApi == null || sessionApi == null) {
					pttPressed = false;
					return;
				}
				Contact me = accountApi.whoAmI();
				if (me == null) {
					pttPressed = false;
					return;
				}
				Session currSess = sessionApi.getSessionState(me.id);
				if (currSess != null && currSess.cid.getId() > 0) {
					if (!pttPressed && PTT_KEY_ON.equals(action)) {
						pttPressed = true;
						sessionApi.talkRequest(me.id, currSess.cid.getType(), currSess.cid.getId());
					} else if (pttPressed && PTT_KEY_OFF.equals(action)){
						pttPressed = false;
						sessionApi.talkRelease(me.id, currSess.cid.getType(), currSess.cid.getId());
					} else {
						pttPressed = false;
					}
				} else {
					pttPressed = false;
				}
			}
		} else {
			Log.i(TAG, "PttReceiver: "+action);
		}
	}

/*	String cur_activity = getRunningActivityName(context);
	Log.e(TAG, "current activity "+cur_activity);
	if (cur_activity != null && !cur_activity.contains("com.jzone.minitl.TLActivity"))	*/
/*	private String getRunningActivityName(Context context) {
		ActivityManager actiManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		String runningActivity = actiManager.getRunningTasks(1).get(0).topActivity
				.getClassName();
		return runningActivity;
	}	*/
}


/*
public class PttReceiver extends BroadcastReceiver {
	private static final String TAG = "key.receiver";
//	private static final String HCT_KEY = "com.microntek.irkeyDown";
//	private static final int PTT_CODE = 0x16E;
	private static final String G3_KEYD = "android.intent.action.PTT.down";
	private static final String G3_KEYU = "android.intent.action.PTT.up";

	private static boolean pttPressed = false;

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		int keyCode = intent.getIntExtra("keyCode", 0x55aa);
		if (HCT_KEY.equals(action) && keyCode == PTT_CODE) {
			String cur_activity = getRunningActivityName(context);
			Log.e(TAG, "current activity " + cur_activity);
			if (cur_activity != null
					&& !cur_activity.contains("com.jzone.minitl.TLActivity")) {
				final Intent it = new Intent(context, TLActivity.class);
				it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(it);
			} else {
				AccountApi accountApi = API.getAccountApi();
				SessionApi sessionApi = API.getSessionApi();
				if (accountApi == null || sessionApi == null) {
					pttPressed = false;
					return;
				}
				Contact me = accountApi.whoAmI();
				if (me == null) {
					pttPressed = false;
					return;
				}
				Session currSess = sessionApi.getSessionState(me.id);
				if (currSess != null && currSess.cid.getId() > 0) {
					if (!pttPressed) {
						pttPressed = true;
						sessionApi.talkRequest(me.id, currSess.cid.getType(),
								currSess.cid.getId());
					} else {
						pttPressed = false;
						sessionApi.talkRelease(me.id, currSess.cid.getType(),
								currSess.cid.getId());
					}
				} else {
					pttPressed = false;
				}
			}
		}
	}

	private String getRunningActivityName(Context context) {
		ActivityManager actiManager = (ActivityManager) context
				.getSystemService(Context.ACTIVITY_SERVICE);
		String runningActivity = actiManager.getRunningTasks(1).get(0).topActivity
				.getClassName();
		return runningActivity;
	}
}
*/