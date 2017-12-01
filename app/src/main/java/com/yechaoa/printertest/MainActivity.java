package com.yechaoa.printertest;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.gprinter.aidl.GpService;
import com.gprinter.command.EscCommand;
import com.gprinter.command.GpCom;
import com.gprinter.command.GpUtils;
import com.gprinter.command.LabelCommand;
import com.gprinter.io.GpDevice;
import com.gprinter.service.GpPrintService;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

// TODO: 2017/11/29 ACCESS_COARSE_LOCATION权限需要适配

public class MainActivity extends AppCompatActivity {

    private PrinterServiceConnection conn = null;
    private GpService mGpService = null;
    private BluetoothSocket socket;//蓝牙socket
    private ConnectThread mThread;//连接的蓝牙线程
    private MyBroadcastReceiver receiver;//蓝牙搜索的广播
    private BluetoothAdapter adapter;//蓝牙适配器
    private List<BluetoothBean> mBluetoothList;//搜索的蓝牙设备
    private List<BluetoothBean> mBluetoothList2;//去重的蓝牙设备
    private PopupWindow pw;
    private String TAG = "main";
    private ProgressDialog pdSearch;//搜索时
    private ProgressDialog pdConnect;//连接时

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startService();
        connection();

        findViewById(R.id.button_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchBlueToothDevice();
            }
        });
    }

    private void startService() {
        Intent i = new Intent(this, GpPrintService.class);
        startService(i);
    }

    private void connection() {
        conn = new PrinterServiceConnection();
        final Intent intent = new Intent();
        intent.setAction("com.gprinter.aidl.GpPrintService");
        intent.setPackage(this.getPackageName());
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    private class PrinterServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mGpService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mGpService = GpService.Stub.asInterface(service);
        }
    }

    public void searchBlueToothDevice() {

        Log.i(TAG, "searchBlueToothDevice(MainActivity.java:112)--->> " + "searchBlueToothDevice");

        pdSearch = ProgressDialog.show(MainActivity.this, "", "连接中", true, true);
        pdSearch.setCanceledOnTouchOutside(false);
        pdSearch.show();

        mBluetoothList = new ArrayList<>();
        // 检查设备是否支持蓝牙
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Toast.makeText(this, "当前设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            return;
        }
        // 如果蓝牙已经关闭就打开蓝牙
        if (!adapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(intent);
            return;
        }
//        // 获取已配对的蓝牙设备
//        Set<BluetoothDevice> devices = adapter.getBondedDevices();
//        // 遍历
//        int count = 0;
//        for (BluetoothDevice pairedDevice : devices) {
//            Log.i(TAG, "searchBlueToothDevice(MainActivity.java:137)--->> " + pairedDevice.getName());
//            if (pairedDevice.getName() == null) {
//                return;
//            } else if (pairedDevice.getName().startsWith("Printer_29D0")) {
//                count++;
//                deviceAddress = pairedDevice.getAddress();
//                mBluetoothDevice = adapter.getRemoteDevice(deviceAddress);
//                connect(deviceAddress, mBluetoothDevice);
//                break;
//            }
//        }

        if (adapter.isEnabled()) {
            //开始搜索
            adapter.startDiscovery();

            // 设置广播信息过滤
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            // 注册广播接收器，接收并处理搜索结果
            receiver = new MyBroadcastReceiver();
            registerReceiver(receiver, intentFilter);
        }
    }

    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //找到设备,有可能重复搜索同一设备,可在结束后做去重操作
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device == null) {
                    return;
                }
                if (device.getName() == null) {
                    return;
                }

                BluetoothBean bluetoothBean = new BluetoothBean();
                bluetoothBean.mBluetoothName = device.getName();
                bluetoothBean.mBluetoothAddress = device.getAddress();
                bluetoothBean.mBluetoothDevice = adapter.getRemoteDevice(bluetoothBean.mBluetoothAddress);
                mBluetoothList.add(bluetoothBean);

                Log.i(TAG, "onReceive(MainActivity.java:184)--->> " + device.getName());
                Log.i(TAG, "onReceive(MainActivity.java:185)--->> " + mBluetoothList.size());

//                if (device.getName().startsWith("Printer_29D0")) {
//                    //取消搜索
//                    adapter.cancelDiscovery();
//                    deviceAddress = device.getAddress();
//                    mBluetoothDevice = adapter.getRemoteDevice(deviceAddress);
//                    connectState = device.getBondState();
//                    switch (connectState) {
//                        // 未配对
//                        case BluetoothDevice.BOND_NONE:
//                            // 配对
//                            try {
//                                Method createBondMethod = mBluetoothDevice.getClass().getMethod("createBond");
//                                createBondMethod.invoke(mBluetoothDevice);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                            break;
//                        // 已配对
//                        case BluetoothDevice.BOND_BONDED:
//                            if (device.getName().startsWith("Printer_29D0")) {
//                                connect(deviceAddress, mBluetoothDevice);
//                            }
//                            break;
//                    }
//                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.i(TAG, "onReceive(MainActivity.java:213)--->> " + "搜索完成");
                pdSearch.dismiss();
                if (0 == mBluetoothList.size())
                    Toast.makeText(MainActivity.this, "搜索不到蓝牙设备", Toast.LENGTH_SHORT).show();
                else {
                    //去重HashSet add会返回一个boolean值，插入的值已经存在就会返回false 所以true就是不重复的
                    HashSet<BluetoothBean> set = new HashSet<>();
                    mBluetoothList2 = new ArrayList<>();
                    for (BluetoothBean bean : mBluetoothList) {
                        boolean add = set.add(bean);
                        if (add) {
                            mBluetoothList2.add(bean);
                        }
                    }
                    showBluetoothPop(mBluetoothList2);
                }

                unregisterReceiver(receiver);
            }
        }
    }

    private void showBluetoothPop(final List<BluetoothBean> bluetoothList) {
        pdSearch.dismiss();
        View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_bluetooth, null);
        ListView mListView = view.findViewById(R.id.lv_bluetooth);
        MyBluetoothAdapter myBluetoothAdapter = new MyBluetoothAdapter();
        mListView.setAdapter(myBluetoothAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (0 != mBluetoothList2.size()) {
                    closePopupWindow();
                    pdConnect = ProgressDialog.show(MainActivity.this, "", "开始连接", true, true);
                    pdConnect.setCanceledOnTouchOutside(false);
                    pdConnect.show();
                    connect(bluetoothList.get(position).mBluetoothAddress, bluetoothList.get(position).mBluetoothDevice);
                }
            }
        });
        pw = new PopupWindow(view, (int) (getScreenWidth() * 0.8), -2);
        closePopupWindow();
        pw.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pw.setOutsideTouchable(true);
        pw.setFocusable(true);
        WindowManager.LayoutParams lp = this.getWindow().getAttributes();
        lp.alpha = 0.7f;
        getWindow().setAttributes(lp);
        pw.setOnDismissListener(new PopupWindow.OnDismissListener() {

            @Override
            public void onDismiss() {
                WindowManager.LayoutParams lp = MainActivity.this.getWindow().getAttributes();
                lp.alpha = 1f;
                getWindow().setAttributes(lp);
            }
        });
        pw.setAnimationStyle(R.style.PopAnim);
        //显示
        pw.showAtLocation(view, Gravity.CENTER, 0, 0);
    }

    private void closePopupWindow() {
        if (pw != null && pw.isShowing()) {
            pw.dismiss();
            pw = null;
        }
    }

    public int getScreenWidth() {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    class MyBluetoothAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mBluetoothList2.size();
        }

        @Override
        public Object getItem(int position) {
            return mBluetoothList2.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_bluetooth, parent, false);
                holder = new ViewHolder();
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.item_text = convertView.findViewById(R.id.item_text);
            holder.item_text_address = convertView.findViewById(R.id.item_text_address);
            holder.item_text.setText(mBluetoothList2.get(position).mBluetoothName);
            holder.item_text_address.setText(mBluetoothList2.get(position).mBluetoothAddress);
            return convertView;
        }

        class ViewHolder {
            TextView item_text;
            TextView item_text_address;
        }
    }

    /**
     * 启动连接蓝牙的线程方法
     */
    public synchronized void connect(String macAddress, BluetoothDevice device) {
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        if (socket != null) {
            try {
                mGpService.closePort(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            socket = null;
        }
        mThread = new ConnectThread(macAddress, device);
        mThread.start();
    }

    private class ConnectThread extends Thread {
        private BluetoothDevice mmDevice;
        private OutputStream mmOutStream;

        public ConnectThread(String mac, BluetoothDevice device) {
            mmDevice = device;
            String SPP_UUID = "00001101-0000-1000-8000-00805f9b34fb";
            try {
                if (socket == null) {
                    socket = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void run() {
            adapter.cancelDiscovery();
            try {
                Log.i(TAG, "run(MainActivity.java:367)--->> " + "连接socket");
                if (socket.isConnected()) {
                    Log.i(TAG, "run(MainActivity.java:369)--->> " + "已经连接过了");
                } else {
                    if (socket != null) {
                        try {
                            if (mGpService != null) {
                                int state = mGpService.getPrinterConnectStatus(0);
                                switch (state) {
                                    case GpDevice.STATE_CONNECTED:
                                        break;
                                    case GpDevice.STATE_LISTEN:
                                        Log.i(TAG, "run(MainActivity.java:379)--->> " + "state:STATE_LISTEN");
                                        break;
                                    case GpDevice.STATE_CONNECTING:
                                        Log.i(TAG, "run(MainActivity.java:382)--->> " + "state:STATE_CONNECTING");
                                        break;
                                    case GpDevice.STATE_NONE:
                                        Log.i(TAG, "run(MainActivity.java:385)--->> " + "state:STATE_NONE");
                                        registerBroadcast();
                                        mGpService.openPort(0, 4, mmDevice.getAddress(), 0);
                                        break;
                                    default:
                                        Log.i(TAG, "run(MainActivity.java:390)--->> " + "state:default");
                                        break;
                                }
                            } else {
                                Log.i(TAG, "run(MainActivity.java:394)--->> " + "mGpService IS NULL");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (Exception connectException) {
                Log.i(TAG, "run(MainActivity.java:402)--->> " + "连接失败");
                try {
                    if (socket != null) {
                        mGpService.closePort(0);
                        socket = null;
                    }
                } catch (Exception closeException) {

                }
            }
        }
    }

    public static final String ACTION_CONNECT_STATUS = "action.connect.status";

    private void registerBroadcast() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CONNECT_STATUS);
        registerReceiver(printerStatusBroadcastReceiver, filter);
    }

    private BroadcastReceiver printerStatusBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_CONNECT_STATUS.equals(intent.getAction())) {
                int type = intent.getIntExtra(GpPrintService.CONNECT_STATUS, 0);
                int id = intent.getIntExtra(GpPrintService.PRINTER_ID, 0);
                if (type == GpDevice.STATE_CONNECTING) {
                    Log.i(TAG, "onReceive(MainActivity.java:430)--->> " + "STATE_CONNECTING");
                } else if (type == GpDevice.STATE_NONE) {
                    Log.i(TAG, "onReceive(MainActivity.java:432)--->> " + "STATE_NONE");
                    showErrorDialog();
                } else if (type == GpDevice.STATE_VALID_PRINTER) {
                    //打印机-有效的打印机
                    Log.i(TAG, "onReceive(MainActivity.java:436)--->> " + "STATE_VALID_PRINTER");
                } else if (type == GpDevice.STATE_INVALID_PRINTER) {
                    Log.i(TAG, "onReceive(MainActivity.java:438)--->> " + "STATE_INVALID_PRINTER");
                } else if (type == GpDevice.STATE_CONNECTED) {
                    //表示已连接可以打印
                    Log.i(TAG, "onReceive(MainActivity.java:441)--->> " + "STATE_CONNECTED");
                    unregisterReceiver(printerStatusBroadcastReceiver);
                    showSuccessDialog();
                } else if (type == GpDevice.STATE_LISTEN) {
                    Log.i(TAG, "onReceive(MainActivity.java:445)--->> " + "STATE_LISTEN");
                }
            }
        }
    };

    private void showSuccessDialog() {
        pdSearch.dismiss();
        DialogInterface.OnClickListener mOnClickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case Dialog.BUTTON_POSITIVE:
                        printOrder();
                        break;
                    case Dialog.BUTTON_NEGATIVE:
                        dialog.dismiss();
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("提示");
        builder.setMessage("连接成功，是否开始打印?");
        builder.setPositiveButton("确定", mOnClickListener);
        builder.setNegativeButton("取消", mOnClickListener);
        builder.create().show();
    }

    private void showErrorDialog() {
        pdSearch.dismiss();
        DialogInterface.OnClickListener mOnClickListener = new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case Dialog.BUTTON_POSITIVE:
                        searchBlueToothDevice();
                        break;
                    case Dialog.BUTTON_NEGATIVE:
                        dialog.dismiss();
                        break;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("提示");
        builder.setMessage("连接失败，是否重试?");
        builder.setPositiveButton("确定", mOnClickListener);
        builder.setNegativeButton("取消", mOnClickListener);
        builder.create().show();
    }

    private void printOrder() {
        Log.i(TAG, "printOrder(MainActivity.java:500)--->> " + "printOrder");
        LabelCommand tsc = new LabelCommand();
        tsc.addSize(40, 30); // 设置标签尺寸，按照实际尺寸设置
        tsc.addGap(1); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0
        tsc.addDirection(LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向
        tsc.addReference(0, 0);// 设置原点坐标
        tsc.addTear(EscCommand.ENABLE.ON); // 撕纸模式开启
        Log.i(TAG, "sendLabel(MainActivity.java:507)--->> " + EscCommand.ENABLE.ON.getValue());

        tsc.addCls();// 清除打印缓冲区
        // 绘制简体中文
        tsc.addText(30, 20, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "花满楼");

        tsc.addText(30, 70, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "仓库：1号仓");
        //180
        tsc.addText(200, 20, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "箱号：2");
        tsc.addText(30, 110, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "线路：A19");
        tsc.addText(30, 150, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "数量：5");
        tsc.addText(30, 190, LabelCommand.FONTTYPE.SIMPLIFIED_CHINESE, LabelCommand.ROTATION.ROTATION_0, LabelCommand.FONTMUL.MUL_1, LabelCommand.FONTMUL.MUL_1,
                "日期：2017年11月21日");

        // 绘制图片
//        Bitmap b = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
//        tsc.addBitmap(20, 50, LabelCommand.BITMAP_MODE.OVERWRITE, b.getWidth(), b);

        //二维码
        tsc.addQRCode(200, 70, LabelCommand.EEC.LEVEL_L, 4, LabelCommand.ROTATION.ROTATION_0, " www.gprinter.com.cn");

        // 绘制一维条码
//        tsc.add1DBarcode(20, 250, LabelCommand.BARCODETYPE.CODE128, 100, LabelCommand.READABEL.EANBEL, LabelCommand.ROTATION.ROTATION_0, "Gprinter");

        tsc.addPrint(1, 1); // 打印标签
        tsc.addSound(2, 100); // 打印标签后 蜂鸣器响
//        tsc.addCashdrwer(LabelCommand.FOOT.F5, 255, 255);

        Vector<Byte> datas = tsc.getCommand(); // 发送数据
        byte[] bytes = GpUtils.ByteTo_byte(datas);
        String str = Base64.encodeToString(bytes, Base64.DEFAULT);
        int rel;
        try {
            rel = mGpService.sendLabelCommand(0, str);
            GpCom.ERROR_CODE r = GpCom.ERROR_CODE.values()[rel];
            if (r != GpCom.ERROR_CODE.SUCCESS) {
                Toast.makeText(getApplicationContext(), GpCom.getErrorText(r), Toast.LENGTH_SHORT).show();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy(MainActivity.java:557)--->> " + "onDestroy");
        super.onDestroy();
        if (conn != null) {
            unbindService(conn);
        }
    }

}
