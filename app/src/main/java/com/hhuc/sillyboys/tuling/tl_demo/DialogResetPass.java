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
import com.hhuc.sillyboys.tuling.entity.MsgCode;
import com.hhuc.sillyboys.tuling.R;

import java.util.HashMap;

public class DialogResetPass extends Dialog {
	private Context context;
	private int resource;
	private DialogResetPass self;
	private Handler handler;
	private int userId = 0;
	private String uAccount = null;
	private HashMap<String, String> outputInfo = new HashMap<String, String>();

	public DialogResetPass(Context context, int theme, int resource,
						   Handler handler, int uid, String uAccount) {
		super(context, theme);
		this.context = context;
		this.resource = resource;
		this.self = this;
		this.handler = handler;
		this.userId = uid;
		this.uAccount = uAccount;
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

		EditText etAccount = (EditText) findViewById(R.id.user_phoneno);
		etAccount.setHint(uAccount);

		((Button) findViewById(R.id.loginButton))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String authCode = ((EditText) findViewById(R.id.auth_code))
								.getText().toString();
						String pw = ((EditText) findViewById(R.id.rp_pass1))
								.getText().toString();
						String pw2 = ((EditText) findViewById(R.id.rp_pass2))
								.getText().toString();
						TextView errorInfo = (TextView) findViewById(R.id.loginErrInfo);

						if (!TextUtils.isEmpty(authCode)
								&& authCode.length() == 6
								&& !TextUtils.isEmpty(pw) && pw.length() >= 4
								&& !TextUtils.isEmpty(pw2) && pw.equals(pw2)) {
							outputInfo.put(RegisterUser.KeyPassword,
									API.md5(pw));
							outputInfo.put(RegisterUser.KeyAccount, uAccount);
							outputInfo.put(RegisterUser.KeyAuthCode, authCode);
							handler.obtainMessage(MsgCode.ASKFORRESETPW, 0,
									userId, outputInfo).sendToTarget();
							self.dismiss();
							return;
						}

						if (authCode == null || authCode.length() != 6) {
							errorInfo.setText(context.getResources().getString(R.string.auth_code_error));
							return;
						}
						if (pw == null || pw2 == null || pw.length() < 4
								|| pw2.length() < 4) {
							if (!pw.equals(pw2))
								errorInfo.setText(context.getResources().getString(R.string.unidentiry_pass));
							errorInfo.setText(context.getResources().getString(R.string.need_passwd));
							return;
						}
					}
				});
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		handler.obtainMessage(MsgCode.ASKFOREXIT).sendToTarget();
	}
}