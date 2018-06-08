package com.lanjian.bluetoothapplication.service;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import com.lanjian.bluetoothapplication.utils.ConfigUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BluetoothService {
    private OutputStream mmOutStream;
    //蓝牙适配器
    private BluetoothAdapter mAdapter;
    private AcceptThread mAcceptThread;// 请求连接的监听进程
    private ConnectThread mConnectThread;// 连接一个设备的进程
    private ConnectedThread mConnectedThread;
    private BluetoothServiceListener bluetoothServiceListener;

    public void setBluetoothServiceListener(BluetoothServiceListener bluetoothServiceListener) {
        this.bluetoothServiceListener = bluetoothServiceListener;
    }

    public BluetoothService() {
        mAdapter = BluetoothAdapter.getDefaultAdapter();//获取蓝牙适配器
    }

    // 连接硬件设备
    public synchronized void connect(BluetoothDevice device) {
        mConnectThread=new ConnectThread(device);
        mConnectThread.start();
    }

    private class ConnectThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            try {
                //安全连接
               // tmp = device.createRfcommSocketToServiceRecord(ConfigUtil.uuid);// Get a BluetoothSocket for a connection with the given BluetoothDevice
                //不安全连接
                tmp = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString(ConfigUtil.HONOR_UUID));// Get a BluetoothSocket for a connection with the given BluetoothDevice
            }
            catch (IOException e) {}
            mmSocket = tmp;
        }

        public void run() {

            setName("ConnectThread");
            //当连接成功，取消蓝牙适配器搜索蓝牙设备的操作，因为搜索操作非常耗时
            mAdapter.cancelDiscovery();// Always cancel discovery because it will slow down a connection

            try {
                mmSocket.connect();
                // This is a blocking call and will only return on a successful connection or an exception
            } catch (IOException e) {
                if (bluetoothServiceListener!=null){
                    bluetoothServiceListener.connectionFailed();
                }
                try {
                    mmSocket.close();
                } catch (IOException e2) {}

                BluetoothService.this.start();// 引用来说明要调用的是外部类的方法 run
                return;
            }

            synchronized (BluetoothService.this) {// Reset the ConnectThread because we're done
                mConnectThread = null;
            }
            connected(mmSocket, mmDevice);// Start the connected thread
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


    class ConnectedThread extends Thread{
        private BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private List<Byte> queueBuffer = new ArrayList<>();
        private StringBuffer sb = new StringBuffer();
        //构造方法
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {}

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            byte[] tempInputBuffer = new byte[1024];
            int acceptedLen; //记录每次读取数据的数据长度
            while(true){
                try {
                    queueBuffer.clear();
                    sb.delete(0,sb.length());
                    acceptedLen = mmInStream.read(tempInputBuffer);//返回接收的长度
                    //从缓冲区中读取数据
                    for (int i = 0; i < acceptedLen; i++) {
                        queueBuffer.add(tempInputBuffer[i]);
                    }
                    byte[] bytes = new byte[acceptedLen];
                    System.arraycopy(tempInputBuffer,0,bytes,0,acceptedLen);
                    sb.append(new String(bytes,"GB2312"));
                    Log.e("tag","接收到的数据："+queueBuffer.toString());
                    Log.e("tag","接收到的数据sb："+sb.toString());


                } catch (IOException e) {
                    if (bluetoothServiceListener!=null){
                        bluetoothServiceListener.connectionLost();
                    }
                    Log.e("tag","youcuo");
                    e.printStackTrace();
                }
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {}
        }
    }

    //用于 蓝牙连接的Activity onResume()方法
    public synchronized void start() {
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }

    public synchronized void connected(BluetoothSocket socket,BluetoothDevice device) {
        Log.d("MAGIKARE","连接到线程");
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);

        mConnectedThread.start();
    }

    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        //private int index;
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            // this.index=index;
            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord("BluetoothData", UUID.fromString(ConfigUtil.HONOR_UUID));
            }
            catch (IOException e) {}
            mmServerSocket = tmp;
        }

        public void run() {
            new Thread(new Runnable() {
                @Override
                public void run() {

                }
            }).start();

        }

        public void cancel() {

            try {
                if(mmServerSocket!=null) {
                    mmServerSocket.close();
                }
            }
            catch (IOException e) {}
        }
    }


    public synchronized void stop() {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
    }

    /**
     * 发送数据
     *
     * @param msg
     */
    public void sendMsg(final String msg) {
        if (mmOutStream != null) {
            try {
                byte[] bytes = msg.getBytes("gb2312");
                //发送数据
                mmOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
                //发送错误
            }
        }
    }
}