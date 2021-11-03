package com.rhino.camera.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import androidx.annotation.Nullable;

import com.rhino.log.LogUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * <p>bitmap压缩</p>
 *
 * @author LuoLin
 * @since Create on 2019/8/26.
 **/
public class BitmapCompressUtils {

    private static final String TAG = BitmapCompressUtils.class.getSimpleName();

    /**
     * RGB_565法
     * 假如对图片没有透明度要求的话，可以改成RGB_565，相比ARGB_8888将节省一半的内存开销
     */
    @Nullable
    public static Bitmap compressToRGB565(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        LogUtils.d(TAG, "压缩前 size = " + bitmap.getByteCount() + ", width = " + bitmap.getWidth() + ", height = " + bitmap.getHeight());
        Bitmap outBitmap = null;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            outBitmap = BitmapFactory.decodeStream(byteArrayInputStream, null, options);
        } catch (IOException e) {
            LogUtils.e(TAG, e.toString());
        }
        return outBitmap;
    }

    /**
     * 质量压缩方法，返回byte数组
     */
    @Nullable
    public static byte[] compressImageByQualityToByte(Bitmap bitmap, int quality) {
        if (bitmap == null) {
            return null;
        }
        LogUtils.d(TAG, "压缩前 size = " + bitmap.getByteCount() + ", width = " + bitmap.getWidth() + ", height = " + bitmap.getHeight());
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            byteArrayOutputStream.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            LogUtils.e(TAG, e.toString());
        }
        return null;
    }

    /**
     * 质量压缩方法，返回bitmap
     */
    @Nullable
    public static Bitmap compressImageByQuality(Bitmap bitmap, int quality) {
        if (bitmap == null || quality >= 100) {
            return bitmap;
        }
        LogUtils.d(TAG, "压缩前 size = " + bitmap.getByteCount() + ", width = " + bitmap.getWidth() + ", height = " + bitmap.getHeight());
        Bitmap outBitmap = null;
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            outBitmap = BitmapFactory.decodeStream(byteArrayInputStream, null, null);
            byteArrayOutputStream.reset();
            LogUtils.d(TAG, "压缩后 size = " + outBitmap.getByteCount() + ", width = " + outBitmap.getWidth() + ", height = " + outBitmap.getHeight());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outBitmap;
    }

    /**
     * 质量压缩方法，压缩到指定大小kb
     */
    @Nullable
    public static byte[] compressImageByQualityToSizeKbToByte(Bitmap bitmap, int toSizeKb) {
        if (bitmap == null) {
            return null;
        }
        int quality = calculateQualityToSizeKb(bitmap, toSizeKb, 1);
        return compressImageByQualityToByte(bitmap, quality);
    }

    /**
     * 质量压缩方法，压缩到指定大小kb
     */
    @Nullable
    public static Bitmap compressImageByQualityToSizeKb(Bitmap bitmap, int toSizeKb) {
        return compressImageByQualityToSizeKb(bitmap, toSizeKb, 1);
    }

    /**
     * 质量压缩方法，压缩到指定大小kb
     */
    @Nullable
    public static Bitmap compressImageByQualityToSizeKb(Bitmap bitmap, int toSizeKb, int toQuality) {
        if (bitmap == null) {
            return null;
        }
        int quality = calculateQualityToSizeKb(bitmap, toSizeKb, toQuality);
        return compressImageByQuality(bitmap, quality);
    }

    /**
     * 获取quality大小，用于压缩bitmap到指定kb
     */
    public static int calculateQualityToSizeKb(Bitmap bitmap, int toSizeKb, int toQuality) {
        if (bitmap == null) {
            return 100;
        }
        Log.d(TAG, "压缩前 size = " + bitmap.getByteCount() + ", width = " + bitmap.getWidth() + ", height = " + bitmap.getHeight());
        ByteArrayOutputStream byteArrayOutputStream = null;
        int quality = 100;

        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);

            while (byteArrayOutputStream.toByteArray().length / 1024 > toSizeKb && quality > toQuality) {
                byteArrayOutputStream.reset();
                quality = Math.max(quality - 5, toQuality);
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
            }

            Log.d(TAG, "压缩后 quality = " + quality);
        } catch (Exception var14) {
            Log.e(TAG, var14.toString());
        } finally {
            if (byteArrayOutputStream != null) {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException var13) {
                    var13.printStackTrace();
                }
            }

        }

        return quality;
    }

    /**
     * 缩放最小边到指定大小
     */
    @Nullable
    public static Bitmap scaleLitterSideToDestSize(Bitmap bitmap, int destSize) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale;
        // 获取想要缩放的matrix
        Matrix matrix = new Matrix();
        if (width > height && height > destSize) {
            scale = ((float) destSize) / height;
            matrix.postScale(scale, scale);
            // 获取新的bitmap
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        } else if (width < height && width > destSize) {
            scale = ((float) destSize) / width;
            matrix.postScale(scale, scale);
            // 获取新的bitmap
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        }
        return bitmap;
    }

}
