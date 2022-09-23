package com.leon.detonator.bluetooth;

import android.bluetooth.BluetoothDevice;

public interface PairBluetoothListener {

    void whilePair(BluetoothDevice device);

    void pairingSuccess(BluetoothDevice device);

    void cancelPair(BluetoothDevice device);
}
