package com.hhuc.sillyboys.tuling.entity;

import android.widget.FrameLayout;

public interface ScrollViewListener {

	void onScrollChanged(FrameLayout scrollView, int x, int y, int oldx,
						 int oldy);

	void onScrollStopped(FrameLayout scrollView, int x, int y);

}