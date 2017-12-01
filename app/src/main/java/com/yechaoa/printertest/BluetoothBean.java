package com.yechaoa.printertest;

import android.bluetooth.BluetoothDevice;

/**
 * Created by yechao on 2017/11/27.
 * Describe :
 */

public class BluetoothBean {

    public BluetoothBean(){}

    public int mBluetoothId;
    public String mBluetoothName;//蓝牙名字
    public String mBluetoothAddress;//蓝牙地址
    public BluetoothDevice mBluetoothDevice;//蓝牙设备

    @Override
    public boolean equals(Object obj) {
        BluetoothBean b=(BluetoothBean)obj;
        return mBluetoothAddress.equals(b.mBluetoothAddress) ;
    }

    @Override
    public int hashCode() {
        int result = mBluetoothAddress.hashCode();
        result = 31 * result + mBluetoothAddress.hashCode();
        return result;
    }

}
