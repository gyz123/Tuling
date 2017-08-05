/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hhuc.sillyboys.tuling.zxing.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.google.zxing.Result;
import com.hhuc.sillyboys.tuling.R;
import com.hhuc.sillyboys.tuling.broadcast.BroadcastActivity;
import com.hhuc.sillyboys.tuling.zxing.camera.CameraManager;
import com.hhuc.sillyboys.tuling.zxing.decode.DecodeThread;
import com.hhuc.sillyboys.tuling.zxing.utils.BeepManager;
import com.hhuc.sillyboys.tuling.zxing.utils.CaptureActivityHandler;
import com.hhuc.sillyboys.tuling.zxing.utils.InactivityTimer;

import java.io.IOException;
import java.lang.reflect.Field;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly,
 * shows feedback as the image processing is happening, and then overlays the
 * results when a scan is successful.
 * <p/>
 * 此页面打开摄像头，并在后台线程上进行实际扫码。
 * 通过取景器来帮助用户获取正确条码，
 * 显示图像正在处理发生的事情，当获取成功后覆盖扫描结果。
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public final class CaptureActivity extends Activity implements SurfaceHolder.Callback
{

    private static final String TAG = "CaptureActivity";

    // 管理摄像头
    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;

    // 布局文件
    private SurfaceView scanPreview = null;
    // 外层布局容器
    private RelativeLayout scanContainer;
    // 扫描框布局容器
    private RelativeLayout scanCropView;
    private ImageView scanLine;

    private Rect mCropRect = null;
    private boolean isHasSurface = false;

    private static final int SET_SUCCESS = 0;
    private static final int SET_FAIL = 1;
    private Handler handlerForOrder = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case SET_SUCCESS:
                    Log.d("test","确认订单成功");
                    break;
                case SET_FAIL:
                    Log.d("test","确认订单失败");
                    break;
                default:
                    break;
            }
        }
    };

    public Handler getHandler()
    {
        return handler;
    }

    public CameraManager getCameraManager()
    {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        Log.d(TAG, "onCreate()");

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_capture);

        scanPreview = (SurfaceView) findViewById(R.id.capture_preview);
        scanContainer = (RelativeLayout) findViewById(R.id.capture_container);
        scanCropView = (RelativeLayout) findViewById(R.id.capture_crop_view);
        scanLine = (ImageView) findViewById(R.id.capture_scan_line);

        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);

        TranslateAnimation animation = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f, Animation
                .RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                0.9f);
        animation.setDuration(4500);
        animation.setRepeatCount(-1);
        animation.setRepeatMode(Animation.RESTART);
        scanLine.startAnimation(animation);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        Log.d(TAG, "onResume()");

        // CameraManager must be initialized here, not in onCreate(). This is
        // necessary because we don't
        // want to open the camera driver and measure the screen size if we're
        // going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the
        // wrong size and partially
        // off screen.
        cameraManager = new CameraManager(getApplication());

        handler = null;

        if (isHasSurface)
        {
            // The activity was paused but not stopped, so the surface still
            // exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(scanPreview.getHolder());
        } else
        {
            // Install the callback and wait for surfaceCreated() to init the
            // camera.
            scanPreview.getHolder().addCallback(this);
        }

        inactivityTimer.onResume();
    }

    @Override
    protected void onPause()
    {
        Log.d(TAG, "onPause()");

        if (handler != null)
        {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();
        if (!isHasSurface)
        {
            scanPreview.getHolder().removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "onDestroy()");

        inactivityTimer.shutdown();
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder)
    {
        Log.d(TAG, "surfaceCreated");

        if (holder == null)
        {
            Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!isHasSurface)
        {
            isHasSurface = true;
            initCamera(holder);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder)
    {
        Log.d(TAG, "surfaceCreated");

        isHasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
    {
        Log.d(TAG, "surfaceChanged");

    }

    /**
     * A valid barcode has been found, so give an indication of success and show
     * the results.
     * <p/>
     * 当一个有效的条码被发现，所以给出一个成功和展示的迹象结果。
     *
     * @param rawResult The contents of the barcode.
     * @param bundle    The extras
     */
    public void handleDecode(Result rawResult, Bundle bundle)
    {

        Log.d(TAG, "handleDecode");

        inactivityTimer.onActivity();
        beepManager.playBeepSoundAndVibrate();

        Log.d("CaptureActivity", mCropRect.width()/3*2 + "---" + mCropRect.height()/3*2);

        String result = rawResult.getText().toString();
        Log.d("CaptureActivity", "扫码结果： " + result);
        //  处理扫码结果
        Intent broadcastIntent = new Intent(this, BroadcastActivity.class);
        broadcastIntent.putExtra("compactId", result)
                .putExtra("cname", "XX大学广播台")
                .putExtra("type", "broadcast");
        startActivity(broadcastIntent);
    }

    private void confirmOrder(String subscribenum){
        OkHttpClient client = new OkHttpClient();
        FormBody formBody = new FormBody.Builder()
                .add("num", subscribenum)
                .build();
        Request request = new Request.Builder()
                .url("http://www.iotesta.com.cn/library/setstatus.action")
                .post(formBody)
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Message message = new Message();
                message.what = SET_FAIL;
                handlerForOrder.sendMessage(message);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String result = response.body().string();
                Message message = new Message();
                message.what = SET_SUCCESS;
                handlerForOrder.sendMessage(message);
            }
        });
    }

    private void initCamera(SurfaceHolder surfaceHolder)
    {
        Log.d(TAG, "initCamera");

        if (surfaceHolder == null)
        {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen())
        {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try
        {
            cameraManager.openDriver(surfaceHolder);
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null)
            {
                handler = new CaptureActivityHandler(CaptureActivity.this, cameraManager, DecodeThread.ALL_MODE);
            }

            initCrop();
        } catch (IOException ioe)
        {
            Log.w(TAG, ioe);
            displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e)
        {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit()
    {
        Log.d(TAG, "displayFrameworkBugMessageAndExit");

        // camera error
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.app_name));
        builder.setMessage("Camera error");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {

            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                finish();
            }

        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener()
        {

            @Override
            public void onCancel(DialogInterface dialog)
            {
                finish();
            }
        });
        builder.show();
    }

    public void restartPreviewAfterDelay(long delayMS)
    {
        if (handler != null)
        {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
    }

    public Rect getCropRect()
    {
//        Log.d(TAG, "getCropRect");

        return mCropRect;
    }

    /**
     * 初始化截取的矩形区域
     */
    private void initCrop()
    {
        Log.d(TAG, "initCrop");


        /** 手机分辨率 **/
        int cameraWidth = cameraManager.getCameraResolution().y;
        int cameraHeight = cameraManager.getCameraResolution().x;

        Log.d(TAG, "我用的AutoLayout布局，设置模版是1280*720，要显示自己手机像素，请删除打印log中的‘/3*2’");
        Log.d(TAG, "手机分辨率：cameraWidth(px)" + cameraWidth/3*2 + "---" + "cameraHeight(px)" + cameraHeight/3*2);


        /** 获取布局中扫描框的位置信息（绝对位置:以像素为标准） */
        int[] location = new int[2];
        scanCropView.getLocationInWindow(location);
        Log.d(TAG, "location1(px):" + String.valueOf(location[0]/3*2) + "---" + "location2(px):" + String.valueOf(location[1]/3*2));


        int cropLeft = location[0];
        int cropTop = location[1] - getStatusBarHeight();
        Log.d(TAG, "cropLeft(px):" + String.valueOf(cropLeft/3*2) + "---" + "cropTop(px):" + String.valueOf(cropTop/3*2));


        /** 获取扫描框布局的宽高 **/
        int cropWidth = scanCropView.getWidth();
        int cropHeight = scanCropView.getHeight();
        Log.d(TAG, "扫描框布局：cropWidth(px):" + cropWidth/3*2 + "---" + "cropHeight(px):" + cropHeight/3*2);

        /** 获取最外层容器布局的宽高 */
        int containerWidth = scanContainer.getWidth();
        int containerHeight = scanContainer.getHeight();
        Log.d(TAG, "外层容器布局containerWidth(px):" + containerWidth/3*2 + "---" + "containerHeight(px):" + containerHeight/3*2);

        /** 计算最终截取的矩形的左上角顶点x坐标 */
//        int x = cropLeft * cameraWidth / containerWidth;
        int x = cropLeft;

        /** 计算最终截取的矩形的左上角顶点y坐标 */
//        int y = cropTop * cameraHeight / containerHeight;
        int y = cropTop;

        Log.d(TAG, "计算最终截取的矩形x(px):" + String.valueOf(x/3*2) + "---" + "y(px):" + String.valueOf(y/3*2));


        /** 计算最终截取的矩形的宽度 */
//        int width = cropWidth * cameraWidth / containerWidth;
        int width = cropWidth;

        /** 计算最终截取的矩形的高度 */
//        int height = cropHeight * cameraHeight / containerHeight;
        int height = cropHeight;

        Log.d(TAG, "计算最终截取的矩形width(px):" + String.valueOf(width/3*2) + "---" + "height(px):" + String.valueOf(height/3*2));


        /** 生成最终的截取的矩形 */
        mCropRect = new Rect(x, y, width + x, height + y);
    }

    // 状态栏高度获取方法
    private int getStatusBarHeight()
    {
        Log.d(TAG, "getStatusBarHeight");

        try
        {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return 0;
    }


}