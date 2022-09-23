package com.leon.detonator.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.content.Context;

import java.io.IOException;

public interface BluetoothServiceInterface {

    void enableBluetooth() throws Exception;

    void searchBluetooth(Context context, SearchBluetoothListener mSearchListener) throws Exception;

    BluetoothSocket getBluetoothSocket(String address) throws IOException;

    void makePair(Context context, String address, PairBluetoothListener mPariListener) throws Exception;

    void closeBluetoothService();
}
