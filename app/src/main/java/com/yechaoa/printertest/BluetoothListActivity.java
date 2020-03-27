package com.yechaoa.printertest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

/**
 * 主要负责打开、搜索、显示蓝牙
 */
public class BluetoothListActivity extends Activity {

    private ListView lvPairedDevice;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mDevicesArrayAdapter;
    public static final String EXTRA_DEVICE_ADDRESS = "address";
    public static final int REQUEST_ENABLE_BT = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.dialog_bluetooth_list);

        lvPairedDevice = findViewById(R.id.lvPairedDevices);
        findViewById(R.id.btBluetoothScan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setVisibility(View.GONE);
                discoveryDevice();
            }
        });

        // 设置广播信息过滤 并注册
        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mFindBlueToothReceiver, filter);
        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mFindBlueToothReceiver, filter);

        initBluetooth();
    }

    /**
     * 初始化蓝牙
     */
    private void initBluetooth() {
        // 获取蓝牙适配器
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 检查蓝牙是否可用
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "当前设备不支持蓝牙", Toast.LENGTH_SHORT).show();
        } else {
            // 检查蓝牙是否打开
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {
                getDeviceList();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                // bluetooth is opened
                getDeviceList();
            } else {
                // bluetooth is not open
                Toast.makeText(this, "蓝牙没有开启", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 蓝牙设备列表
     */
    protected void getDeviceList() {
        // 初始化一个数组适配器，用来显示已匹对和未匹对的设备
        mDevicesArrayAdapter = new ArrayAdapter<>(this, R.layout.bluetooth_device_name_item);
        lvPairedDevice.setAdapter(mDevicesArrayAdapter);
        lvPairedDevice.setOnItemClickListener(mDeviceClickListener);
        // 已匹对数据
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // 添加一个item显示信息
        mDevicesArrayAdapter.add("已配对：");
        if (pairedDevices.size() > 0) {
            //遍历填充数据
            for (BluetoothDevice device : pairedDevices) {
                mDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            mDevicesArrayAdapter.add("没有已配对设备");
        }
    }

    /**
     * 接收扫描设备的广播
     * changes the title when discovery is finished
     */
    private final BroadcastReceiver mFindBlueToothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // 每当发现一个蓝牙设备时
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                //获取设备
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed
                // 未匹对的情况下添加显示
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // 扫描结束
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle("选择蓝牙设备");
                //此处-2是减去我们手动添加的两个区分显示的item
                Log.i("tag", "finish discovery" + (mDevicesArrayAdapter.getCount() - 2));
                if (mDevicesArrayAdapter.getCount() == 0) {
                    mDevicesArrayAdapter.add("没有找到蓝牙设备");
                }
            }
        }
    };

    /**
     * 扫描设备
     */
    private void discoveryDevice() {
        setProgressBarIndeterminateVisibility(true);
        setTitle("扫描中");
        // 添加一个item区分显示信息
        mDevicesArrayAdapter.add("未配对：");
        // If we're already discovering, stop it
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        // 开始扫描，每扫描到一个设备，都会发送一个广播
        mBluetoothAdapter.startDiscovery();
    }

    /**
     * The on-click listener for all devices in the ListViews
     */
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String noDevices = "没有已配对设备";
            String noNewDevice = "没有找到蓝牙设备";
            Log.i("TAG", info);
            // info 不是我们手动添加的信息 即表示为真实蓝牙设备信息
            if (!info.equals(noDevices) && !info.equals(noNewDevice) && !info.equals("未配对") && !info.equals("已配对")) {
                mBluetoothAdapter.cancelDiscovery();
                //mac 地址
                String address = info.substring(info.length() - 17);
                // 设置信息并返回
                // Set result and finish this Activity
                Intent intent = new Intent();
                intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Make sure we're not doing discovery anymore
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        // Unregister broadcast listeners
        unregisterReceiver(mFindBlueToothReceiver);
    }
}
