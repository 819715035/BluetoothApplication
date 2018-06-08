package com.lanjian.bluetoothapplication;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lanjian.bluetoothapplication.adapter.BlueToothDeviceListAdapter;
import com.lanjian.bluetoothapplication.service.BluetoothService;
import com.lanjian.bluetoothapplication.service.BluetoothServiceListener;
import com.lanjian.bluetoothapplication.utils.ConfigUtil;
import com.lanjian.bluetoothapplication.utils.HidUtil;
import com.lanjian.bluetoothapplication.utils.WIFIConfingUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class HidActivity extends AppCompatActivity{
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
    private HidUtil mHidUtil;

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
                device = (BluetoothDevice) deviceListAdapter.getItem(position);
                Log.e("tag",device.getName()+device.getAddress());
                if (device == null) return;
                String btname = device.getName();
                String address = device.getAddress();
                Log.i("tag", "bluetooth name:"+btname+",address:"+address);
                if(!mHidUtil.isBonded(device)){
                    //先配对
                    mHidUtil.pair(device);
                }else {
                    //已经配对则直接连接
                    mHidUtil.connect(device);
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
        mHidUtil = HidUtil.getInstance(this);
        //创建蓝牙列表适配器
        bluetoothDevices = new ArrayList<>();
        deviceListAdapter = new BlueToothDeviceListAdapter(bluetoothDevices);
        deviceLv.setAdapter(deviceListAdapter);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "不支持蓝牙功能", 0).show();
            // 不支持蓝牙
            return;
        }
// 如果没有打开蓝牙
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
// 初始化广播接收者
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED");
        this.registerReceiver(mReceiver, intentFilter);
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
                bluetoothAdapter.startDiscovery();
                for(Iterator iterator = bluetoothAdapter.getBondedDevices().iterator(); iterator.hasNext();){
                    BluetoothDevice device = (BluetoothDevice) iterator.next();
                    bluetoothDevices.add(device);
                    deviceListAdapter.notifyDataSetChanged();
                }
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

    private BluetoothDevice device;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.e("tag","action=="+action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //避免重复添加已经绑定过的设备
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    //此处的adapter是列表的adapter，不是BluetoothAdapter
                    bluetoothDevices.add(device);
                    deviceListAdapter.notifyDataSetChanged();
                    Toast.makeText(HidActivity.this, device.getName(), Toast.LENGTH_SHORT).show();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Toast.makeText(HidActivity.this, "开始搜索", Toast.LENGTH_SHORT).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(HidActivity.this, "搜索完毕", Toast.LENGTH_SHORT).show();
            }else if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                String name = device.getName();
                String address = device.getAddress();
                Log.i("tag","name:"+name+",address:"+address+",bondstate:"+device.getBondState());
                if(device.getBondState() == BluetoothDevice.BOND_BONDED)
                    mHidUtil.connect(device);
            } else if(action.equals("android.bluetooth.input.profile.action.CONNECTION_STATE_CHANGED")){
                int state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE,0);
                Log.i("tag","state="+state+",device="+device);
                if(state == BluetoothProfile.STATE_CONNECTED){//连接成功
                    Toast.makeText(HidActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                } else if(state == BluetoothProfile.STATE_DISCONNECTED){//连接失败
                    Toast.makeText(HidActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if (device!=null){
            mHidUtil.disConnect(device);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.e("tag","keycode=="+keyCode);
        switch (keyCode){
            //遥控
            case 96:
                //按下0键
                return true;
            case 97:
                //按下返回键
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(sendMessageEt.getWindowToken(), 0);//（关闭软键盘。。。）
                return true;
            case 24:
                //按下向上键或者d键或者按下放大键
                return true;
            case 25:
                //按下向下键或者c键或者按下缩小建
                return true;
            case 88:
                //按下向左键
                return true;
            case 85:
                //按下a键
                return true;

            //自拍杆
            case 10:
                //切换镜头
                return true;
            case 11:
                //录像
                return true;
            case 307:
            case 401:
            case 303:
            case 304:
                //录像按钮
                return true;
        }
        return super.onKeyDown(keyCode,event);
    }
}
