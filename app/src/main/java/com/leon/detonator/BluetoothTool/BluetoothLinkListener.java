package com.leon.detonator.BluetoothTool;

import android.bluetooth.BluetoothDevice;

public interface BluetoothLinkListener {

    void disconnected(BluetoothDevice device);

    void connected(BluetoothDevice device);
}
