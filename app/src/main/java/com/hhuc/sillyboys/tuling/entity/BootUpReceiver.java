package com.hhuc.sillyboys.tuling.entity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.hhuc.sillyboys.tuling.LoginActivity;

public class BootUpReceiver extends BroadcastReceiver {
	public static final String TAG = "boot.start";
	static final String action_boot = "android.intent.action.BOOT_COMPLETED";

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.i(TAG, "get action: " + action);
		if (action.equals(action_boot)) {
			Intent bootStartIn = new Intent(context, LoginActivity.class);
			bootStartIn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(bootStartIn);
		}
	}
}
