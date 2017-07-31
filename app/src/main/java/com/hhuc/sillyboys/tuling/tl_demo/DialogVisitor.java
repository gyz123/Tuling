package com.hhuc.sillyboys.tuling.tl_demo;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
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

public class DialogVisitor extends Dialog {
	public static final String TAG = "visitor.login";
	private Context context;
	private int resource;
	private String myAccount = null;
	private String myNick = null;
	private DialogVisitor self;
	private Handler handler;
	private HashMap<String, String> outputInfo = new HashMap<String, String>();

	public DialogVisitor(Context context, int theme, int resource,
						 Handler handler, String account, String nick) {
		super(context, theme);
		this.context = context;
		this.resource = resource;
		this.myAccount = account;
		this.myNick = nick;
		this.self = this;
		this.handler = handler;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.setContentView(resource);
		LinearLayout linearLayout = ((LinearLayout) findViewById(R.id.visitor_login));
		linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
		LayoutParams layoutParams = linearLayout.getLayoutParams();
		DisplayMetrics dm = context.getResources().getDisplayMetrics();
		if (dm.widthPixels > dm.heightPixels)
			layoutParams.width = (int) (dm.widthPixels * 0.6);// -
																// dm.densityDpi/1;
		else
			layoutParams.width = (int) (dm.widthPixels * 0.8);
		linearLayout.setLayoutParams(layoutParams);

		Button nextB = (Button) findViewById(R.id.login_next);
		final String dfltNick = getNickHint();
		EditText etNick = (EditText) findViewById(R.id.visitor_nick);
		if (myAccount != null && myNick != null) {
			TextView tvVisit = (TextView) findViewById(R.id.visitor_intro);
			tvVisit.setText(context.getResources().getString(R.string.visitor_welcome));
			nextB.setText("GO");
			etNick.setHint(myNick);
		} else {
			etNick.setHint(dfltNick);
		}

		nextB.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!TextUtils.isEmpty(myAccount) && !TextUtils.isEmpty(myNick)) {
					outputInfo.put(RegisterUser.KeyAccount, myAccount);
					outputInfo.put(RegisterUser.KeyPassword, API.md5("888888"));
					handler.obtainMessage(MsgCode.ASKFORLOGIN, 2, 0, outputInfo)
							.sendToTarget();
				} else {
					String visitor = ((EditText) findViewById(R.id.visitor_nick))
							.getText().toString();
					if (TextUtils.isEmpty(visitor))
						visitor = dfltNick;
					if (Utils.getCharacterNum(visitor) < AccountApi.MINNICKLEN) {
						TextView tvVisit = (TextView) findViewById(R.id.visitor_intro);
						tvVisit.setText(tvVisit.getText() + context.getResources().getString(R.string.visitor_nick_err));
						return;
					}
					outputInfo.put(RegisterUser.KeyNick, visitor);
					handler.obtainMessage(MsgCode.CREATEVISITOR, outputInfo)
							.sendToTarget();
				}
				self.dismiss();
			}
		});

		((TextView) (findViewById(R.id.user_entry)))
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
		return context.getResources().getString(R.string.luren) + int2tiangan(stage);
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
