package com.rhino.camera.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.rhino.log.LogUtils;

import java.io.ByteArrayOutputStream;

/**
 * @author LuoLin
 * @since Create on 2019/8/2.
 **/
public class NV21ByteUtils {

    /**
     * NV21字节数组转Bitmap
     */
    @Nullable
    public static Bitmap byteToBitmap(final byte[] bytes, final Camera camera, Camera.CameraInfo cameraInfo) {
        final int previewFormat = camera.getParameters().getPreviewFormat();
        final Camera.Parameters parameters = camera.getParameters();
        int width = parameters.getPreviewSize().width;
        int height = parameters.getPreviewSize().height;
        return byteToBitmap(bytes, width, height, previewFormat, cameraInfo.orientation);
    }

    /**
     * NV21字节数组转Bitmap
     */
    @Nullable
    public static Bitmap byteToBitmap(byte[] bytes, int imageWidth, int imageHeight, int format, int rotateOrientation) {
        try {
            YuvImage yuv = new YuvImage(bytes, format, imageWidth, imageHeight, null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuv.compressToJpeg(new Rect(0, 0, imageWidth, imageHeight), 100, out);
            byte[] b = out.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(b, 0, b.length);
            if (rotateOrientation == 0) {
                return bitmap;
            }
            Matrix matrix = new Matrix();
            matrix.postRotate(rotateOrientation);
            Bitmap matrixBitmap = Bitmap.createBitmap(bitmap, 0, 0, imageWidth, imageHeight, matrix, true);
            bitmap.recycle();
            return matrixBitmap;
        } catch (Exception e) {
            LogUtils.e(e.toString());
        }
        return null;
    }

    /**
     * 旋转270度
     */
    @Nullable
    public static byte[] rotate270(byte[] nv21_data, int width, int height) {
        try {
            int y_size = width * height;
            int buffser_size = y_size * 3 / 2;
            byte[] nv21_rotated = new byte[buffser_size];
            int i = 0;

            // Rotate the Y luma
            for (int x = width - 1; x >= 0; x--) {
                int offset = 0;
                for (int y = 0; y < height; y++) {
                    nv21_rotated[i] = nv21_data[offset + x];
                    i++;
                    offset += width;
                }
            }

            // Rotate the U and V color components
            i = y_size;
            for (int x = width - 1; x > 0; x = x - 2) {
                int offset = y_size;
                for (int y = 0; y < height / 2; y++) {
                    nv21_rotated[i] = nv21_data[offset + (x - 1)];
                    i++;
                    nv21_rotated[i] = nv21_data[offset + x];
                    i++;
                    offset += width;
                }
            }
            return nv21_rotated;
        } catch (Exception e) {
            LogUtils.e(e.toString());
        }
        return null;
    }

    /**
     * 旋转180度
     */
    @Nullable
    public static byte[] rotate180(byte[] nv21_data, int width, int height) {
        try {
            int y_size = width * height;
            int buffser_size = y_size * 3 / 2;
            byte[] nv21_rotated = new byte[buffser_size];
            int i = 0;
            int count = 0;
            for (i = y_size - 1; i >= 0; i--) {
                nv21_rotated[count] = nv21_data[i];
                count++;
            }
            for (i = buffser_size - 1; i >= y_size; i -= 2) {
                nv21_rotated[count++] = nv21_data[i - 1];
                nv21_rotated[count++] = nv21_data[i];
            }
            return nv21_rotated;
        } catch (Exception e) {
            LogUtils.e(e.toString());
        }
        return null;
    }

    /**
     * 旋转90度
     */
    @Nullable
    public static byte[] rotate90(byte[] nv21_data, int width, int height) {
        try {
            int y_size = width * height;
            int buffser_size = y_size * 3 / 2;
            byte[] nv21_rotated = new byte[buffser_size];

            // Rotate the Y luma
            int i = 0;
            int startPos = (height - 1) * width;
            for (int x = 0; x < width; x++) {
                int offset = startPos;
                for (int y = height - 1; y >= 0; y--) {
                    nv21_rotated[i] = nv21_data[offset + x];
                    i++;
                    offset -= width;
                }
            }
            // Rotate the U and V color components
            i = buffser_size - 1;
            for (int x = width - 1; x > 0; x = x - 2) {
                int offset = y_size;
                for (int y = 0; y < height / 2; y++) {
                    nv21_rotated[i] = nv21_data[offset + x];
                    i--;
                    nv21_rotated[i] = nv21_data[offset + (x - 1)];
                    i--;
                    offset += width;
                }
            }
            return nv21_rotated;
        } catch (Exception e) {
            LogUtils.e(e.toString());
        }
        return null;
    }

    /**
     * 旋转到固定角度
     */
    @NonNull
    public static NV21Result rotate(@NonNull byte[] bytes, int width, int height, int rotateDegree) {
        LogUtils.d("开始旋转, bytes.length = " + bytes.length + ", width = " + width + ", height = " + height + ", rotateDegree = " + rotateDegree);
        long timestamp = System.currentTimeMillis();
        NV21Result result = new NV21Result(bytes, width, height);
        if (rotateDegree == 270) {
            result.bytes = rotate270(bytes, width, height);
            result.width = height;
            result.height = width;
        } else if (rotateDegree == 180) {
            result.bytes = rotate180(bytes, width, height);
            result.width = width;
            result.height = height;
        } else if (rotateDegree == 90) {
            result.bytes = rotate90(bytes, width, height);
            result.width = height;
            result.height = width;
        }
        LogUtils.d("旋转完成, bytes.length = " + (result.bytes != null ? result.bytes.length : -1) + ", time = " + (System.currentTimeMillis() - timestamp));
        return result;
    }

    /**
     * NV21裁剪
     *
     * @param bytes        NV21图片数据
     * @param width        NV21图片宽
     * @param height       NV21图片高
     * @param rotateDegree 旋转角度
     * @param previewRect  预览坐标
     * @param cropRect     相对预览裁剪坐标
     * @return
     */
    @Nullable
    public static NV21Result rotateAndCrop(byte[] bytes, int width, int height, int rotateDegree, Rect previewRect, Rect cropRect) {
        LogUtils.d("准备裁剪, bytes.length = " + bytes.length + ", width = " + width + ", height = " + height + ", rotateDegree = " + rotateDegree + ", previewRect = " + previewRect.toString() + ", cropRect = " + cropRect);
        long timestamp = System.currentTimeMillis();
        NV21Result result = rotate(bytes, width, height, rotateDegree);

        int cropLeft = cropRect.left;
        int cropTop = cropRect.top;
        int cropWidth = cropRect.width();
        int cropHeight = cropRect.height();

        if (cropLeft > previewRect.left || cropTop > previewRect.top) {
            cropLeft = (int) (1.0f * cropLeft / previewRect.width() * result.width);
            cropTop = (int) (1.0f * cropTop / previewRect.height() * result.height);
            cropWidth = (int) (1.0f * cropWidth / previewRect.width() * result.width);
            cropHeight = (int) (1.0f * cropHeight / previewRect.height() * result.height);
            LogUtils.d("开始裁剪, cropLeft = " + cropLeft + ", cropTop = " + cropTop + ", cropWidth = " + cropWidth + ", cropHeight = " + cropHeight + ", time = " + (System.currentTimeMillis() - timestamp));
            result = crop(result.bytes, result.width, result.height, cropLeft, cropTop, cropWidth, cropHeight);
            LogUtils.d("裁剪完成, cropBytes.length = " + (result != null && result.bytes != null ? result.bytes.length : -1) + ", time = " + (System.currentTimeMillis() - timestamp));
        }
        return result;
    }

    /**
     * NV21裁剪
     *
     * @param bytes       源数据
     * @param width       源宽
     * @param height      源高
     * @param crop_left   相对图片顶点坐标
     * @param crop_top    相对图片顶点坐标
     * @param crop_width  相对图片裁剪后的宽
     * @param crop_height 相对图片裁剪后的高
     * @return 裁剪后的数据
     */
    @Nullable
    public static NV21Result crop(byte[] bytes, int width, int height, int crop_left, int crop_top, int crop_width, int crop_height) {
        try {
            if (crop_left > width || crop_top > height) {
                return null;
            }
            //取偶
            int x = crop_left / 2 * 2, y = crop_top / 2 * 2;
            int w = crop_width / 2 * 2, h = crop_height / 2 * 2;
            int y_unit = w * h;
            int src_unit = width * height;
            int uv = y_unit >> 1;
            byte[] nData = new byte[y_unit + uv];

            for (int i = y, len_i = y + h; i < len_i; i++) {
                for (int j = x, len_j = x + w; j < len_j; j++) {
                    nData[(i - y) * w + j - x] = bytes[i * width + j];
                    nData[y_unit + ((i - y) / 2) * w + j - x] = bytes[src_unit + i / 2 * width + j];
                }
            }
            return new NV21Result(nData, w, h);
        } catch (Exception e) {
            LogUtils.e(e.toString());
        }
        return null;
    }

    /**
     * NV21裁剪
     *
     * @param bytes    源数据
     * @param width    源宽
     * @param height   源高
     * @param cropRect 相对图片大小的坐标
     * @return 裁剪后的数据
     */
    @Nullable
    public static NV21Result crop(byte[] bytes, int width, int height, Rect cropRect) {
        return crop(bytes, width, height, cropRect.left, cropRect.top, cropRect.width(), cropRect.height());
    }

    public static class NV21Result {
        public byte[] bytes;
        public int width;
        public int height;

        public NV21Result(byte[] bytes, int width, int height) {
            this.bytes = bytes;
            this.width = width;
            this.height = height;
        }
    }

}
