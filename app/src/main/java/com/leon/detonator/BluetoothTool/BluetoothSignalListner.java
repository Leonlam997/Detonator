package com.leon.detonator.BluetoothTool;

public interface BluetoothSignalListner {
    void startSearchBluetooth();

    void whileGetSignalIntensity(short signalIntensity);

    void finishSearch();
}
