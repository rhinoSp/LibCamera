package com.rhino.camera;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;

import com.rhino.log.LogUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/**
 * @author LuoLin
 * @since Create on 2019/7/18.
 */
public class CameraTextureView extends TextureView implements TextureView.SurfaceTextureListener {

    public static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    public static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    public static final int DEGREES_0 = 0;
    public static final int DEGREES_45 = 45;
    public static final int DEGREES_90 = 90;
    public static final int DEGREES_135 = 135;
    public static final int DEGREES_180 = 180;
    public static final int DEGREES_270 = 270;
    public static final int DEGREES_225 = 225;
    public static final int DEGREES_315 = 315;
    public static final int DEGREES_360 = 360;
    public static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    public static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    /**
     * 开启预览失败最多重试次数
     */
    private static final int MAX_START_PREVIEW_TRY_COUNT = 3;
    /**
     * 最小摄像头预览分辨率
     */
    public static final int INVALID_PREVIEW_MIN_SIZE = 480;

    /**
     * 视频编码帧率
     */
    public static final int VIDEO_ENCODING_BIT_RATE = 2 * 1280 * 720;
    /**
     * 视频帧频率
     */
    public static final int VIDEO_FRAME_RATE = 30;
    /**
     * 音频帧频率
     */
    public static final int AUDIO_SAMPLING_RATE = 16000;

    /**
     * 不缩放，分辨率和控件大小一样，可能会变形
     */
    public static final int FIT_STYLE_NONE = 0;
    /**
     * 宽铺满，高根据分辨率缩放
     */
    public static final int FIT_STYLE_FILL_WIDTH = 1;
    /**
     * 高铺满，宽根据分辨率缩放
     */
    public static final int FIT_STYLE_FILL_HEIGHT = 2;
    /**
     * 根据分辨率缩放，将宽和高都铺满
     */
    public static final int FIT_STYLE_FILL_WIDTH_HEIGHT = 3;
    public int fitStyle = FIT_STYLE_FILL_WIDTH_HEIGHT;

    @IntDef({FIT_STYLE_NONE, FIT_STYLE_FILL_WIDTH, FIT_STYLE_FILL_HEIGHT, FIT_STYLE_FILL_WIDTH_HEIGHT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface FitStyle {
    }

    /**
     * Camera
     */
    public Camera camera;
    /**
     * CameraInfo
     */
    public Camera.CameraInfo cameraInfo;
    /**
     * 摄像头预览回调
     */
    public Camera.PreviewCallback previewCallback;
    /**
     * 摄像头预览大小
     */
    public Camera.Size previewSize;
    /**
     * 摄像头拍照图片大小
     */
    public Camera.Size pictureSize;
    /**
     * 摄像头facing
     */
    public int cameraFacing = Camera.CameraInfo.CAMERA_FACING_FRONT;
    /**
     * 摄像头id
     */
    public int cameraId = -1;
    /**
     * 摄像头是否打开
     */
    public boolean isCameraOpened = false;
    /**
     * 是否打开预览
     */
    public boolean isStartPreview = false;
    /**
     * 打开预览失败重试次数
     */
    private int startPreviewTryCount = 0;

    /**
     * 期望的预览分辨率宽
     */
    public int expectPreviewWidth = 1920;
    /**
     * 期望的预览分辨率高
     */
    public int expectPreviewHeight = 1080;

    /**
     * 期望的拍照分辨率宽
     */
    public int expectPictureWidth = -1;
    /**
     * 期望的拍照分辨率高
     */
    public int expectPictureHeight = -1;

    /**
     * 视频编码
     */
    public int videoEncoder = MediaRecorder.VideoEncoder.H264;
    /**
     * 音频编码
     */
    public int audioEncoder = MediaRecorder.AudioEncoder.DEFAULT;
    /**
     * MediaRecorder
     */
    public MediaRecorder mediaRecorder;
    /**
     * 是否正在录制
     */
    public boolean recording;

    /**
     * 手机的方向
     */
    public int windowRotation;
    /**
     * 方向传感器监听器
     */
    public OrientationEventListener orientationEventListener;
    /**
     * 手机的方向
     */
    public int phoneDegree = 0;
    /**
     * 手机方向改变回调时间
     */
    public OnPhoneDegreeChangeListener onPhoneDegreeChangeListener;
    /**
     * 控件状态变化监听器
     */
    private SurfaceTextureListener outerSurfaceTextureListener;

    /**
     * 是否开启方向传感器监听
     */
    private boolean enableOrientationListener = false;

    public interface OnPhoneDegreeChangeListener {
        void onPhoneDegreeChanged(int phoneDegree);
    }

    public CameraTextureView(Context context) {
        super(context);
        init();
    }

    public CameraTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (fitStyle == FIT_STYLE_NONE || previewSize == null) {
            setMeasuredDimension(width, height);
            return;
        }
        int ratioWidth;
        int ratioHeight;
        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            ratioWidth = previewSize.height;
            ratioHeight = previewSize.width;
        } else {
            ratioWidth = previewSize.width;
            ratioHeight = previewSize.height;
        }

        // 高以控件的高算出比例的宽
        int rWidth = (int) (1.0f * height / ratioHeight * ratioWidth);
        // 宽以控件的宽算出比例的高
        int rHeight = (int) (1.0f * width / ratioWidth * ratioHeight);
        switch (fitStyle) {
            case FIT_STYLE_FILL_WIDTH:
                // 将宽铺满，高根据分辨率适应
                setMeasuredDimension(width, rHeight);
                break;
            case FIT_STYLE_FILL_HEIGHT:
                // 将高铺满，宽根据分辨率适应
                setMeasuredDimension(rWidth, height);
                break;
            case FIT_STYLE_FILL_WIDTH_HEIGHT:
                if (width < rWidth) {
                    // 比例宽大于控件宽，以控件的高作为实际高，可以铺满
                    setMeasuredDimension(rWidth, height);
                } else if (height < rHeight) {
                    // 比例高大于控件高，以控件的宽作为实际的宽，可以铺满
                    setMeasuredDimension(width, rHeight);
                }
                break;
            default:
                setMeasuredDimension(width, height);
                break;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        startPreview();
        requestLayout();
        if (outerSurfaceTextureListener != null) {
            outerSurfaceTextureListener.onSurfaceTextureAvailable(surface, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        if (outerSurfaceTextureListener != null) {
            outerSurfaceTextureListener.onSurfaceTextureSizeChanged(surface, width, height);
        }
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        stopPreview();
        closeCamera();
        if (outerSurfaceTextureListener != null) {
            outerSurfaceTextureListener.onSurfaceTextureDestroyed(surface);
        }
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (outerSurfaceTextureListener != null) {
            outerSurfaceTextureListener.onSurfaceTextureUpdated(surface);
        }
    }

    /**
     * 初始化
     */
    public void init() {
        setSurfaceTextureListener(this);
        // 默认摄像头
        setCameraFacing(cameraFacing);
    }

    /**
     * 设置是否开启方向传感器监听
     */
    public void setEnableOrientationListener(boolean enableOrientationListener) {
        this.enableOrientationListener = enableOrientationListener;
    }

    /**
     * 设置外部SurfaceTextureListener
     */
    public void setOuterSurfaceTextureListener(SurfaceTextureListener outerSurfaceTextureListener) {
        this.outerSurfaceTextureListener = outerSurfaceTextureListener;
    }

    /**
     * 设置方向监听
     */
    public void setOnPhoneDegreeChangeListener(OnPhoneDegreeChangeListener onPhoneDegreeChangeListener) {
        this.onPhoneDegreeChangeListener = onPhoneDegreeChangeListener;
    }

    /**
     * 开启方向传感器
     */
    public void enableOrientationListener() {
        if (enableOrientationListener && orientationEventListener == null) {
            orientationEventListener = new OrientationEventListener(getContext()) {
                @Override
                public void onOrientationChanged(int orientation) {
                    if (((orientation >= 0) && (orientation <= DEGREES_45)) || (orientation > DEGREES_315) && (orientation <= DEGREES_360)) {
                        phoneDegree = DEGREES_0;
                        setWindowRotation(Surface.ROTATION_0);
                    } else if ((orientation > DEGREES_45) && (orientation <= DEGREES_135)) {
                        phoneDegree = DEGREES_90;
                        if (isFrontCamera()) {
                            setWindowRotation(Surface.ROTATION_90);
                        } else {
                            setWindowRotation(Surface.ROTATION_270);
                        }
                    } else if ((orientation > DEGREES_135) && (orientation <= DEGREES_225)) {
                        phoneDegree = DEGREES_180;
                        setWindowRotation(Surface.ROTATION_180);
                    } else if ((orientation > DEGREES_225) && (orientation <= DEGREES_315)) {
                        phoneDegree = DEGREES_270;
                        if (isFrontCamera()) {
                            setWindowRotation(Surface.ROTATION_270);
                        } else {
                            setWindowRotation(Surface.ROTATION_90);
                        }
                    }
                    if (onPhoneDegreeChangeListener != null) {
                        onPhoneDegreeChangeListener.onPhoneDegreeChanged(phoneDegree);
                    }
                    LogUtils.d("onOrientationChanged: orientation = " + orientation + ", phoneDegree = " + phoneDegree + ", cameraInfo.orientation = " + cameraInfo.orientation);
                }
            };
            orientationEventListener.enable();
        }
    }

    /**
     * 关闭方向传感器
     */
    public void disableOrientationListener() {
        if (orientationEventListener != null) {
            orientationEventListener.disable();
            orientationEventListener = null;
        }
    }

    /**
     * 方向调整、预览界面调整避免拉伸挤压
     */
    private void setDisplayOrientation() {
        try {
            if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                int orientation = isFrontCamera() ? DEGREES_360 - cameraInfo.orientation : cameraInfo.orientation;
                LogUtils.d("初始化摄像头预览扭转角度：cameraInfo.orientation = " + cameraInfo.orientation + ", orientation = " + orientation);
                camera.setDisplayOrientation(orientation);
            } else {
                camera.setDisplayOrientation(0);
            }
        } catch (Exception e) {
            LogUtils.e(e.toString());
        }
    }

    /**
     * 设置预览尺寸
     */
    private void setPreviewSize() {
        try {
            Camera.Parameters params = camera.getParameters();
            List<Camera.Size> supportedPreviewSizes = params.getSupportedPreviewSizes();
            previewSize = getMatchingSize(supportedPreviewSizes, expectPreviewWidth, expectPreviewHeight);
            LogUtils.d("最佳preview尺寸 width = " + previewSize.width + ", height = " + previewSize.height);
            params.setPreviewSize(previewSize.width, previewSize.height);
            camera.setParameters(params);
        } catch (Exception e) {
            previewSize = camera.getParameters().getPreviewSize();
            LogUtils.e(e.toString());
        }
    }

    /**
     * 设置拍照图片尺寸
     */
    private void setPictureSize(Camera.Size previewSize) {
        try {
            Camera.Parameters params = camera.getParameters();
            List<Camera.Size> supportedPictureSizes = params.getSupportedPictureSizes();
            if (expectPictureWidth <= 0 || expectPictureHeight <= 0) {
                pictureSize = getMatchingSize(supportedPictureSizes, previewSize.width, previewSize.height);
            } else {
                pictureSize = getMatchingSize(supportedPictureSizes, expectPictureWidth, expectPictureHeight);
            }
            LogUtils.d("最佳picture尺寸 width = " + pictureSize.width + ", height = " + pictureSize.height);
            params.setPictureSize(pictureSize.width, pictureSize.height);
            this.camera.setParameters(params);
        } catch (Exception e) {
            pictureSize = camera.getParameters().getPictureSize();
            LogUtils.e(e.toString());
        }
    }

    /**
     * 设置预览格式
     */
    private void setPreviewFormat() {
        try {
            Camera.Parameters params = camera.getParameters();
            params.setPreviewFormat(ImageFormat.NV21);
            camera.setParameters(params);
        } catch (Exception e) {
            LogUtils.e(e.toString());
        }
    }

    /**
     * 设置聚焦模式(自动对焦)
     */
    private void setFocusMode() {
        try {
            Camera.Parameters params = camera.getParameters();
            // 设置聚焦模式(自动对焦)
            if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            camera.setParameters(params);
        } catch (Exception e) {
            LogUtils.e(e.toString());
        }
    }

    /**
     * 通过对比得到与宽高比最接近的尺寸（如果有相同尺寸，优先选择）
     *
     * @param sizes        需要对比的预览尺寸列表
     * @param expectWidth  期望的分辨率宽
     * @param expectHeight 期望的分辨率高
     * @return 得到与原宽高比例最接近的尺寸
     */
    private Camera.Size getMatchingSize(List<Camera.Size> sizes, int expectWidth, int expectHeight) {
        Camera.Size selectSize = null;
        float selectProportion = 0.0F;
        float viewProportion = (float) getWidth() / (float) getHeight();
        for (int i = 0; i < sizes.size(); ++i) {
            Camera.Size itemSize = sizes.get(i);
            LogUtils.d("support size width: " + itemSize.width + ", height: " + itemSize.height);
            if (itemSize.width < INVALID_PREVIEW_MIN_SIZE || itemSize.height < INVALID_PREVIEW_MIN_SIZE) {
                continue;
            }
            float itemSizeProportion;
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                itemSizeProportion = (float) itemSize.width / (float) itemSize.height;
                if (itemSize.width == expectWidth && itemSize.height == expectHeight) {
                    return itemSize;
                }
            } else {
                itemSizeProportion = (float) itemSize.height / (float) itemSize.width;
                if (itemSize.width == expectHeight && itemSize.height == expectWidth) {
                    return itemSize;
                }
            }
            float differenceProportion = Math.abs(viewProportion - itemSizeProportion);
            if (i == 0) {
                selectSize = itemSize;
                selectProportion = differenceProportion;
            } else if (differenceProportion <= selectProportion) {
                LogUtils.d("<= differenceProportion = " + differenceProportion + ", selectProportion: " + selectProportion + ", itemSize = " + itemSize.width + "x" + itemSize.height);
                if (differenceProportion == selectProportion) {
                    if (selectSize != null && selectSize.width + selectSize.height < itemSize.width + itemSize.height) {
                        LogUtils.d("selectSize = " + selectSize.width + "x" + selectSize.height);
                        selectSize = itemSize;
                        selectProportion = differenceProportion;
                    }
                } else {
                    selectSize = itemSize;
                    selectProportion = differenceProportion;
                }
            }
        }
        return selectSize;
    }

    /**
     * Get fix style.
     *
     * @return {@link FitStyle}
     */
    @FitStyle
    public int getFitStyle() {
        return fitStyle;
    }

    /**
     * Set fix style.
     *
     * @param fitStyle {@link FitStyle}
     */
    public void setFitStyle(@FitStyle int fitStyle) {
        this.fitStyle = fitStyle;
    }

    /**
     * 打开摄像头
     *
     * @return true 成功开启
     */
    public boolean openCamera() {
        isCameraOpened = false;
        try {
            camera = Camera.open(cameraId);
            camera.setPreviewTexture(getSurfaceTexture());
            setPreviewSize();
            setPictureSize(previewSize);
            setDisplayOrientation();
            setFocusMode();
            setPreviewFormat();
            isCameraOpened = (camera != null);
        } catch (Exception e) {
            LogUtils.e("摄像头打开失败：" + e.toString());
            isCameraOpened = false;
        }
        return isCameraOpened;
    }

    /**
     * 关闭摄像头
     *
     * @return true 成功关闭
     */
    public boolean closeCamera() {
        try {
            if (camera != null) {
                camera.release();
            }
            camera = null;
            isCameraOpened = false;
        } catch (Exception e) {
            LogUtils.e("关闭摄像头失败：" + e.toString());
            return false;
        }
        return true;
    }

    /**
     * 开始预览
     *
     * @return true 成功开启
     */
    public boolean startPreview() {
        enableOrientationListener();
        isStartPreview = false;
        if (isAvailable()) {
            try {
                if (camera == null) {
                    openCamera();
                }
                camera.setPreviewCallback(previewCallback);
                camera.startPreview();
                isStartPreview = true;
                startPreviewTryCount = 0;
            } catch (Exception e) {
                LogUtils.e("打开预览失败：" + e.toString());
                isStartPreview = false;
                closeCamera();
                if (startPreviewTryCount < MAX_START_PREVIEW_TRY_COUNT) {
                    startPreviewTryCount++;
                    startPreview();
                }
            }
        }
        return isStartPreview;
    }

    /**
     * 停止预览
     *
     * @return true 成功停止
     */
    public boolean stopPreview() {
        startPreviewTryCount = 0;
        isStartPreview = false;
        disableOrientationListener();
        try {
            if (camera != null) {
                camera.stopPreview();
                camera.setPreviewCallback(null);
            }
        } catch (Exception e) {
            LogUtils.e("停止预览失败：" + e.toString());
            return false;
        }
        return true;
    }

    /**
     * 是否正在录制
     */
    public boolean isRecording() {
        return recording;
    }

    /**
     * 设置方向
     */
    public void setWindowRotation(int rotation) {
        windowRotation = rotation;
    }

    /**
     * 获取摄像头方向
     */
    public int getCameraOrientation() {
        return cameraInfo.orientation;
    }

    /**
     * 获取图片需要旋转方向
     */
    public int getRotateDegree() {
        return (phoneDegree + cameraInfo.orientation) % 360;
    }

    /**
     * 获取手机方向
     */
    public int getPhoneDegree() {
        return phoneDegree;
    }

    /**
     * 是否旋转
     */
    public boolean isRotate() {
        return phoneDegree == DEGREES_90 || phoneDegree == DEGREES_270;
    }

    /**
     * 设置视频编码格式
     */
    public void setVideoEncoder(int videoEncoder) {
        this.videoEncoder = videoEncoder;
    }

    /**
     * 设置音频编码格式
     */
    public void setAudioEncoder(int audioEncoder) {
        this.audioEncoder = audioEncoder;
    }

    /**
     * 录像
     */
    public boolean startRecord(String outPutFilePath) {
        if (recording) {
            return false;
        }
        try {

            // 关闭预览并释放资源
            camera.unlock();
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setCamera(camera);
            Surface previewSurface = new Surface(getSurfaceTexture());
            mediaRecorder.setPreviewDisplay(previewSurface); // 预览
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA); // 视频源
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER); // 录音源为麦克风静音
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // 输出格式为mp4
            mediaRecorder.setVideoSize(previewSize.width, previewSize.height); // 视频尺寸
            mediaRecorder.setOutputFile(outPutFilePath);
            mediaRecorder.setVideoEncodingBitRate(VIDEO_ENCODING_BIT_RATE); //设置视频编码帧率
            mediaRecorder.setVideoFrameRate(VIDEO_FRAME_RATE); // 视频帧频率
            mediaRecorder.setAudioSamplingRate(AUDIO_SAMPLING_RATE);
            mediaRecorder.setVideoEncoder(videoEncoder); // 视频编码
            mediaRecorder.setAudioEncoder(audioEncoder); // 音频编码
            int mSensorOrientation = getCameraOrientation();
            LogUtils.d("windowRotation = " + windowRotation + ", mSensorOrientation = " + mSensorOrientation);
            switch (mSensorOrientation) {
                case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                    mediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(windowRotation));
                    break;
                case SENSOR_ORIENTATION_INVERSE_DEGREES:
                    mediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(windowRotation));
                    break;
                default:
                    break;
            }
            mediaRecorder.prepare();
            mediaRecorder.start();
            recording = true;
            setAutoFocus(null);
            return true;
        } catch (Exception e) {
            LogUtils.e(e.toString());
        }
        return false;
    }

    /**
     * 停止录制
     */
    public void stopRecord() {
        if (recording) {
            try {
                recording = false;
                mediaRecorder.setOnErrorListener(null);
                mediaRecorder.setOnInfoListener(null);
                mediaRecorder.setPreviewDisplay(null);
                mediaRecorder.stop();
                mediaRecorder.release();
            } catch (Exception e) {
                LogUtils.e(e.toString());
            }
        }
    }

    /**
     * 设置自动对焦
     */
    public void setAutoFocus(@Nullable final Camera.AutoFocusCallback callback) {
        if (camera != null) {
            // get Camera parameters
            Camera.Parameters params = camera.getParameters();
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                if (callback != null) {
                    camera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            callback.onAutoFocus(success, camera);
                        }
                    });
                } else {
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    camera.setParameters(params);
                    camera.autoFocus(null);
                }
            }
        }
    }

    /**
     * 设置期望预览分辨率，如果摄像头不支持会自动适配最佳分辨率
     */
    public void setExpectPreviewSize(int previewWidth, int previewHeight) {
        this.expectPreviewWidth = previewWidth;
        this.expectPreviewHeight = previewHeight;
    }

    /**
     * 设置期望拍照分辨率，如果摄像头不支持会自动适配最佳分辨率
     */
    public void setExpectPictureSize(int pictureWidth, int pictureHeight) {
        this.expectPictureWidth = pictureWidth;
        this.expectPictureHeight = pictureHeight;
    }

    /**
     * 是否为前置摄像头
     *
     * @return 前置摄像头
     */
    public boolean isFrontCamera() {
        return cameraFacing == Camera.CameraInfo.CAMERA_FACING_FRONT;
    }

    /**
     * 设置前置摄像头 Camera.CameraInfo.CAMERA_FACING_FRONT
     */
    public void setFrontCamera() {
        setCameraFacing(Camera.CameraInfo.CAMERA_FACING_FRONT);
    }

    /**
     * 设置后置摄像头 Camera.CameraInfo.CAMERA_FACING_BACK
     */
    public void setBackCamera() {
        setCameraFacing(Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    /**
     * 获取摄像头
     *
     * @return Camera.CameraInfo.CAMERA_FACING_FRONT or Camera.CameraInfo.CAMERA_FACING_BACK or ...
     */
    public int getCameraFacing() {
        return cameraFacing;
    }

    /**
     * 设置摄像头
     *
     * @param cameraFacing Camera.CameraInfo.CAMERA_FACING_FRONT or Camera.CameraInfo.CAMERA_FACING_BACK or ...
     */
    public void setCameraFacing(int cameraFacing) {
        this.cameraFacing = cameraFacing;
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == cameraFacing) {
                cameraInfo = info;
                cameraId = i;
                break;
            }
        }
        if (isCameraOpened) {
            stopPreview();
            closeCamera();

            openCamera();
            startPreview();
        }
    }

    /**
     * 是否已经开启预览
     *
     * @return true 开启预览
     */
    public boolean isStartPreview() {
        return isStartPreview;
    }

    /**
     * 相机是否打开
     *
     * @return true 打开
     */
    public boolean isCameraOpened() {
        return isCameraOpened;
    }

    /**
     * 设置预览回调
     *
     * @param previewCallback Camera.PreviewCallback
     */
    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        this.previewCallback = previewCallback;
    }

    /**
     * 获取Camera
     *
     * @return Camera
     */
    public Camera getCamera() {
        return camera;
    }

    /**
     * 获取Camera.CameraInfo
     *
     * @return Camera.CameraInfo
     **/
    public Camera.CameraInfo getCameraInfo() {
        return cameraInfo;
    }

    /**
     * 获取预览Camera.Size
     *
     * @return Camera.Size
     */
    public Camera.Size getPreviewSize() {
        if (camera.getParameters() != null) {
            return camera.getParameters().getPreviewSize();
        }
        return null;
    }

    /**
     * 开始对焦
     */
    public void startFocus(float x, float y, float viewWidth, float viewHeight, Camera.AutoFocusCallback callback) {
        Camera.Parameters parameters = camera.getParameters();
        if (parameters.getMaxNumFocusAreas() <= 0) {
            camera.autoFocus(callback);
            return;
        }
        int areaX = (int) (x / viewWidth * 2000) - 1000; // 获取映射区域的X坐标
        int areaY = (int) (y / viewHeight * 2000) - 1000; // 获取映射区域的Y坐标

        // 创建Rect区域
        Rect focusArea = new Rect();
        focusArea.left = Math.max(areaX - 100, -1000); // 取最大或最小值，避免范围溢出屏幕坐标
        focusArea.top = Math.max(areaY - 100, -1000);
        focusArea.right = Math.min(areaX + 100, 1000);
        focusArea.bottom = Math.min(areaY + 100, 1000);
        // 创建Camera.Area
        Camera.Area cameraArea = new Camera.Area(focusArea, 1000);
        List<Camera.Area> meteringAreas = new ArrayList<>();
        List<Camera.Area> focusAreas = new ArrayList<>();
        meteringAreas.add(cameraArea);
        focusAreas.add(cameraArea);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO); // 设置对焦模式
        parameters.setFocusAreas(focusAreas); // 设置对焦区域
        parameters.setMeteringAreas(meteringAreas); // 设置测光区域
        try {
            camera.cancelAutoFocus(); // 每次对焦前，需要先取消对焦
            camera.setParameters(parameters); // 设置相机参数
            camera.autoFocus(callback); // 开启对焦
        } catch (Exception e) {
            LogUtils.e(e.toString());
        }
    }

    /**
     * 拍照
     *
     * @param callback Camera.PictureCallback
     */
    public void takePicture(Camera.PictureCallback callback) {
        try {
            if (camera != null) {
                camera.takePicture(null, null, callback);
            }
        } catch (Exception e) {
            LogUtils.e(e.toString());
        }
    }

    public static class DefaultSurfaceTextureListener implements SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            LogUtils.d("onSurfaceTextureAvailable");
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            LogUtils.d("onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            LogUtils.d("onSurfaceTextureDestroyed");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            LogUtils.d("onSurfaceTextureUpdated");
        }
    }

}