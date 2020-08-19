package com.rhino.camera.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 * @author LuoLin
 * @since Create on 2019/8/29.
 **/
public class PrimaryUtils {

    /**
     * 获取唯一标识 yyyyMMddHHmmssSSS + 6位随机数
     */
    @NonNull
    public static String createPrimary() {
        Random random = new Random();
        int randomNumber = 100000 + random.nextInt(899999);
        return new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.getDefault()).format(new Date()) + randomNumber;
    }

}
