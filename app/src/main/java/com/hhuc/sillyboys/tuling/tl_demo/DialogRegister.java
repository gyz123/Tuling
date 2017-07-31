package com.hhuc.sillyboys.tuling.tl_demo;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.algebra.sdk.API;
import com.algebra.sdk.AccountApi;
import com.algebra.sdk.entity.Utils;
import com.hhuc.sillyboys.tuling.entity.MsgCode;
import com.hhuc.sillyboys.tuling.R;

import java.util.HashMap;

public class DialogRegister extends Dialog {
	private Context context;
	private int resource;
	private DialogRegister self;
	private Handler handler;
	private HashMap<String, String> outputInfo = new HashMap<String, String>();

	public DialogRegister(Context context, int theme, int resource,
						  Handler handler) {
		super(context, theme);
		this.context = context;
		this.resource = resource;
		this.self = this;
		this.handler = handler;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(resource);
		LinearLayout linearLayout = ((LinearLayout) findViewById(R.id.registerTop));
		linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		LayoutParams layoutParams = linearLayout.getLayoutParams();
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		if (dm.widthPixels > dm.heightPixels)
			layoutParams.width = (int) (dm.widthPixels * 0.6);// -
																// dm.densityDpi/1;
		else
			layoutParams.width = (int) (dm.widthPixels * 0.8);
		linearLayout.setLayoutParams(layoutParams);

		final String nickHint = getNickHint();
		final EditText etNick = (EditText) findViewById(R.id.user_nick);
		etNick.setHint(nickHint);
		((Button) findViewById(R.id.registerButton))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String invCode = ((EditText) findViewById(R.id.invite_code))
								.getText().toString();
						String nick = ((EditText) findViewById(R.id.user_nick))
								.getText().toString();
						String pw = ((EditText) findViewById(R.id.registerPW))
								.getText().toString();
						String rePw = ((EditText) findViewById(R.id.registerRePW))
								.getText().toString();
						TextView errorInfo = (TextView) findViewById(R.id.registerInfo);

						if (invCode == null || invCode.length() < 8) {
							errorInfo.setText("error code");
							return;
						}
						if (!pw.equals(rePw) || "".equals(pw)
								|| pw.length() < 4) {
							errorInfo.setText("pass format");
							return;
						}
						if (nick == null || nick.length() == 0)
							nick = nickHint;
						else if (Utils.getCharacterNum(nick) < AccountApi.MINNICKLEN) {
							errorInfo.setText("nick format");
							return;
						}

						outputInfo.put(RegisterUser.KeyInviteCode, invCode);
						outputInfo.put(RegisterUser.KeyNick, nick);
						outputInfo.put(RegisterUser.KeyPassword, API.md5(pw));
						handler.obtainMessage(MsgCode.ASKFORREGISTER,
								outputInfo).sendToTarget();
						self.dismiss();
					}
				});
		((TextView) (findViewById(R.id.go_login)))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						handler.obtainMessage(MsgCode.LOGINPAGE, null)
								.sendToTarget();
						self.dismiss();
					}
				});
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		handler.obtainMessage(MsgCode.ASKFOREXIT).sendToTarget();
	}

	private String getNickHint() {
		long tick = System.currentTimeMillis();
		int stage = (int) (tick % 10L);
		return context.getResources().getString(R.string.user_hint) + int2tiangan(stage);
	}

	public String int2tiangan(int i) {
		switch (i) {
			case 1:
				return context.getResources().getString(R.string.tg1);
			case 2:
				return context.getResources().getString(R.string.tg2);
			case 3:
				return context.getResources().getString(R.string.tg3);
			case 4:
				return context.getResources().getString(R.string.tg4);
			case 5:
				return context.getResources().getString(R.string.tg5);
			case 6:
				return context.getResources().getString(R.string.tg6);
			case 7:
				return context.getResources().getString(R.string.tg7);
			case 8:
				return context.getResources().getString(R.string.tg8);
			case 9:
				return context.getResources().getString(R.string.tg9);
			case 0:
				return context.getResources().getString(R.string.tg0);
		}
		return "?";
	}
}