package com.rhino.camera.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.rhino.log.LogUtils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author LuoLin
 * @since Create on 2019/7/18.
 */
public class BitmapUtils {

    /**
     * 根据资源id获取bitmap
     */
    @Nullable
    public static Bitmap decodeBitmapFromResource(Context context, int resId) {
        if (0 == resId) {
            return null;
        }
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inPurgeable = true;
        opt.inInputShareable = true;
        InputStream is = context.getResources().openRawResource(resId);
        return BitmapFactory.decodeStream(is, null, opt);
    }

    /**
     * 读取图片文件
     */
    @Nullable
    public static Bitmap decodeBitmapFromFile(String filePath) {
        Bitmap bitmap = null;
        InputStream inputStream = null;
        try {
            File file = new File(filePath);
            inputStream = new FileInputStream(file);
            bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                bitmap = bitmap.copy(bitmap.getConfig(), true);
            }
        } catch (Exception e) {
            LogUtils.e(e.toString());
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
                LogUtils.e(e.toString());
            }
        }
        return bitmap;
    }

    /**
     * 读取Assets文件夹下图片文件
     */
    @Nullable
    public static Bitmap decodeBitmapFromAssets(Context context, String filename) {
        Bitmap bitmap = null;
        InputStream inputStream = null;
        try {
            AssetManager asm = context.getAssets();
            inputStream = asm.open(filename);
            bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                bitmap = bitmap.copy(bitmap.getConfig(), true);
            }
        } catch (IOException e) {
            LogUtils.e(e.toString());
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {
                LogUtils.e(e.toString());
            }
        }
        return bitmap;
    }

    /**
     * 读取Resources bitmap
     */
    @Nullable
    public static Bitmap decodeBitmapFromResource(Resources res, int resId, int destWidth, int destHeight) {
        // 获取 BitmapFactory.Options，这里面保存了很多有关 Bitmap 的设置
        final BitmapFactory.Options options = new BitmapFactory.Options();
        // 设置 true 轻量加载图片信息
        options.inJustDecodeBounds = true;
        // 由于上方设置false，这里轻量加载图片
        BitmapFactory.decodeResource(res, resId, options);
        // 计算采样率
        options.inSampleSize = calculateInSampleSize(options, destWidth, destHeight);
        // 设置 false 正常加载图片
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * 复制图片，并设置isMutable=true
     */
    @Nullable
    public static Bitmap copyBitmap(Bitmap bitmap) {
        return bitmap.copy(bitmap.getConfig(), true);
    }

    /**
     * 用ByteArrayOutputStream方式把Bitmap转Byte
     */
    @NonNull
    public static byte[] bitmap2BytesByStream(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * 用Buffer方式把Bitmap转Byte
     */
    @NonNull
    public static byte[] bitmap2BytesByBuffer(Bitmap bitmap) {
        int byteCount = bitmap.getByteCount();
        ByteBuffer byteBuffer = ByteBuffer.allocate(byteCount);
        bitmap.copyPixelsToBuffer(byteBuffer);
        return byteBuffer.array();
    }

    /**
     * 根据高度缩放Bitmap
     *
     * @param srcBitmap 源Bitmap
     * @param newHeight 新的height
     * @return 新的Bitmap
     */
    @Nullable
    public static Bitmap zoomImageByHeight(Bitmap srcBitmap, int newHeight) {
        if (null == srcBitmap || 0 >= srcBitmap.getWidth()
                || 0 >= srcBitmap.getHeight()
                || 0 >= newHeight) {
            return null;
        }
        int width = srcBitmap.getWidth();
        int height = srcBitmap.getHeight();
        Matrix matrix = new Matrix();
        float scale = (float) newHeight / height;
        matrix.setScale(scale, scale);
        srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, width, height,
                matrix, true);
        return srcBitmap;
    }

    /**
     * 根据宽度缩放Bitmap
     *
     * @param srcBitmap 源Bitmap
     * @param newWidth  新的width
     * @return 新的Bitmap
     */
    @Nullable
    public static Bitmap zoomImageByWidth(Bitmap srcBitmap, int newWidth) {
        if (null == srcBitmap || 0 >= srcBitmap.getWidth()
                || 0 >= srcBitmap.getHeight()
                || 0 >= newWidth) {
            return null;
        }
        int width = srcBitmap.getWidth();
        int height = srcBitmap.getHeight();
        Matrix matrix = new Matrix();
        float scale = (float) newWidth / width;
        matrix.setScale(scale, scale);
        srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, width, height,
                matrix, true);
        return srcBitmap;
    }

    /**
     * 根据宽高缩放Bitmap
     *
     * @param srcBitmap 源Bitmap
     * @param newWidth  新的width
     * @param newHeight 新的height
     * @return 新的Bitmap
     */
    @Nullable
    public static Bitmap zoomImage(Bitmap srcBitmap, int newWidth, int newHeight) {
        if (null == srcBitmap || 0 >= srcBitmap.getWidth()
                || 0 >= srcBitmap.getHeight()
                || 0 >= newWidth
                || 0 >= newHeight) {
            return null;
        }
        int width = srcBitmap.getWidth();
        int height = srcBitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = (float) newWidth / width;
        float scaleHeight = (float) newHeight / height;
        matrix.setScale(scaleWidth, scaleHeight);
        srcBitmap = Bitmap.createBitmap(srcBitmap, 0, 0, width, height,
                matrix, true);
        return srcBitmap;
    }

    /**
     * 保存bytes
     *
     * @param bytes bytes
     * @return 返回保存图片路径，null 保存失败
     */
    @Nullable
    public static String saveBytes(@NonNull byte[] bytes) {
        File dir = new File(Environment.getExternalStorageDirectory(), "Album");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        String fileName = PrimaryUtils.createPrimary() + ".jpg";
        File file = new File(dir, fileName);
        if (saveBytes(bytes, file.getAbsolutePath())) {
            return file.getPath();
        }
        return null;
    }

    /**
     * 保存bytes
     *
     * @param bytes bytes
     * @return true 保存成功， false 保存失败
     */
    public static boolean saveBytes(byte[] bytes, String filePath) {
        boolean saveSuccess = false;
        File file = new File(filePath);
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            return false;
        }
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            bufferedOutputStream.write(bytes);
            bufferedOutputStream.flush();
            saveSuccess = true;
        } catch (Exception e) {
            LogUtils.e(e.toString());
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    LogUtils.e(e);
                }
            }
            if (bufferedOutputStream != null) {
                try {
                    bufferedOutputStream.close();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        }
        return saveSuccess;
    }

    /**
     * 保存图片的方法
     *
     * @param bitmap Bitmap
     * @return 返回保存图片路径，null 保存失败
     */
    @Nullable
    public static String saveBitmap(@NonNull Bitmap bitmap) {
        return saveBitmap(bitmap, 100);
    }

    /**
     * 保存图片的方法
     *
     * @param bitmap Bitmap
     * @return 返回保存图片路径，null 保存失败
     */
    @Nullable
    public static String saveBitmap(@NonNull Bitmap bitmap, int quality) {
        File dir = new File(Environment.getExternalStorageDirectory(), "Album");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        String fileName = PrimaryUtils.createPrimary() + ".jpg";
        File file = new File(dir, fileName);
        return saveBitmap(bitmap, file.getAbsolutePath(), quality);
    }

    /**
     * 保存图片的方法
     *
     * @param bitmap             Bitmap
     * @param filePathOrFileName 保存文件路径或者文件名
     * @return true 保存成功， false 保存失败
     */
    public static String saveBitmap(@NonNull Bitmap bitmap, String filePathOrFileName) {
        return saveBitmap(bitmap, filePathOrFileName, 100);
    }

    /**
     * 保存图片的方法
     *
     * @param bitmap             Bitmap
     * @param filePathOrFileName 保存文件路径或者文件名
     * @param quality            保存质量
     * @return true 保存成功， false 保存失败
     */
    public static String saveBitmap(@NonNull Bitmap bitmap, String filePathOrFileName, int quality) {
        File file = new File(filePathOrFileName);
        if (!filePathOrFileName.contains("/")) {
            File dir = new File(Environment.getExternalStorageDirectory(), "Album");
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            file = new File(dir, filePathOrFileName);
        }
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            return null;
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            fos.flush();
            return file.getPath();
        } catch (IOException e) {
            LogUtils.e(e.toString());
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                LogUtils.e(e);
            }
        }
        return null;
    }

    /**
     * 回收bitmap
     */
    public static void recycleBitmap(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    /**
     * 裁剪
     */
    @Nullable
    public static Bitmap crop(@NonNull Bitmap bitmap, Rect previewRect, Rect cropRect) {
        LogUtils.d("准备裁剪, previewRect = " + previewRect.toString() + ", cropRect = " + cropRect);
        long timestamp = System.currentTimeMillis();

        int cropLeft = cropRect.left;
        int cropTop = cropRect.top;
        int cropWidth = cropRect.width();
        int cropHeight = cropRect.height();

        cropLeft = (int) (1.0f * cropLeft / previewRect.width() * bitmap.getWidth());
        cropTop = (int) (1.0f * cropTop / previewRect.height() * bitmap.getHeight());
        cropWidth = (int) (1.0f * cropWidth / previewRect.width() * bitmap.getWidth());
        cropHeight = (int) (1.0f * cropHeight / previewRect.height() * bitmap.getHeight());
        LogUtils.d("开始裁剪, cropLeft = " + cropLeft + ", cropTop = " + cropTop + ", cropWidth = " + cropWidth + ", cropHeight = " + cropHeight + ", time = " + (System.currentTimeMillis() - timestamp));
        Bitmap cropBitmap = Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight);
        LogUtils.d("裁剪完成, time = " + (System.currentTimeMillis() - timestamp));
        return cropBitmap;
    }

    /**
     * 根据比例裁剪中心图片
     */
    public static Bitmap cropCenterByScale(Bitmap bitmap, int cropWidth, int cropHeight) {
        // 高以图片的高算出比例的宽
        int rWidth = (int) (1.0f * bitmap.getHeight() / cropHeight * cropWidth);
        // 宽以图片的宽算出比例的高
        int rHeight = (int) (1.0f * bitmap.getWidth() / cropWidth * cropHeight);

        Rect cropRect = new Rect();
        if (rWidth < bitmap.getWidth()) {
            // 说明图片宽的比例较大，按高来裁剪
            cropRect.left = (bitmap.getWidth() - rWidth) / 2;
            cropRect.top = 0;
            cropRect.bottom = bitmap.getHeight();
            cropRect.right = cropRect.left + rWidth;
        } else {
            // 说明图片的高比例较大，按宽来裁剪
            cropRect.left = 0;
            cropRect.top = (bitmap.getHeight() - rHeight) / 2;
            cropRect.bottom = cropRect.top + rHeight;
            cropRect.right = bitmap.getWidth();
        }
        return Bitmap.createBitmap(bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());
    }

    /**
     * 计算采样率
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int width = options.outWidth;
        final int height = options.outHeight;
        int inSampleSize = 1; // 宽或高大于预期就将采样率 *=2 进行缩放
        if (width > reqWidth || height > reqHeight) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

}
