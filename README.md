# PrinterDemo
Android蓝牙连打印机

博客地址：http://blog.csdn.net/yechaoa/article/details/78666446

![](https://github.com/yechaoa/PrinterDemo/raw/master/pic/p1.jpg)
![](https://github.com/yechaoa/PrinterDemo/raw/master/pic/p2.jpg)
![](https://github.com/yechaoa/PrinterDemo/raw/master/pic/p3.jpg)


以Gprinter佳博打印机为例，从蓝牙到打印。很简单的 >_< <br>
demo环境：as3.0。

### 1、去官网下载安卓版SDK，解压并开始配置
app目录下新建libs文件夹，拷入jar包并add as library，具体如图<br>
![](http://img.blog.csdn.net/20171129161947120?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQveWVjaGFvYQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/Center)

然后main文件夹目录下新建aidl文件夹 <br>
![](http://img.blog.csdn.net/20171129162047492?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQveWVjaGFvYQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/Center)

main文件夹目录下新建jniLibs文件夹 <br>
![](https://img-blog.csdn.net/20171129162119817?watermark/2/text/aHR0cDovL2Jsb2cuY3Nkbi5uZXQveWVjaGFvYQ==/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70/gravity/Center)

### 2、AndroidManifest文件中添加权限和service
```
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />  
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />  
    <uses-permission android:name="android.permission.INTERNET" />  
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />  
    <uses-permission android:name="android.permission.BLUETOOTH" />  
    <uses-permission android:name="android.hardware.usb.accessory" />  
    <uses-permission android:name="android.permission.WAKE_LOCK" />  
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />  
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />  
    <uses-permission android:name="android.permission.MOUNT_UNMOUNT_FILESYSTEMS" />  
    <uses-permission android:name="android.permission.GET_TASKS" />  
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />  
    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />  
    <uses-permission android:name="android.permission.WRITE_SETTINGS" />  
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />  
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />  
    <uses-feature android:name="android.hardware.usb.host" />  
```

```
    <service  
        android:name="com.gprinter.service.GpPrintService"  
        android:enabled="true"  
        android:exported="true"  
        android:label="GpPrintService" >  
        <intent-filter>  
            <action android:name="com.gprinter.aidl.GpPrintService" />  
        </intent-filter>  
    </service>  
    <service android:name="com.gprinter.service.AllService" >  
    </service>  
```

<br>
*  注意：ACCESS_COARSE_LOCATION权限在6.0+需要动态获取 
<br>

### 3、在页面的onCreate中初始化service并bind
```
    startService();  
    connection();  
```
```
private void startService() {  
       Intent i = new Intent(this, GpPrintService.class);  
       startService(i);  
   }  
  
   private void connection() {  
       Log.i(TAG, "connection(MainActivity.java:90)--->> " + "connection");  
       conn = new PrinterServiceConnection();  
       final Intent intent = new Intent();  
       intent.setAction("com.gprinter.aidl.GpPrintService");  
       intent.setPackage(this.getPackageName());  
       bindService(intent, conn, Context.BIND_AUTO_CREATE);  
   }  
  
   private class PrinterServiceConnection implements ServiceConnection {  
       @Override  
       public void onServiceDisconnected(ComponentName name) {  
           Log.i(TAG, "onServiceDisconnected(MainActivity.java:101)--->> " + "onServiceDisconnected");  
           mGpService = null;  
       }  
  
       @Override  
       public void onServiceConnected(ComponentName name, IBinder service) {  
           mGpService = GpService.Stub.asInterface(service);  
       }  
   }  
```

### 4、点击按钮触发打开、搜索、连接等一系列操作，可拆分
```
    findViewById(R.id.button_connect).setOnClickListener(new View.OnClickListener() {  
        @Override  
        public void onClick(View view) {  
            Log.i(TAG, "onClick(MainActivity.java:78)--->> " + "onClick");  
            searchBlueToothDevice();  
        }  
    });  
```

```
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
```

#### 接收的广播
```
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
  
                Log.i(TAG, "onReceive(MainActivity.java:191)--->> " + device.getName());  
                Log.i(TAG, "onReceive(MainActivity.java:192)--->> " + mBluetoothList.size());  
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {  
                Log.i(TAG, "onReceive(MainActivity.java:220)--->> " + "搜索完成");  
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
```

#### 弹出搜索到的蓝牙列表，点击开始连接
```
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
```

#### 连接的方法
```
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
                Log.i(TAG, "run(MainActivity.java:362)--->> " + "连接socket");  
                if (socket.isConnected()) {  
                    Log.i(TAG, "run(MainActivity.java:364)--->> " + "已经连接过了");  
                } else {  
                    if (socket != null) {  
                        try {  
                            if (mGpService != null) {  
                                int state = mGpService.getPrinterConnectStatus(0);  
                                switch (state) {  
                                    case GpDevice.STATE_CONNECTED:  
                                        break;  
                                    case GpDevice.STATE_LISTEN:  
                                        Log.i(TAG, "run(MainActivity.java:374)--->> " + "state:STATE_LISTEN");  
                                        break;  
                                    case GpDevice.STATE_CONNECTING:  
                                        Log.i(TAG, "run(MainActivity.java:377)--->> " + "state:STATE_CONNECTING");  
                                        break;  
                                    case GpDevice.STATE_NONE:  
                                        Log.i(TAG, "run(MainActivity.java:380)--->> " + "state:STATE_NONE");  
                                        registerBroadcast();  
                                        mGpService.openPort(0, 4, mmDevice.getAddress(), 0);  
                                        break;  
                                    default:  
                                        Log.i(TAG, "run(MainActivity.java:385)--->> " + "state:default");  
                                        break;  
                                }  
                            } else {  
                                Log.i(TAG, "run(MainActivity.java:389)--->> " + "mGpService IS NULL");  
                            }  
                        } catch (Exception e) {  
                            e.printStackTrace();  
                        }  
                    }  
                }  
            } catch (Exception connectException) {  
                Log.i(TAG, "run(MainActivity.java:397)--->> " + "连接失败");  
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
```

#### 连接状态的广播
```
private BroadcastReceiver printerStatusBroadcastReceiver = new BroadcastReceiver() {  
       @Override  
       public void onReceive(Context context, Intent intent) {  
           if (ACTION_CONNECT_STATUS.equals(intent.getAction())) {  
               int type = intent.getIntExtra(GpPrintService.CONNECT_STATUS, 0);  
               int id = intent.getIntExtra(GpPrintService.PRINTER_ID, 0);  
               if (type == GpDevice.STATE_CONNECTING) {  
                   Log.i(TAG, "onReceive(MainActivity.java:425)--->> " + "STATE_CONNECTING");  
               } else if (type == GpDevice.STATE_NONE) {  
                   Log.i(TAG, "onReceive(MainActivity.java:427)--->> " + "STATE_NONE");  
                   showErrorDialog();  
               } else if (type == GpDevice.STATE_VALID_PRINTER) {  
                   //打印机-有效的打印机  
                   Log.i(TAG, "onReceive(MainActivity.java:431)--->> " + "STATE_VALID_PRINTER");  
               } else if (type == GpDevice.STATE_INVALID_PRINTER) {  
                   Log.i(TAG, "onReceive(MainActivity.java:433)--->> " + "STATE_INVALID_PRINTER");  
               } else if (type == GpDevice.STATE_CONNECTED) {  
                   //表示已连接可以打印  
                   Log.i(TAG, "onReceive(MainActivity.java:436)--->> " + "STATE_CONNECTED");  
                   unregisterReceiver(printerStatusBroadcastReceiver);  
                   showSuccessDialog();  
               } else if (type == GpDevice.STATE_LISTEN) {  
                   Log.i(TAG, "onReceive(MainActivity.java:440)--->> " + "STATE_LISTEN");  
               }  
           }  
       }  
   };  
```

#### 连接成功的dialog
```
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
```

#### 开始打印
```
private void printOrder() {  
        Log.i(TAG, "printOrder(MainActivity.java:495)--->> " + "printOrder");  
        LabelCommand tsc = new LabelCommand();  
        tsc.addSize(40, 30); // 设置标签尺寸，按照实际尺寸设置  
        tsc.addGap(1); // 设置标签间隙，按照实际尺寸设置，如果为无间隙纸则设置为0  
        tsc.addDirection(LabelCommand.DIRECTION.FORWARD, LabelCommand.MIRROR.NORMAL);// 设置打印方向  
        tsc.addReference(0, 0);// 设置原点坐标  
        tsc.addTear(EscCommand.ENABLE.ON); // 撕纸模式开启  
        Log.i(TAG, "sendLabel(MainActivity.java:502)--->> " + EscCommand.ENABLE.ON.getValue());  
  
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
//        tsc.add1DBarcode(20, 250, LabelCommand.BARCODETYPE.CODE128, 100, LabelCommand.READABEL.EANBEL,        LabelCommand.ROTATION.ROTATION_0, "Gprinter");  
  
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
```

以上基本是核心代码了，注释都有，按照上面这个顺序来走的话思路还是很清晰的<br>
（就是权限没有动态获取，可参考[Android6.0运行时权限](http://blog.csdn.net/yechaoa/article/details/61920584)。）
