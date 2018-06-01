package com.lanjian.bluetoothapplication.service;

/**
 * @author lanjian
 * @email 819715035@qq.com
 * creat at $date$
 * description
 */
public interface BluetoothServiceListener {
    //连接失败
     void connectionFailed();
    // 连接丢失
     void connectionLost();
}
