package com.aiseminar.EasyPR;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;


import org.apache.cordova.CordovaActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class CameraActivity extends CordovaActivity implements SurfaceHolder.Callback, View.OnClickListener {

    SurfaceView mSvCamera;


    ImageView mIvCapturePhoto;

    ViewfinderView maskView;
    RadioGroup rgType;
    LinearLayout back;

    private static final String TAG = CameraActivity.class.getSimpleName();

    private int cameraPosition = 0; // 0表示后置，1表示前置

    private SurfaceHolder mSvHolder;
    private Camera mCamera;
    private Camera.CameraInfo mCameraInfo;
    private MediaPlayer mShootMP;
    private PlateRecognizer mPlateRecognizer;

    private int type = 5;

    private Intent resultIntent;
    private Rect rect;
    private float screenProp = -1.0f;
    private boolean isReady;
    private int failcount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mPlateRecognizer = new PlateRecognizer(this);

        mSvCamera = (SurfaceView) findViewById(R.id.svCamera);
        mIvCapturePhoto = (ImageView) findViewById(R.id.ivCapturePhoto);
        maskView = (ViewfinderView) findViewById(R.id.maskView);
        rgType = (RadioGroup) findViewById(R.id.rec_type);
        back = (LinearLayout) findViewById(R.id.back);
        resultIntent = new Intent();
        initData();
        initListener();
        copySD();
    }

    private void copySD() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                WriteToSD.CopyAssets(CameraActivity.this, "model", WriteToSD.filePath);
                isReady=true;
            }
        }).start();
    }

    private void initListener() {
        mIvCapturePhoto.setOnClickListener(this);
        back.setOnClickListener(this);
        rgType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch (checkedId) {
                    case R.id.rec_five:
                        type = 5;
                        break;
                    case R.id.rec_six:
                        type = 6;
                        break;
                }
            }
        });

        maskView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                rect = maskView.getFrame();
                if (maskView.getWidth() != 0) {
                    screenProp = maskView.getHeight() / maskView.getWidth();
                }


            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        if (this.checkCameraHardware(this) && (mCamera == null)) {
            // 打开camera
            mCamera = getCamera();
            // 设置camera方向
            mCameraInfo = getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK);
            if (null != mCameraInfo) {
                adjustCameraOrientation();
            }

            if (mSvHolder != null) {
                setStartPreview(mCamera, mSvHolder);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        /**
         * 记得释放camera，方便其他应用调用
         */
        releaseCamera();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    /**
     * 初始化相关data
     */
    private void initData() {
        // 获得句柄
        mSvHolder = mSvCamera.getHolder(); // 获得句柄
        // 添加回调
        mSvHolder.addCallback(this);
    }

    private Camera getCamera() {
        Camera camera = null;
        try {
            camera = Camera.open();
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
            camera = null;
            Log.e(TAG, "Camera is not available (in use or does not exist)");
        }
        return camera;
    }

    private Camera.CameraInfo getCameraInfo(int facing) {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == facing) {
                return cameraInfo;
            }
        }
        return null;
    }

    private void adjustCameraOrientation() { // 调整摄像头方向
        if (null == mCameraInfo || null == mCamera) {
            return;
        }

        int orientation = this.getWindowManager().getDefaultDisplay().getOrientation();
        int degrees = 0;

        switch (orientation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else {
            // back-facing
            result = (mCameraInfo.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    /**
     * 释放mCamera
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();// 停掉原来摄像头的预览
            mCamera.release();
            mCamera = null;
        }
    }


    public void onClick(View view) {
        switch (view.getId()) {
            case 999: // R.id.id_switch_camera_btn:
                // 切换前后摄像头
                int cameraCount = 0;
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                cameraCount = Camera.getNumberOfCameras();// 得到摄像头的个数

                for (int i = 0; i < cameraCount; i++) {
                    Camera.getCameraInfo(i, cameraInfo);// 得到每一个摄像头的信息
                    if (cameraPosition == 1) {
                        // 现在是后置，变更为前置
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                            /**
                             * 记得释放camera，方便其他应用调用
                             */
                            releaseCamera();
                            // 打开当前选中的摄像头
                            mCamera = Camera.open(i);
                            // 通过surfaceview显示取景画面
                            setStartPreview(mCamera, mSvHolder);
                            cameraPosition = 0;
                            break;
                        }
                    } else {
                        // 现在是前置， 变更为后置
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                            /**
                             * 记得释放camera，方便其他应用调用
                             */
                            releaseCamera();
                            mCamera = Camera.open(i);
                            setStartPreview(mCamera, mSvHolder);
                            cameraPosition = 1;
                            break;
                        }
                    }

                }
                break;

            case R.id.ivCapturePhoto:
                // 拍照,设置相关参数
                if(isReady){
                    try {
                        mCamera.takePicture(shutterCallback, null, jpgPictureCallback);
                    } catch (Exception e) {
                        Log.d(TAG, e.getMessage());
                    }
                }else{
                    Toast.makeText(this,"还没有加载好,稍等",Toast.LENGTH_SHORT).show();
                }


                break;
            case R.id.back:
                finish();
                break;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // 打开camera
        // mCamera = getCamera();
        setStartPreview(mCamera, mSvHolder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mSvHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        setStartPreview(mCamera, mSvHolder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // 当surfaceview关闭时，关闭预览并释放资源
        /**
         * 记得释放camera，方便其他应用调用
         */
        releaseCamera();
        holder = null;
        mSvCamera = null;
    }

    /**
     * TakePicture回调
     */
    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
            // shootSound();
            mCamera.setOneShotPreviewCallback(previewCallback);
        }
    };

    Camera.PictureCallback rawPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            camera.startPreview();
        }
    };

    Camera.PictureCallback jpgPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            camera.startPreview();

            File pictureFile = FileUtil.getOutputMediaFile(FileUtil.FILE_TYPE_IMAGE);
            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions: ");
                return;
            }
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                // 照片转方向
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                Bitmap normalBitmap = BitmapUtil.createRotateBitmap(bitmap);
                normalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.close();
                CameraActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + pictureFile.getAbsolutePath())));
                Toast.makeText(CameraActivity.this, "图像已保存。", Toast.LENGTH_SHORT).show();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    /**
     * Check if this device has a camera
     */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    /**
     * activity返回式返回拍照图片路径
     *
     * @param mediaFile
     */
    private void returnResult(File mediaFile) {
//        Intent intent = new Intent();
//        intent.setData(Uri.fromFile(mediaFile));
//        this.setResult(RESULT_OK, intent);
        this.finish();
    }

    /**
     * 设置camera显示取景画面,并预览
     *
     * @param camera
     */
    private void setStartPreview(Camera camera, SurfaceHolder holder) {
        try {
            if (camera != null) {
                Camera.Parameters params = camera.getParameters();
               /* DisplayMetrics metric = new DisplayMetrics();
                Camera.Size previewSize = CameraParamUtil.getInstance().getPreviewSize(params
                        .getSupportedPreviewSizes(), 1000, screenProp);
                Camera.Size pictureSize = CameraParamUtil.getInstance().getPictureSize(params
                        .getSupportedPictureSizes(), 1200, screenProp);

                params.setPreviewSize(previewSize.width, previewSize.height);

                params.setPictureSize(pictureSize.width, pictureSize.height);*/
                // 自动对焦
                if (CameraParamUtil.getInstance().isSupportedFocusMode(
                        params.getSupportedFocusModes(),
                        Camera.Parameters.FOCUS_MODE_AUTO)) {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
                if (CameraParamUtil.getInstance().isSupportedPictureFormats(params.getSupportedPictureFormats(),
                        ImageFormat.JPEG)) {
                    params.setPictureFormat(ImageFormat.JPEG);
                    params.setJpegQuality(100);
                }

                camera.setParameters(params);
                camera.setPreviewDisplay(holder);
                camera.startPreview();

            }

        } catch (IOException e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * 获取Preview界面的截图，并存储
     */
    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            // 获取Preview图片转为bitmap并旋转
            Camera.Size size = mCamera.getParameters().getPreviewSize(); //获取预览大小
            final int w = size.width;  //宽度
            final int h = size.height;
            final YuvImage image = new YuvImage(data, ImageFormat.NV21, w, h, null);
            // 转Bitmap
            ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
            if (!image.compressToJpeg(new Rect(0, 0, w, h), 100, os)) {
                return;
            }
            byte[] tmp = os.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(tmp, 0, tmp.length);
            Bitmap rotatedBitmap = BitmapUtil.createRotateBitmap(bitmap);

            cropBitmapAndRecognize(rotatedBitmap);
        }
    };

    public void cropBitmapAndRecognize(Bitmap originalBitmap) {
        // 裁剪出关注区域
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;  // 屏幕宽度（像素）
        int height = metric.heightPixels;  // 屏幕高度（像素）
        Bitmap sizeBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, true);

        int rectWidth = maskView.getWidth();
        int rectHight = maskView.getHeight();
        int[] location = new int[2];
        maskView.getLocationOnScreen(location);
        if (rect != null) {
            rectWidth = dip2px(this, 250) + 20;
            rectHight = dip2px(this, 150 + 20);
            location[0] = rect.left - 10;
            location[1] += rect.top + dip2px(this, 49) - 10;
        }
        Bitmap normalBitmap = Bitmap.createBitmap(sizeBitmap, location[0], location[1], rectWidth, rectHight);

        // 保存图片并进行车牌识别
        File pictureFile = FileUtil.getOutputMediaFile(FileUtil.FILE_TYPE_PLATE);
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions: ");
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            normalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            // 最后通知图库更新
            CameraActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + pictureFile.getAbsolutePath())));

            // 进行车牌识别
            String plate = "";
            if (type == 6) {
                plate = mPlateRecognizer.recognizes(pictureFile.getAbsolutePath());
            } else {
                plate = mPlateRecognizer.recognize(pictureFile.getAbsolutePath());
            }
            if (null != plate && !plate.equals("")) {
                resultIntent.putExtra("result", plate);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                failcount++;
                if(failcount>=2){
                    resultIntent.putExtra("result", "识别失败");
                    setResult(RESULT_OK, resultIntent);
                    finish();
                }else{
                    Toast.makeText(this,"识别失败,清调整角度",Toast.LENGTH_SHORT).show();
                }

            }


        } catch (FileNotFoundException e) {
            resultIntent.putExtra("result", "未找到文件");
            finish();
        } catch (IOException e) {
            resultIntent.putExtra("result", "io异常");
            finish();
        } catch (Exception e) {
            resultIntent.putExtra("result", "是被异常");
            finish();
        }
    }

    public void handleFocus(final Context context, final float x, final float y) {
        if (mCamera == null) {
            return;
        }
        final Camera.Parameters params = mCamera.getParameters();
        Rect focusRect = calculateTapArea(x, y, 1f, context);
        if (params.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<Camera.Area>();
            focusAreas.add(new Camera.Area(focusRect, 800));
            params.setFocusAreas(focusAreas);
        } else {

            return;
        }
        final String currentFocusMode = params.getFocusMode();
        try {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            mCamera.setParameters(params);
            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        Camera.Parameters params = camera.getParameters();
                        params.setFocusMode(currentFocusMode);
                        camera.setParameters(params);

                    } else {
                        handleFocus(context, x, y);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "autoFocus failer");
        }
    }


    private static Rect calculateTapArea(float x, float y, float coefficient, Context context) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) (x / ScreenUtils.getScreenHeight(context) * 2000 - 1000);
        int centerY = (int) (y / ScreenUtils.getScreenWidth(context) * 2000 - 1000);
//        Log.i("CJT", "FocusArea centerX = " + centerX + " , centerY = " + centerY);
        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);
        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF
                .bottom));
    }

    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getPointerCount() == 1) {
                    //显示对焦指示器
                    handleFocus(this, event.getX(), event.getY());
                }
                break;
        }
        return true;
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

}
