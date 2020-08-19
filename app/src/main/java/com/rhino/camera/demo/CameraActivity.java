package com.rhino.camera.demo;

import android.app.Activity;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.rhino.camera.CameraTextureView;
import com.rhino.camera.utils.BitmapUtils;
import com.rhino.camera.utils.CameraPermissionUtils;
import com.rhino.camera.utils.PrimaryUtils;
import com.rhino.camera.demo.databinding.ActivityCameraBinding;
import com.rhino.log.LogUtils;
import com.rhino.log.crash.CrashHandlerUtils;
import com.rhino.log.crash.DefaultCrashHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;


public class CameraActivity extends Activity implements Camera.PreviewCallback {

    private ActivityCameraBinding dataBinding;
    private File outPutFile;

    /**
     * Second for count down time
     */
    private int countDownSecond = 10;
    /**
     * The timer for record
     */
    private Timer mTimer;
    /**
     * The timer for record
     */
    private CountDownTimer mCountDownTimer;
    /**
     * The second for record
     */
    private int mRecordSecond = -1;
    /**
     * Whether record mode
     */
    private boolean record = false;
    /**
     * The current phone degree
     */
    private int currentPhoneDegree = 0;
    /**
     * 视频流预览测试开关
     */
    private boolean previewTestEnable = false;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        LogUtils.init(getApplicationContext(), BuildConfig.DEBUG, false);
        CrashHandlerUtils.getInstance().init(getApplicationContext(), new DefaultCrashHandler());

        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_camera);
        initView();
    }


    public void initView() {
        // 隐藏底部虚拟按键
//        hideBottomUIMenu();
        dataBinding.btBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        dataBinding.btSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dataBinding.cameraTextureView.isFrontCamera()) {
                    dataBinding.cameraTextureView.setBackCamera();
                } else {
                    dataBinding.cameraTextureView.setFrontCamera();
                }
            }
        });
        dataBinding.llStartRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (record) {
                    onClickRecord();
                } else {
                    onClickCapture();
                }
            }
        });
        dataBinding.tvTabCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                record = false;
                dataBinding.tvStartRecord.setText("拍照");
                dataBinding.tvTabCapture.setSelected(true);
                dataBinding.tvTabRecord.setSelected(false);
                dataBinding.cameraTextureView.stopRecord();
                stopRecordTimer();
            }
        });
        dataBinding.tvTabRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                record = true;
                dataBinding.tvStartRecord.setText("录像");
                dataBinding.tvTabCapture.setSelected(false);
                dataBinding.tvTabRecord.setSelected(true);
            }
        });
        dataBinding.tvTabCapture.setSelected(true);
        CameraPermissionUtils.checkVideoRecordPermission(this);
        init();
    }

    @Override
    public void onPause() {
        super.onPause();
        dataBinding.cameraTextureView.stopPreview();
        stopRecordTimer();
        dataBinding.cameraTextureView.stopRecord();
        if (record) {
            dataBinding.tvStartRecord.setText("录像");
        } else {
            dataBinding.tvStartRecord.setText("拍照");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        dataBinding.cameraTextureView.startPreview();
        if (record) {
            dataBinding.tvStartRecord.setText("录像");
        } else {
            dataBinding.tvStartRecord.setText("拍照");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!CameraPermissionUtils.checkHasAllPermission(permissions, grantResults)) {
            showToast("需要手动设置权限");
            finish();
            return;
        }
        init();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();
            dataBinding.focusView.moveToPosition(x, y);
            dataBinding.focusView.startFocus();
            dataBinding.focusView.postDelayHideFocusView(5000);
            dataBinding.cameraTextureView.startFocus(x, y, dataBinding.rlContainer.getWidth(), dataBinding.rlContainer.getHeight(), new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    if (b) {
                        dataBinding.focusView.focusSuccess();
                    } else {
                        dataBinding.focusView.focusSuccess();
                    }
                    dataBinding.focusView.postDelayHideFocusView();
                }
            });
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (previewTestEnable && dataBinding.cameraTextureView.getCamera() != null) {
            final Camera.Parameters parameters = camera.getParameters();
            Camera.Size previewSize = parameters.getPreviewSize();
            YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), previewSize.width, previewSize.height, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, out);
            byte[] bytes = out.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            // TODO 测试
            BitmapUtils.saveBitmap(bitmap, "pBitmap.jpg");

            // 旋转图片动作
            Matrix matrix = new Matrix();
            matrix.postRotate(dataBinding.cameraTextureView.getRotateDegree());
            // 创建新的图片
            Bitmap matrixBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            // TODO 测试
            BitmapUtils.saveBitmap(matrixBitmap, "pMatrixBitmap.jpg");

            previewTestEnable = false;
        }
    }

    /**
     * 初始化
     */
    private void init() {
        WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();

        dataBinding.cameraTextureView.setEnableOrientationListener(true);
        dataBinding.cameraTextureView.setFitStyle(CameraTextureView.FIT_STYLE_FILL_WIDTH_HEIGHT);
        dataBinding.cameraTextureView.setExpectPreviewSize(1080, 1920);
        dataBinding.cameraTextureView.setPreviewCallback(this);
        dataBinding.cameraTextureView.setBackCamera();
        dataBinding.cameraTextureView.setOnPhoneDegreeChangeListener(new CameraTextureView.OnPhoneDegreeChangeListener() {
            @Override
            public void onPhoneDegreeChanged(int phoneDegree) {
                if (currentPhoneDegree == phoneDegree) {
                    return;
                }
                int fromDegree = 0;
                int toDegree = 0;
                if (phoneDegree == 270) {
                    if (currentPhoneDegree == 0) {
                        fromDegree = 0;
                        toDegree = 90;
                    } else if (currentPhoneDegree == 90) {
                        fromDegree = -90;
                        toDegree = 90;
                    }
                } else if (phoneDegree == 90) {
                    if (currentPhoneDegree == 0) {
                        fromDegree = 0;
                        toDegree = -90;
                    } else if (currentPhoneDegree == 270) {
                        fromDegree = 90;
                        toDegree = -90;
                    }
                } else if (phoneDegree == 0) {
                    if (currentPhoneDegree == 90) {
                        fromDegree = -90;
                        toDegree = 0;
                    } else if (currentPhoneDegree == 270) {
                        fromDegree = 90;
                        toDegree = 0;
                    }
                } else {
                    return;
                }
                currentPhoneDegree = phoneDegree;

                AnimUtils.rotate(dataBinding.llStartRecord, fromDegree, toDegree, null);
                AnimUtils.rotate(dataBinding.btBack, fromDegree, toDegree, null);
                AnimUtils.rotate(dataBinding.btSwitchCamera, fromDegree, toDegree, null);
            }
        });
    }

    /**
     * 录像
     */
    private void onClickRecord() {
        if (dataBinding.cameraTextureView.isRecording()) {
            stopRecord();
        } else {
            startRecord();
        }
    }

    private void startRecord() {
        File dir = new File(Environment.getExternalStorageDirectory(), "Video");
        if (!dir.exists() && !dir.mkdirs()) {
            return;
        }
        outPutFile = new File(dir, PrimaryUtils.createPrimary() + ".mp4");
        dataBinding.cameraTextureView.startRecord(outPutFile.getAbsolutePath());
        dataBinding.tvStartRecord.setText("停止");
        startRecordTimer();
    }

    private void stopRecord() {
        stopRecordTimer();
        dataBinding.cameraTextureView.stopRecord();
        dataBinding.tvStartRecord.setText("录像");
        showToast("保存成功");
    }

    /**
     * 拍照
     */
    private void onClickCapture() {
        dataBinding.cameraTextureView.takePicture(new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                final Camera.Parameters parameters = camera.getParameters();
                Camera.Size pictureSize = parameters.getPictureSize();

                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                // TODO 测试
//                BitmapUtils.saveBitmap(bitmap, "bitmap.jpg");

                // 根据摄像头和手机方向旋转
                Matrix matrix = new Matrix();
                matrix.postRotate(dataBinding.cameraTextureView.getRotateDegree());
                Bitmap matrixBitmap = Bitmap.createBitmap(bitmap, 0, 0, pictureSize.width, pictureSize.height, matrix, true);
                // TODO 测试
//                BitmapUtils.saveBitmap(matrixBitmap, "matrixBitmap.jpg");

                // 这里预览是在屏幕上居中，直接按屏幕上预览的大小切图
                int cropWidth, cropHeight;
                if (dataBinding.cameraTextureView.isRotate()) {
                    cropWidth = dataBinding.rlContainer.getHeight();
                    cropHeight = dataBinding.rlContainer.getWidth();
                } else {
                    cropWidth = dataBinding.rlContainer.getWidth();
                    cropHeight = dataBinding.rlContainer.getHeight();
                }
                Bitmap cropBitmap = BitmapUtils.cropCenterByScale(matrixBitmap, cropWidth, cropHeight);
                BitmapUtils.saveBitmap(cropBitmap);

                BitmapUtils.recycleBitmap(cropBitmap);
                BitmapUtils.recycleBitmap(bitmap);
                BitmapUtils.recycleBitmap(matrixBitmap);
                showToast("保存成功");
                dataBinding.cameraTextureView.startPreview();
            }
        });
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void startRecordTimer() {
        if (countDownSecond > 0) {
            int second = countDownSecond + 1;
            if (mCountDownTimer == null) {
                mCountDownTimer = new CountDownTimer(second * 1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        mRecordSecond = (int) (millisUntilFinished / 1000);
                        refreshRecordTime();
                    }

                    @Override
                    public void onFinish() {
                        stopRecord();
                    }
                };
            } else {
                mCountDownTimer.cancel();
                mCountDownTimer.onTick(second * 1000);
            }
            mRecordSecond = second;
            refreshRecordTime();
            mCountDownTimer.start();
        } else {
            if (mTimer == null) {
                mTimer = new Timer();
            }
            mRecordSecond = 0;
            refreshRecordTime();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mRecordSecond++;
                    refreshRecordTime();
                }
            }, 1000, 1000);
        }
    }

    private void stopRecordTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
            mCountDownTimer = null;
        }
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
            mCountDownTimer = null;
        }
        mRecordSecond = -1;
        refreshRecordTime();
    }

    private void refreshRecordTime() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRecordSecond >= 0) {
                    dataBinding.llRecordTime.setVisibility(View.VISIBLE);
                    if (countDownSecond > 0) {
                        dataBinding.tvRecordTime.setText(mRecordSecond + "s");
                    } else {
                        dataBinding.tvRecordTime.setText(second2TimeString(mRecordSecond));
                    }
                } else {
                    dataBinding.llRecordTime.setVisibility(View.GONE);
                    dataBinding.tvRecordTime.setText("0");
                }
            }
        });
    }

    /**
     * 隐藏虚拟按键，并且全屏
     */
    private void hideBottomUIMenu() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
//                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE;
        decorView.setSystemUiVisibility(uiOptions);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
    }

    private String second2TimeString(int totalSeconds) {
        DecimalFormat df = new DecimalFormat("00");
        StringBuilder sb = new StringBuilder();
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds / 60) % 60;
        int seconds = totalSeconds % 60;
        if (0 < hours) {
            sb.append(df.format(hours)).append(":");
        }
        sb.append(df.format(minutes)).append(":").append(df.format(seconds));
        return sb.toString();
    }
}
