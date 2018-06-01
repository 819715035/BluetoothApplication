package com.lanjian.bluetoothapplication.utils;

import java.util.UUID;

/**
 * @author lanjian
 * @email 819715035@qq.com
 * creat at $date$
 * description
 */
public class ConfigUtil {
    private static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    //private static final String SPP_UUID = "8ce255c0-200a-11e0-ac64-0800200c9a66";// 不安全连接
//    private static final String SPP_UUID = "fa87c0d0-afac-11de-8a39-0800200c9a66";// 安全连接
    public static final UUID uuid = UUID.fromString(SPP_UUID);
}
