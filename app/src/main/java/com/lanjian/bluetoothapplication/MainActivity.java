package com.lanjian.bluetoothapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
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
import com.lanjian.bluetoothapplication.service.BluetoothService;
import com.lanjian.bluetoothapplication.service.BluetoothServiceListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

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
    private BluetoothService mBluetoothService;

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
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
               BluetoothDevice device = (BluetoothDevice) deviceListAdapter.getItem(position);
                //连接设备
               // connectDevice(device);
                if (mBluetoothService != null) {
                    //根据MAC地址远程获取一个蓝牙设备，这里固定了，实际开发中，需要动态设置参数（MAC地址）
                    if (device != null) {
                        //成功获取到远程蓝牙设备
                        mBluetoothService.connect(device);
                    }
                }
            }
        });

        mBluetoothService.setBluetoothServiceListener(new BluetoothServiceListener() {
            @Override
            public void connectionFailed() {
                Log.e("tag","connectionFailed");
            }

            @Override
            public void connectionLost() {
                Log.e("tag","connectionLost");
            }
        });
    }

    private void initData() {
        //创建蓝牙列表适配器
        bluetoothDevices = new ArrayList<>();
        deviceListAdapter = new BlueToothDeviceListAdapter(bluetoothDevices);
        deviceLv.setAdapter(deviceListAdapter);
        //注册蓝牙搜索链接的广播
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
        // 3、创建自定义蓝牙服务对象
        if (mBluetoothService == null) {
            mBluetoothService = new BluetoothService();
        }
    }

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
                bluetoothAdapter.startDiscovery();
                break;
            case R.id.sendMessage_btn:
                //发送消息
                if (mBluetoothService != null) {
                    mBluetoothService.sendMsg(sendMessageEt.getText().toString());
                }
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

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //避免重复添加已经绑定过的设备
                //if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    //此处的adapter是列表的adapter，不是BluetoothAdapter
                    bluetoothDevices.add(device);
                    deviceListAdapter.notifyDataSetChanged();
                    Toast.makeText(MainActivity.this, device.getName(), Toast.LENGTH_SHORT).show();
                //}
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Toast.makeText(MainActivity.this, "开始搜索", Toast.LENGTH_SHORT).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(MainActivity.this, "搜索完毕", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if (mBluetoothService!=null){
            mBluetoothService.stop();
        }
    }

}
