package com.leon.detonator.BluetoothTool;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

public class GattUpdateReceiver extends BroadcastReceiver {
    public static String TAG = "GattUpdateReceiver";
    String data;
    private BluetoothConnectListen mBluetoothConnectListen;//实现接口

    private final Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 0: // 连接成功
                    if (mBluetoothConnectListen != null) {
                        mBluetoothConnectListen.SuccessConnect();
                    }
                    break;
                case 1:
                    if (mBluetoothConnectListen != null) {
                        mBluetoothConnectListen.Connecting();
                    }
                    break;
                case 2: // 链接中断
                    if (mBluetoothConnectListen != null) {
                        mBluetoothConnectListen.CancelConnect();
                    }
                    break;
                case 3: // 可以进行数据通信
                    if (mBluetoothConnectListen != null) {
                        mBluetoothConnectListen.onDoThing();
                    }
                    break;
                case 4: // 接受到数据
                    if (mBluetoothConnectListen != null) {
                        mBluetoothConnectListen.ReceiveData(data);
                    }
                    break;
            }

            return false;
        }
    });

    public void setBluetoothConnectInterface(BluetoothConnectListen m) {
        mBluetoothConnectListen = m;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub

        final String action = intent.getAction();
        Log.d(TAG, "Action==" + action);
        if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) { //连接成功
            mHandler.sendEmptyMessage(0);
        } else if (BluetoothLeService.ACTION_GATT_CONNECTING.equals(action)) {
            mHandler.sendEmptyMessage(1);
        } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) { //连接失败
            mHandler.sendEmptyMessage(2);
        } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {//可以通信
            mHandler.sendEmptyMessage(3);
        } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) { //接受到数据
            data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA_RAW);
            mHandler.sendEmptyMessage(4);
        }
    }
}