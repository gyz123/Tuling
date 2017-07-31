package com.hhuc.sillyboys.tuling.tl_demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Handler;

import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.entity.MsgCode;

public class RegisterUser {
	public static final String TAG = "reg.io";
	public static final String KeyAccount = "flag_account_string";
	public static final String KeyPassword = "flag_password_string";
	public static final String KeyNick = "flag_nick_string";
	public static final String KeyInviteCode = "flag_invite_string";
	public static final String KeyHasRegistered = "flag_has_registered_bool";
	public static final String KeyIsVisitor = "flag_visitor_bool";
	public static final String KeyAuthCode = "flag_auth_code";

	private Context mContext = null;
	private int mResource = 0;
	private MySharedPreferences myIO;
	private Handler handler;

	public RegisterUser(Context context, Handler handler) {
		this.mContext = context;
		this.handler = handler;

		myIO = new MySharedPreferences(context);
	}

	public void startVisitor(int resource) {
		mResource = resource;

		if (!myIO.getBoolean(KeyHasRegistered)) {
			DialogVisitor loginDialog = new DialogVisitor(mContext,
					R.style.loginDialogStyle, mResource, handler, null, null);
			loginDialog.show();
		} else if (myIO.getBoolean(KeyIsVisitor)) {
			String account = myIO.Read(KeyAccount);
			String nick = myIO.Read(KeyNick);
			DialogVisitor loginDialog = new DialogVisitor(mContext,
					R.style.loginDialogStyle, mResource, handler, account, nick);
			loginDialog.show();
		} else {
			handler.obtainMessage(MsgCode.LOGINPAGE, null).sendToTarget();
		}
	}

	public void startUserLogin(int resource) {
		mResource = resource;

		if (!myIO.getBoolean(KeyHasRegistered) || myIO.getBoolean(KeyIsVisitor)) {
			DialogLogin loginDialog = new DialogLogin(mContext,
					R.style.loginDialogStyle, mResource, handler, null, null);
			loginDialog.show();
		} else {
			String account = myIO.Read(KeyAccount);
			String passwd = myIO.Read(KeyPassword);
			DialogLogin loginDialog = new DialogLogin(mContext,
					R.style.loginDialogStyle, mResource, handler, account,
					passwd);
			loginDialog.show();
		}
	}

	public void startRegister(int resource) {
		mResource = resource;
		DialogRegister loginReg = new DialogRegister(mContext,
				R.style.loginDialogStyle, mResource, handler);
		loginReg.show();
	}

	public void startResetPass(int resource, int uid, String uAccount) {
		mResource = resource;
		DialogResetPass loginReg = new DialogResetPass(mContext,
				R.style.loginDialogStyle, mResource, handler, uid, uAccount);
		loginReg.show();
	}

	public String getUserAccount() {
		return myIO.Read(KeyAccount);
	}

	public void saveUserInfo(String name, String nick, String pass) {
		myIO.Write(KeyAccount, name);
		myIO.Write(KeyNick, nick);
		myIO.Write(KeyPassword, pass);
	}

	public void setNickName(String nick) {
		myIO.Write(KeyNick, nick);
	}

	public void removeUserInfo() {
		myIO.Remove(KeyHasRegistered);
		myIO.Remove(KeyAccount);
		myIO.Remove(KeyPassword);
	}

	public void setVisitor(boolean yn) {
		myIO.setBoolean(KeyIsVisitor, yn);
		return;
	}

	public void setUserRegistered() {
		myIO.setBoolean(KeyHasRegistered, true);
		return;
	}

	private class MySharedPreferences {
		public static final String TourLinkRegisterInfo = "register.info.tourlink";

		private Context mContext = null;

		public MySharedPreferences(Context c) {
			mContext = c;
		}

		public boolean getBoolean(String key) {
			SharedPreferences sharedPreferences = mContext
					.getSharedPreferences(TourLinkRegisterInfo,
							Context.MODE_PRIVATE);
			boolean rBool = sharedPreferences.getBoolean(key, false);

			return rBool;
		}

		public void setBoolean(String key, boolean tf) {
			SharedPreferences sharedPreferences = mContext
					.getSharedPreferences(TourLinkRegisterInfo,
							Context.MODE_PRIVATE);
			Editor editor = sharedPreferences.edit();
			editor.putBoolean(key, tf);
			editor.commit();
		}

		public String Read(String key) {
			SharedPreferences sharedPreferences = mContext
					.getSharedPreferences(TourLinkRegisterInfo,
							Context.MODE_PRIVATE);
			String rStr = sharedPreferences.getString(key, null);
			return rStr;
		}

		public void Write(String key, String value) {
			SharedPreferences sharedPreferences = mContext
					.getSharedPreferences(TourLinkRegisterInfo,
							Context.MODE_PRIVATE);
			Editor editor = sharedPreferences.edit();
			editor.putString(key, value);
			editor.commit();
		}

		public void Remove(String key) {
			SharedPreferences sharedPreferences = mContext
					.getSharedPreferences(TourLinkRegisterInfo,
							Context.MODE_PRIVATE);
			Editor editor = sharedPreferences.edit();
			editor.remove(key);
			editor.commit();
		}
	}
}

