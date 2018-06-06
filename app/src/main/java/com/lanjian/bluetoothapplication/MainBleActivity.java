package com.lanjian.bluetoothapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lanjian.bluetoothapplication.adapter.BlueToothDeviceListAdapter;
import com.lanjian.bluetoothapplication.utils.ConfigUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainBleActivity extends AppCompatActivity {

    @BindView(R.id.openBluetooth_btn)
    Button openBluetoothBtn;
    @BindView(R.id.searchDevice_btn)
    Button searchDeviceBtn;
    @BindView(R.id.connectState_tv)
    TextView connectStateTv;
    @BindView(R.id.sendMessage_btn)
    Button sendMessageBtn;
    @BindView(R.id.sendMessage_tv)
    TextView sendMessageTv;
    @BindView(R.id.device_lv)
    ListView deviceLv;
    @BindView(R.id.sendMessage_et)
    EditText sendMessageEt;

    private List<BluetoothDevice> bluetoothDevices;
    private BlueToothDeviceListAdapter deviceListAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt mGatt;

    // Initializes Bluetooth adapter.
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initData();
        setListener();
    }

    private void setListener() {
        deviceLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                BluetoothDevice device = (BluetoothDevice) deviceListAdapter.getItem(position);
                //连接设备
                // connectDevice(device);

                for (int i = 0;i<device.getUuids().length;i++){
                    Log.e("tag","device.getUuids()["+i+"]="+device.getUuids()[i]);
                }
                mGatt = device.connectGatt(MainBleActivity.this, false, new BluetoothGattCallback() {
                    @Override
                    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                        super.onPhyUpdate(gatt, txPhy, rxPhy, status);
                    }

                    @Override
                    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
                        super.onPhyRead(gatt, txPhy, rxPhy, status);
                    }

                    @Override
                    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                        super.onConnectionStateChange(gatt, status, newState);
                        Log.e("onConnectionStateChange","status="+status+"newState="+newState+"="+BluetoothProfile.STATE_CONNECTED);
                        if (status == BluetoothGatt.GATT_SUCCESS) {

                            if (newState == BluetoothGatt.STATE_CONNECTED) {
                                mGatt.discoverServices();
                            } else if  (newState == BluetoothGatt.STATE_DISCONNECTED) {
                                close(); // 防止出现status 133
                                Log.i("tag", "Disconnected from GATT server.");
                            }

                        } else {
                            Log.d("tag", "onConnectionStateChange received: "  + status);
                            close(); // 防止出现status 133

                        }
                    }

                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        super.onServicesDiscovered(gatt, status);
                        Log.e("onServicesDiscovered","status="+status+"==="+BluetoothGatt.GATT_SUCCESS);
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            //寻找到服务
                            //寻找服务之后，我们就可以和设备进行通信，比如下发配置值，获取设备电量什么的
                            mGatt.getServices();
                            for (int i = 0;i<mGatt.getServices().size();i++){
                                List<BluetoothGattCharacteristic> services = mGatt.getServices().get(i).getCharacteristics();
                                for (int j = 0;j<services.size();j++){
                                    Log.e("tag","services"+j+"="+services.get(j).getUuid());
                                }
                                for (int j = 0;j<services.get(j).getDescriptors().size();j++){
                                    Log.e("tag","getDescriptors"+j+"="+services.get(j).getDescriptors().get(i).getUuid());
                                }
                                Log.e("tag","mGatt.getServices()"+i+"="+mGatt.getServices().get(i).getIncludedServices());
                            }
                            readBatrery();  //读取电量操作
                            sendSetting(); //下发配置值

                        }
                    }

                    @Override
                    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicRead(gatt, characteristic, status);
                        Log.e("tag","onCharacteristicRead=="+gatt.getDevice().getAddress());
                        if (characteristic.getUuid().toString()
                                .equals("00002a01-0000-1000-8000-00805f9b34fb")) {// 获取到电量
                            int battery = characteristic.getValue()[0];
                            Log.e("tag","battery=="+battery);
                        }
                    }

                    @Override
                    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        super.onCharacteristicWrite(gatt, characteristic, status);
                        Log.e("tag","onCharacteristicWrite==");
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            //write成功（发送值成功），可以根据 characteristic.getValue()来判断是哪个值发送成功了，
                            // 比如 连接上设备之后你有一大串命令需要下发，你调用多次写命令，
                            // 这样你需要判断是不是所有命令都成功了，因为android不太稳定，有必要来check命令是否成功，
                            // 否则你会发现你明明调用 写命令，但是设备那边不响应
                            Log.e("tag","onCharacteristicWrite");
                        }
                    }

                    @Override
                    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                        super.onCharacteristicChanged(gatt, characteristic);
                    }

                    @Override
                    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                        super.onDescriptorRead(gatt, descriptor, status);
                    }

                    @Override
                    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                        super.onDescriptorWrite(gatt, descriptor, status);
                    }

                    @Override
                    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
                        super.onReliableWriteCompleted(gatt, status);
                    }

                    @Override
                    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                        super.onReadRemoteRssi(gatt, rssi, status);
                    }

                    @Override
                    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
                        super.onMtuChanged(gatt, mtu, status);
                    }
                });
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void initData() {
        //创建蓝牙列表适配器
        bluetoothDevices = new ArrayList<>();
        deviceListAdapter = new BlueToothDeviceListAdapter(bluetoothDevices);
        deviceLv.setAdapter(deviceListAdapter);
        BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "当前设备不支持蓝牙功能", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            //如果蓝牙还没打开
            /*
            发送打开蓝牙的意图
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(i);*/
            bluetoothAdapter.enable();//强制打开
        }
        //注册蓝牙搜索链接的广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @OnClick({R.id.openBluetooth_btn, R.id.searchDevice_btn, R.id.sendMessage_btn})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.openBluetooth_btn:
                //打开蓝牙
                if (openBluetooth()) return;
                //开启被其它蓝牙设备发现的功能
                if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    //设置为一直开启,最后的参数设置为0，可以让蓝牙设备一直处于可发现状态。
                    // 当我们需要设置具体可被发现的时间时，最多只能设置300秒。
                    i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                    startActivity(i);
                }
                break;
            case R.id.searchDevice_btn:
                //搜索蓝牙
                if (openBluetooth()) return;
                bluetoothDevices.clear();
                //bluetoothAdapter.startDiscovery();
                bluetoothDevices.clear();
                bluetoothAdapter.startDiscovery();
                break;
            case R.id.sendMessage_btn:
                //发送消息

                break;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private boolean openBluetooth() {
        //bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothManager mbluetoothmanager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = mbluetoothmanager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "当前设备不支持蓝牙功能", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (!bluetoothAdapter.isEnabled()) {
            //如果蓝牙还没打开
            /*
            发送打开蓝牙的意图
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(i);*/
            bluetoothAdapter.enable();//强制打开
        }
        return false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //避免重复添加已经绑定过的设备
                //if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                //此处的adapter是列表的adapter，不是BluetoothAdapter
                bluetoothDevices.add(device);
                deviceListAdapter.notifyDataSetChanged();
                Toast.makeText(MainBleActivity.this, device.getName(), Toast.LENGTH_SHORT).show();
                //}
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Toast.makeText(MainBleActivity.this, "开始搜索", Toast.LENGTH_SHORT).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(MainBleActivity.this, "搜索完毕", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public
    void  close() {

        if(mGatt == null) {
            return;
        }
        Log.w("tag", "mBluetoothGatt closed");
        mGatt.close();
        mGatt = null;

    }

    /***读操作***/
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    void   readBatrery(){
        //如上面所说，想要和一个学生通信，先知道他的班级（ServiceUUID）和学号（CharacUUID）
        BluetoothGattService batteryService= mGatt.getService(
                UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"));
        //此处的0000180f...是举例，实际开发需要询问硬件那边
        if(batteryService!=null){
            BluetoothGattCharacteristic batteryCharacteristic=batteryService.getCharacteristic(
                    UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb"));
            //此处的00002a19...是举例，实际开发需要询问硬件那边
            if(batteryCharacteristic!=null){
                mGatt.readCharacteristic(batteryCharacteristic);
                //读取电量, 这是读取batteryCharacteristic值的方法，读取其他的值也是如此，只是它们的ServiceUUID 和CharacUUID不一样
            }
        }else{
            Log.e("tag","batteryService == null");
        }
    }
    /***写操作***/
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    void   sendSetting(){
        BluetoothGattService sendService= mGatt.getService(
                UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"));
        //此处的00001805...是举例，实际开发需要询问硬件那边
        Log.e("tag","======================");
        if(sendService!=null){
            BluetoothGattCharacteristic sendCharacteristic=sendService.getCharacteristic(
                    UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb"));
            //此处的00002a08...是举例，实际开发需要询问硬件那边
            if(sendCharacteristic!=null){
                sendCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                sendCharacteristic.setValue(new byte[] { 0x01,0x20,0x03  });//随便举个数据
                mGatt.writeCharacteristic(sendCharacteristic);//写命令到设备，
                Log.e("tag","///////////////////////");
            }else{
                Log.e("tag","sendCharacteristic==null");
            }
        }else{
            Log.e("tag","sendService==null");
        }
        Log.e("tag","***************");
    }

}
