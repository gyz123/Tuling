package com.hhuc.sillyboys.tuling.entity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ScrollView;

import com.hhuc.sillyboys.tuling.LoginActivity;


public class ObservableScrollView extends ScrollView {
	public static final String TAG = "observeable.scrollview";
	private static final int ItemHeight = 60; // defined in xml

	private Context mContext = null;
	private static final int EvObsTimerout = 1234;
	private static final int ObsTimer = 20;
	private int stepPixs = 60;
	private int fastMovePixs = 6;

	private ScrollViewListener scrollViewListener = null;
	private int nowY = 0;
	private int lastY = 0;

	public ObservableScrollView(Context context) {
		super(context);
		mContext = context;
	}

	public ObservableScrollView(Context context, AttributeSet attrs,
								int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
	}

	public ObservableScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
	}

	public void setScrollViewListener(ScrollViewListener scrollViewListener) {
		this.scrollViewListener = scrollViewListener;
	}

	public int getTargetIdx() {
		return computerTarget();
	}

	public void start() {
		DisplayMetrics metric = new DisplayMetrics();
		((LoginActivity) mContext).getWindowManager().getDefaultDisplay()
				.getMetrics(metric);
		float density = metric.density;
		stepPixs = (int) (ItemHeight * density + 0.5f);
		fastMovePixs = stepPixs / 10;

		nowY = 0;
		lastY = 0;
		sHandler.sendEmptyMessageDelayed(EvObsTimerout, ObsTimer);
	}

	public int stop() {
		sHandler.removeMessages(EvObsTimerout);
		return computerTarget();
	}

	@SuppressLint("HandlerLeak")
	private Handler sHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == EvObsTimerout) {
				if (lastY == nowY) {
					if (nowY % stepPixs != 0) {
						int target = computerTarget();
						int dist = target * stepPixs - nowY;
						if (Math.abs(dist) > fastMovePixs) {
							ObservableScrollView.this.scrollBy(0, dist / 3);
						} else {
							ObservableScrollView.this.scrollBy(0,
									dist / Math.abs(dist));
						}
					} else {
						scrollViewListener.onScrollStopped(
								ObservableScrollView.this, 0, nowY);
					}
				} else {
					lastY = nowY;
				}

				sHandler.sendEmptyMessageDelayed(EvObsTimerout, ObsTimer);
			}
		}
	};

	private int computerTarget() {
		int tmp = (int) (nowY / stepPixs);
		int dist1 = nowY - tmp * stepPixs;
		int target = dist1 > (stepPixs / 2) ? tmp + 1 : tmp;
		return target;
	}

	@Override
	protected void onScrollChanged(int x, int y, int oldx, int oldy) {
		super.onScrollChanged(x, y, oldx, oldy);
		if (scrollViewListener != null) {
			scrollViewListener.onScrollChanged(this, x, y, oldx, oldy);
		}
		nowY = y;
	}

}