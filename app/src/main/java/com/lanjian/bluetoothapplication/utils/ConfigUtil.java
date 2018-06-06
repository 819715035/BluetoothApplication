package com.lanjian.bluetoothapplication.utils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * @author lanjian
 * @email 819715035@qq.com
 * creat at $date$
 * description
 */
public class ConfigUtil {
    public static final String SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB";
   // public static final String SPP_UUID = "0000110a-0000-1000-8000-00805f9b34fb"; //文件传输
  // public static final String SPP_UUID = "00001105-0000-1000-8000-00805F9B34FB"; //信息同步
   // public static final String SPP_UUID = "00001115-0000-1000-8000-00805f9b34fb"; //个人局域网服务
   // public static final String SPP_UUID = "00001116-0000-1000-8000-00805f9b34fb"; //个人局域网服务
    //public static final String SPP_UUID = "00001112-0000-1000-8000-00805f9b34fb"; //蓝牙传真
   // public static final String SPP_UUID = "0000111f-0000-1000-8000-00805f9b34fb"; //个人局域网服务
   // public static final String SPP_UUID = "00000000-0000-1000-8000-00805f9b34fb";
    //public static final String SPP_UUID = "679a0c20-0008-998a-de11-acafd0c088fa";
   // public static final String SPP_UUID = "ffffffff-f6ce-3d2c-ffff-ffff8052061b"; //魅族id
   // public static final String SPP_UUID = "00002a05-0000-1000-8000-00805f9b34fb"; //魅族id
   // public static final String SPP_UUID = "00002a00-0000-1000-8000-00805f9b34fb"; //魅族id
   // public static final String SPP_UUID = "00002a01-0000-1000-8000-00805f9b34fb"; //魅族id
   // public static final String SPP_UUID = "00002aa6-0000-1000-8000-00805f9b34fb"; //魅族id
   // public static final String SPP_UUID = "00001800-0000-1000-8000-00805f9b34fb"; //荣耀8id
   // public static final String SPP_UUID = "00002a00-0000-1000-8000-00805f9b34fb"; //荣耀8id
   // public static final String SPP_UUID = "00002a01-0000-1000-8000-00805f9b34fb"; //荣耀8id

    public static final UUID uuid = UUID.fromString(SPP_UUID);

    /**
     * 与设备配对 参考源码：platform/packages/apps/Settings.git
     * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
     */
    static public boolean createBond(Class btClass,BluetoothDevice btDevice) throws Exception {
        Method createBondMethod = btClass.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    /**
     * 与设备解除配对 参考源码：platform/packages/apps/Settings.git
     * /Settings/src/com/android/settings/bluetooth/CachedBluetoothDevice.java
     */
    static public boolean removeBond(Class btClass,BluetoothDevice btDevice) throws Exception {
        Method removeBondMethod = btClass.getMethod("removeBond");
        Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }

    /**
     *
     * @param clsShow
     */
    static public void printAllInform(Class clsShow) {
        try {
            // 取得所有方法
            Method[] hideMethod = clsShow.getMethods();
            int i = 0;
            for (; i < hideMethod.length; i++) {
                Log.e("method name", hideMethod[i].getName());
            }
            // 取得所有常量
            Field[] allFields = clsShow.getFields();
            for (i = 0; i < allFields.length; i++) {
                Log.e("Field name", allFields[i].getName());
            }
        } catch (SecurityException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    static public boolean setPin(Class btClass, BluetoothDevice btDevice,
                                 String str) throws Exception
    {
        try
        {
            Method removeBondMethod = btClass.getDeclaredMethod("setPin",
                    new Class[]
                            {byte[].class});
            Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice,
                    new Object[]
                            {str.getBytes()});
            Log.d("returnValue", "setPin is success " +btDevice.getAddress()+ returnValue.booleanValue());
        }
        catch (SecurityException e)
        {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        }
        catch (IllegalArgumentException e)
        {
            // throw new RuntimeException(e.getMessage());
            e.printStackTrace();
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;

    }

    // 取消用户输入
    @SuppressWarnings("unchecked")
    static public boolean cancelPairingUserInput(Class btClass,
                                                 BluetoothDevice device)

            throws Exception
    {
        Method createBondMethod = btClass.getMethod("cancelPairingUserInput");
        // cancelBondProcess()
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        Log.d("returnValue", "cancelPairingUserInput is success " + returnValue.booleanValue());
        return returnValue.booleanValue();
    }

    // 取消配对
    @SuppressWarnings("unchecked")
    static public boolean cancelBondProcess(Class btClass,
                                            BluetoothDevice device)

            throws Exception
    {
        Method createBondMethod = btClass.getMethod("cancelBondProcess");
        Boolean returnValue = (Boolean) createBondMethod.invoke(device);
        return returnValue.booleanValue();
    }


        @SuppressLint("MissingPermission")
        public static String getUniqueID(Context context){
            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            final String tmDevice, tmSerial, androidId;
            tmDevice = "" + tm.getDeviceId();
            tmSerial = "" + tm.getSimSerialNumber();
            androidId = "" + android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
            return deviceUuid.toString();
        }
}
