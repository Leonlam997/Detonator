package com.leon.detonator.BluetoothTool;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.leon.detonator.Base.BaseApplication;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class BluetoothUtil implements BluetoothServiceInterface {

    public final static String CONNECTED_BLUETOOTH = "ConnectedBluetooth";
    public final static String NEW_BLUETOOTH = "NewBluetooth";
    private final List<BluetoothBean> connectedBluetooth;
    private final List<BluetoothBean> newBluetooth;
    private final BluetoothAdapter BTAdapter;
    private Context context;
    private SearchBluetoothListener mSearchListener;
    private BluetoothLinkListener listener_Link;
    private PairBluetoothListener mPairListener;
    private final BroadcastReceiver mSearchReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            String action = arg1.getAction();
            switch (action) {
                case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                    connectedBluetooth.clear();
                    newBluetooth.clear();
                    mSearchListener.startSearch();
                    Set<BluetoothDevice> devices = BTAdapter.getBondedDevices();
                    if (devices.size() > 0) {
                        for (BluetoothDevice device : devices) {
                            BluetoothBean blueTooth = new BluetoothBean();
                            blueTooth.setName(device.getName());
                            blueTooth.setAddress(device.getAddress());
                            blueTooth.setType(device.getType());
                            blueTooth.setDeviceType(device.getBluetoothClass().getDeviceClass());
                            blueTooth.setUuid(device.getUuids());
                            try {
                                Method isConnectedMethod = BluetoothDevice.class.getDeclaredMethod("isConnected", (Class[]) null);
                                isConnectedMethod.setAccessible(true);
                                blueTooth.setConnected((boolean) isConnectedMethod.invoke(device, (Object[]) null));
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                            mSearchListener.foundDevice(blueTooth, false);
                        }
                    }
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device = arg1.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    BluetoothBean blueTooth = new BluetoothBean();
                    assert device != null;
                    blueTooth.setName(device.getName());
                    blueTooth.setAddress(device.getAddress());
                    blueTooth.setType(device.getType());
                    blueTooth.setDeviceType(device.getBluetoothClass().getDeviceClass());
                    blueTooth.setUuid(device.getUuids());
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        for (BluetoothBean blueToothPul : connectedBluetooth) {
                            if (blueToothPul.getAddress().equals(blueTooth.getAddress()))
                                return;
                        }
                        connectedBluetooth.add(blueTooth);
                        mSearchListener.foundDevice(blueTooth, false);
                    } else {
                        for (BluetoothBean blueToothPul : newBluetooth) {
                            if (blueToothPul.getAddress().equals(blueTooth.getAddress())) {
                                return;
                            }
                        }
                        newBluetooth.add(blueTooth);
                        mSearchListener.foundDevice(blueTooth, true);
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    Map<String, List<BluetoothBean>> blueToothMap = new HashMap<>();
                    blueToothMap.put(CONNECTED_BLUETOOTH, connectedBluetooth);
                    blueToothMap.put(NEW_BLUETOOTH, newBluetooth);
                    mSearchListener.finishSearch(blueToothMap);
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    device = arg1.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    switch (Objects.requireNonNull(device).getBondState()) {
                        case BluetoothDevice.BOND_BONDING:
                            mPairListener.whilePair(device);
                            break;
                        case BluetoothDevice.BOND_BONDED:
                            mPairListener.pairingSuccess(device);
                            break;
                        case BluetoothDevice.BOND_NONE:
                            mPairListener.cancelPair(device);
                        default:
                            break;
                    }
                    break;
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    BluetoothDevice device1 = arg1.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    listener_Link.connected(device1);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    BluetoothDevice device2 = arg1.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    listener_Link.disconnected(device2);
                    break;

                default:
                    break;
            }
        }
    };
    private boolean mSearchReceiver_isRegister = false;

    public BluetoothUtil() {
        this.BTAdapter = BluetoothAdapter.getDefaultAdapter();
        connectedBluetooth = new ArrayList<>();
        newBluetooth = new ArrayList<>();
    }

    public void enableBluetooth() throws Exception {
        if (BTAdapter == null) {
            throw new Exception("设备上没有发现蓝牙设备！");
        }
        if (!BTAdapter.isEnabled()) {
            BTAdapter.enable();
        }
    }

    public void setListener_Link(BluetoothLinkListener listener_Link) {
        this.listener_Link = listener_Link;
    }

    @Override
    public void makePair(Context context, String address, PairBluetoothListener mPairListener) throws Exception {
        this.mPairListener = mPairListener;
        this.context = context;
        IntentFilter iFilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(mSearchReceiver, iFilter);
        mSearchReceiver_isRegister = true;

        enableBluetooth();
        BluetoothDevice device = BTAdapter.getRemoteDevice(address);

        device.createBond();
    }

    public void unPair(String address) throws Exception {
        BluetoothDevice device = BTAdapter.getRemoteDevice(address);
        Method m = device.getClass().getMethod("removeBond", (Class[]) null);
        m.setAccessible(true);
        m.invoke(device, (Object[]) null);
    }

    @Override
    public void searchBluetooth(Context context, SearchBluetoothListener mSearchListener) throws Exception {
        this.mSearchListener = mSearchListener;
        this.context = context;
        enableBluetooth();
        if (BTAdapter.isDiscovering()) {
            BTAdapter.cancelDiscovery();
        }
        BTAdapter.startDiscovery();

        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        iFilter.addAction(BluetoothDevice.ACTION_FOUND);
        iFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(mSearchReceiver, iFilter);
        mSearchReceiver_isRegister = true;
    }

    @Override
    public BluetoothSocket getBluetoothSocket(String address) throws IOException {
        //BluetoothDevice device = BTAdapter.getRemoteDevice(address);
        //BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID.fromString(BluetoothBean.MY_UUID));
        return BTAdapter.getRemoteDevice(address).createRfcommSocketToServiceRecord(UUID.fromString(BluetoothBean.MY_UUID));
    }

    @Override
    public void closeBluetoothService() {
        if (mSearchReceiver_isRegister) {
            context.unregisterReceiver(mSearchReceiver);
            mSearchReceiver_isRegister = false;
        }
    }
}
