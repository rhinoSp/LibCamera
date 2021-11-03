package com.rhino.camera.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

    private static final String TAG = BitmapUtils.class.getSimpleName();

    // 拼接图片方向标志（原图左边）
    private static final int LEFT = 0;
    // 拼接图片方向标志（原图右边）
    private static final int RIGHT = 1;
    // 拼接图片方向标志（原图上边）
    private static final int TOP = 2;
    // 拼接图片方向标志（原图下边）
    private static final int BOTTOM = 3;

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
        File file = new File(filePath);
        try (InputStream inputStream = new FileInputStream(file)) {
            bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                bitmap = bitmap.copy(bitmap.getConfig(), true);
            }
        } catch (IOException e) {
            LogUtils.e(TAG, e.toString());
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
            LogUtils.e(TAG, e.toString());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                LogUtils.e(TAG, e.toString());
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
    public static byte[] bitmapToBytesByStream(Bitmap bitmap) {
        return bitmapToBytesByStream(bitmap, 100);
    }

    /**
     * Bitmap转RGB
     */
    @NonNull
    public static byte[] bitmapToRGB(Bitmap bitmap) {
        int bytes = bitmap.getByteCount();  //返回可用于储存此位图像素的最小字节数

        ByteBuffer buffer = ByteBuffer.allocate(bytes); //  使用allocate()静态方法创建字节缓冲区
        bitmap.copyPixelsToBuffer(buffer); // 将位图的像素复制到指定的缓冲区

        byte[] rgba = buffer.array();
        byte[] pixels = new byte[(rgba.length / 4) * 3];

        int count = rgba.length / 4;

        //Bitmap像素点的色彩通道排列顺序是RGBA
        for (int i = 0; i < count; i++) {

            pixels[i * 3] = rgba[i * 4];        //R
            pixels[i * 3 + 1] = rgba[i * 4 + 1];    //G
            pixels[i * 3 + 2] = rgba[i * 4 + 2];       //B

        }
        return pixels;
    }

    /**
     * 用ByteArrayOutputStream方式把Bitmap转Byte
     */
    @NonNull
    public static byte[] bitmapToBytesByStream(Bitmap bitmap, int quality) {
        if (bitmap == null) {
            return new byte[]{};
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * 用Buffer方式把Bitmap转Byte
     */
    @NonNull
    public static byte[] bitmapToBytesByBuffer(Bitmap bitmap) {
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
        try (FileOutputStream fileOutputStream = new FileOutputStream(file);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream)) {
            bufferedOutputStream.write(bytes);
            bufferedOutputStream.flush();
            saveSuccess = true;
        } catch (IOException e) {
            LogUtils.e(TAG, e.toString());
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
    public static String saveBitmap(Bitmap bitmap) {
        return saveBitmap(bitmap, 100);
    }

    /**
     * 保存图片的方法
     *
     * @param bitmap Bitmap
     * @return 返回保存图片路径，null 保存失败
     */
    @Nullable
    public static String saveBitmap(Bitmap bitmap, int quality) {
        if (bitmap == null) {
            return null;
        }
        File dir = new File(Environment.getExternalStorageDirectory(), "Album");
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }
        String fileName = PrimaryUtils.createPrimary() + ".jpg";
        File file = new File(dir, fileName);
        if (saveBitmap(bitmap, file.getAbsolutePath(), quality)) {
            return file.getPath();
        }
        return null;
    }

    /**
     * 保存图片的方法
     *
     * @param bitmap   Bitmap
     * @param filePath 保存文件路径
     * @param quality  保存质量
     * @return true 保存成功， false 保存失败
     */
    public static boolean saveBitmap(Bitmap bitmap, String filePath, int quality) {
        if (bitmap == null) {
            return false;
        }
        boolean saveSuccess = false;
        File file = new File(filePath);
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            return false;
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
            fos.flush();
            saveSuccess = true;
        } catch (IOException e) {
            LogUtils.e(TAG, e.toString());
        }
        return saveSuccess;
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
        LogUtils.d(TAG, "准备裁剪, previewRect = " + previewRect.toString() + ", cropRect = " + cropRect);
        long timestamp = System.currentTimeMillis();

        int cropLeft = cropRect.left;
        int cropTop = cropRect.top;
        int cropWidth = cropRect.width();
        int cropHeight = cropRect.height();

        cropLeft = (int) (1.0f * cropLeft / previewRect.width() * bitmap.getWidth());
        cropTop = (int) (1.0f * cropTop / previewRect.height() * bitmap.getHeight());
        cropWidth = (int) (1.0f * cropWidth / previewRect.width() * bitmap.getWidth());
        cropHeight = (int) (1.0f * cropHeight / previewRect.height() * bitmap.getHeight());
        LogUtils.d(TAG, "开始裁剪, cropLeft = " + cropLeft + ", cropTop = " + cropTop + ", cropWidth = " + cropWidth + ", cropHeight = " + cropHeight + ", time = " + (System.currentTimeMillis() - timestamp));
        Bitmap cropBitmap = Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight);
        LogUtils.d(TAG, "裁剪完成, time = " + (System.currentTimeMillis() - timestamp));
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

    /**
     * 计算图片比例缩放
     */
    public static byte[] getRecognizeImageByte(Bitmap bitmap, int w, int h, boolean circulation) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        // 计算压缩的比率
        float scaleHeight = ((float) h) / height;
        float scaleWidth = ((float) w) / width;
        // 获取想要缩放的matrix
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 获取新的bitmap
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        return compressImageByte(bitmap, circulation);
    }

    /**
     * 质量压缩方法（返回图片字节数组byte[]）
     */
    public static byte[] compressImageByte(Bitmap image, boolean circulation) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (circulation) {
            image.compress(Bitmap.CompressFormat.JPEG, 100, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
            int options = 90;
            while (baos.toByteArray().length / 1024 > 30) { // 循环判断如果压缩后图片是否大于30kb,大于继续压缩
                baos.reset(); // 重置baos即清空baos
                image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
                options -= 10;// 每次都减少10
            }
        } else {
            int options = 90;
            image.compress(Bitmap.CompressFormat.JPEG, 100, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
            while (baos.toByteArray().length / 1024 > 100 && options >= 75) { // 循环判断如果压缩后图片是否大于100kb且压缩比例不小于0.75,满足继续压缩
                baos.reset(); // 重置baos即清空baos
                image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
                options -= 5;// 每次都减少10
            }
        }
        return baos.toByteArray();
    }

    /**
     * 创建补位图片setPixel的方式设置图片  白色
     */
    public static Bitmap createBitmap(int w, int h) {
        Bitmap bitmapBlank = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        for (int i = 0; i < bitmapBlank.getHeight(); i++) {
            for (int j = 0; j < bitmapBlank.getWidth(); j++) {
                bitmapBlank.setPixel(j, i, Color.argb(255, 255, 255, 255));
            }
        }
        return bitmapBlank;
    }

    /**
     * 根据方向判断在不同位置拼接图片
     */
    private static Bitmap createBitmapForFateMix(Bitmap first, Bitmap second, int direction) {
        if (first == null) {
            return null;
        }
        if (second == null) {
            return first;
        }
        int fw = first.getWidth();
        int fh = first.getHeight();
        int sw = second.getWidth();
        int sh = second.getHeight();
        Bitmap newBitmap = null;
        if (direction == LEFT) {
            newBitmap = Bitmap.createBitmap(fw + sw, Math.max(fh, sh), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(newBitmap);
            canvas.drawBitmap(first, sw, 0, null);
            canvas.drawBitmap(second, 0, 0, null);
        } else if (direction == RIGHT) {
            newBitmap = Bitmap.createBitmap(fw + sw, Math.max(fh, sh), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(newBitmap);
            canvas.drawBitmap(first, 0, 0, null);
            canvas.drawBitmap(second, fw, 0, null);
        } else if (direction == TOP) {
            newBitmap = Bitmap.createBitmap(Math.max(sw, fw), fh + sh, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(newBitmap);
            canvas.drawBitmap(first, 0, sh, null);
            canvas.drawBitmap(second, 0, 0, null);
        } else if (direction == BOTTOM) {
            newBitmap = Bitmap.createBitmap(Math.max(sw, fw), fh + sh, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(newBitmap);
            canvas.drawBitmap(first, 0, 0, null);
            canvas.drawBitmap(second, 0, fh, null);
        }
        return newBitmap;
    }

    /**
     * 检查bitmap是否正方形的图
     */
    public static Bitmap toSquareBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        if (bitmap.getWidth() == bitmap.getHeight()) {
            // 正方形的图直接返回
            return bitmap;
        }

        int direction;
        int spaceWidth;
        int spaceHeight;
        if (bitmap.getWidth() > bitmap.getHeight()) {
            direction = BOTTOM;
            spaceWidth = bitmap.getWidth();
            spaceHeight = bitmap.getWidth() - bitmap.getHeight();
        } else {
            direction = RIGHT;
            spaceWidth = bitmap.getHeight() - bitmap.getWidth();
            spaceHeight = bitmap.getHeight();
        }
        Bitmap blackBitmap = createBitmap(spaceWidth, spaceHeight);
        Bitmap destBitmap = createBitmapForFateMix(bitmap, blackBitmap, direction);
        recycleBitmap(blackBitmap);
        return destBitmap;
    }

}
