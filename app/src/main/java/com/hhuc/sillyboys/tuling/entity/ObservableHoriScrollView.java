package com.hhuc.sillyboys.tuling.entity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.HorizontalScrollView;

public class ObservableHoriScrollView extends HorizontalScrollView {
	public static final String TAG = "observeable.scrollview";

	private static final int EvObsTimerout = 1234;
	private static final int ObsTimer = 20;
	private int stepPixs = 60;
	private int fastMovePixs = 6;
	private boolean timerStarted = false;

	private ScrollViewListener scrollViewListener = null;
	private int nowX = 0;
	private int lastX = 0;

	public ObservableHoriScrollView(Context context) {
		super(context);
	}

	public ObservableHoriScrollView(Context context, AttributeSet attrs,
									int defStyle) {
		super(context, attrs, defStyle);
	}

	public ObservableHoriScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setScrollViewListener(ScrollViewListener scrollViewListener) {
		this.scrollViewListener = scrollViewListener;
	}

	public void setTargetIdx(int idx) {
		int diff = (int) (stepPixs / 2 - 0.5f);
		int to = idx * stepPixs;
		if (Math.abs(nowX - to) < fastMovePixs) {
			this.scrollTo(to, 0);
		} else {
			if (nowX > to)
				this.scrollTo(to + diff, 0);
			else
				this.scrollTo(to - diff, 0);
		}
	}

	public int getTargetIdx() {
		return computerTarget();
	}

	public void start(int w) {
		stepPixs = w;
		fastMovePixs = stepPixs / 10;
		timerStarted = false;
	}

	@SuppressLint("HandlerLeak")
	private Handler sHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (msg.what == EvObsTimerout) {
				if (lastX == nowX) { // scroll stopped.
					if (nowX % stepPixs != 0) {
						int target = computerTarget();
						int dist = target * stepPixs - nowX;
						if (Math.abs(dist) > fastMovePixs) {
							ObservableHoriScrollView.this.scrollBy(dist / 5, 0);
						} else if (Math.abs(dist) > 5) {
							ObservableHoriScrollView.this.scrollBy(2 * dist
									/ Math.abs(dist), 0);
						} else {
							ObservableHoriScrollView.this.scrollBy(
									dist / Math.abs(dist), 0);
						}
						sHandler.sendEmptyMessageDelayed(EvObsTimerout,
								ObsTimer);
					} else {
						timerStarted = false;
						scrollViewListener.onScrollStopped(
								ObservableHoriScrollView.this, nowX, 0);
						Log.i(TAG, "channels scrollor get: " + lastX + " idx: "
								+ lastX / stepPixs);
					}
				} else {
					lastX = nowX;
					sHandler.sendEmptyMessageDelayed(EvObsTimerout, ObsTimer);
				}
			}
		}
	};

	private int computerTarget() {
		int tmp = (int) (nowX / stepPixs);
		int dist1 = nowX - tmp * stepPixs;
		int target = dist1 > (stepPixs / 2) ? tmp + 1 : tmp;

		return target;
	}

	@Override
	protected void onScrollChanged(int x, int y, int oldx, int oldy) {
		super.onScrollChanged(x, y, oldx, oldy);
		if (scrollViewListener != null) {
			scrollViewListener.onScrollChanged(this, x, y, oldx, oldy);
		}
		nowX = x;

		if (!timerStarted) {
			timerStarted = true;
			sHandler.sendEmptyMessageDelayed(EvObsTimerout, ObsTimer);
		}
	}
}