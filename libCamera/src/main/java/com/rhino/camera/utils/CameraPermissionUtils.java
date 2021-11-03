package com.rhino.camera.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;


/**
 * <p>权限管理类</p>
 *
 * @author LuoLin
 * @since Create on 2019/7/29.
 **/
public class CameraPermissionUtils {

    /**
     * 拍照需要的权限
     */
    public static final String[] PERMISSIONS_PICTURE_CAPTURE = {
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    /**
     * 录像需要的权限
     */
    public static final String[] PERMISSIONS_VIDEO_RECORD = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    /**
     * 拍照权限的请求code
     */
    public static final int REQUEST_CODE_CAMERA = 1;
    /**
     * 录像权限的请求code
     */
    public static final int REQUEST_CODE_VIDEO_RECORD = 2;

    /**
     * 请求拍照权限
     */
    public static void requestPictureCapturePermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, PERMISSIONS_PICTURE_CAPTURE, REQUEST_CODE_CAMERA);
    }

    /**
     * 请求录像权限
     */
    public static void requestVideoRecordPermission(Activity activity) {
        ActivityCompat.requestPermissions(activity, PERMISSIONS_VIDEO_RECORD, REQUEST_CODE_VIDEO_RECORD);
    }

    /**
     * 请求权限
     */
    public static void requestPermission(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    /**
     * 检查拍照权限
     */
    public static boolean checkPictureCapturePermission(Activity activity) {
        if (!checkSelfPermission(activity, PERMISSIONS_PICTURE_CAPTURE)) {
            requestPictureCapturePermission(activity);
            return false;
        } else {
            return true;
        }
    }

    /**
     * 检查录像权限
     */
    public static boolean checkVideoRecordPermission(Activity activity) {
        if (!checkSelfPermission(activity, PERMISSIONS_VIDEO_RECORD)) {
            requestVideoRecordPermission(activity);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Check permissions
     */
    public static boolean checkSelfPermission(@NonNull Context context, @NonNull String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check permission when onRequestPermissionsResult
     */
    public static boolean checkHasAllPermission(@NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length == permissions.length) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

}
