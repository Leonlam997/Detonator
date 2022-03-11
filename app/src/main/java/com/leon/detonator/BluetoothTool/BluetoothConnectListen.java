package com.leon.detonator.BluetoothTool;

public interface BluetoothConnectListen {
    void SuccessConnect();

    void Connecting();

    void CancelConnect();

    void onDoThing();

    void ReceiveData(String data);
}
