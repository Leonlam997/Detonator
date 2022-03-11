package com.leon.detonator.BluetoothTool;

import java.util.List;
import java.util.Map;

public interface SearchBluetoothListener {
    void startSearch();

    void foundDevice(BluetoothBean bluetooth, boolean newDevice);

    void finishSearch(Map<String, List<BluetoothBean>> blueToothMap);
}
