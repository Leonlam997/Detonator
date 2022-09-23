package com.leon.detonator.bluetooth;

import android.bluetooth.BluetoothDevice;

public interface BluetoothLinkListener {

    void disconnected(BluetoothDevice device);

    void connected(BluetoothDevice device);
}
