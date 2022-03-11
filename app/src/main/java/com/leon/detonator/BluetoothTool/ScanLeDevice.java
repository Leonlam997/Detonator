package com.leon.detonator.BluetoothTool;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class ScanLeDevice {
    private final List<BluetoothBean> connectedBluetooth;
    private final List<BluetoothBean> newBluetooth;
    private final Context mContext;
    private final SearchBluetoothListener mSearchListener;

    public ScanLeDevice(Context context, SearchBluetoothListener listener) {
        mContext = context;
        mSearchListener = listener;
        connectedBluetooth = new ArrayList<>();
        newBluetooth = new ArrayList<>();
    }

    public void startScan() {
        BluetoothLeScanner scanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
//        ScanSettings scanSettings = new  ScanSettings.Builder();
//        scanner.startScan(new ArrayList<ScanFilter>(),)
        scanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        });
    }
}
