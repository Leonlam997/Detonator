package com.leon.detonator.BluetoothTool;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.leon.detonator.Base.BaseApplication;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    public final static String ACTION_GATT_CONNECTED = BluetoothLeService.class.getName() + ".ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_CONNECTING = BluetoothLeService.class.getName() + ".ACTION_GATT_CONNECTING";
    public final static String ACTION_GATT_DISCONNECTED = BluetoothLeService.class.getName() + ".ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = BluetoothLeService.class.getName() + ".ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = BluetoothLeService.class.getName() + ".ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA_RAW = BluetoothLeService.class.getName() + ".EXTRA_DATA_RAW";
    public final static String EXTRA_UUID_CHAR = BluetoothLeService.class.getName() + ".EXTRA_UUID_CHAR";

    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private final static UUID UUID_NOTIFY = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"),
            UUID_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private final IBinder mBinder = new LocalBinder();
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            Log.d(TAG, "onConnectionStateChange: status=" + status + ", newState=" + newState);
            ((BaseApplication) getApplication()).myToast(getApplication(), "onConnectionStateChange: status=" + status + ", newState=" + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                setConnectionState(State.CONNECTED, true);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                //mBluetoothGatt.discoverServices();
                //Log.i(TAG, "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());
                ((BaseApplication) getApplication()).myToast(getApplication(), "Attempting to start service discovery:" + mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Make sure we tidy up. On certain devices reusing a Gatt after a disconnection
                // can cause problems.
                disconnect();
                setConnectionState(State.DISCONNECTED, true);
                //((BaseApplication) getApplication()).myToast(getApplication(), "Disconnected from GATT server.");
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                findService(gatt.getServices());
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
    };

    public void writeValue(@NotNull String strValue) {
        mNotifyCharacteristic.setValue(strValue.getBytes());
        mBluetoothGatt.writeCharacteristic(mNotifyCharacteristic);
    }

    public void findService(@NotNull List<BluetoothGattService> gattServices) {
        Log.i(TAG, "Count is:" + gattServices.size());
        for (BluetoothGattService gattService : gattServices) {
            Log.i(TAG, gattService.getUuid().toString());
            Log.i(TAG, UUID_SERVICE.toString());
            if (gattService.getUuid().toString().equalsIgnoreCase(UUID_SERVICE.toString())) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                Log.i(TAG, "Count is:" + gattCharacteristics.size());
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    if (gattCharacteristic.getUuid().toString().equalsIgnoreCase(UUID_NOTIFY.toString())) {
                        Log.i(TAG, gattCharacteristic.getUuid().toString());
                        Log.i(TAG, UUID_NOTIFY.toString());
                        mNotifyCharacteristic = gattCharacteristic;
                        setCharacteristicNotification(gattCharacteristic, true);
                        broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                        return;
                    }
                }
            }
        }
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_UUID_CHAR, characteristic.getUuid().toString());

        // Always try to add the RAW value
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            intent.putExtra(EXTRA_DATA_RAW, data);
        }

        sendBroadcast(intent);
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        final boolean retVal;
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            retVal = false;
            // Previously connected device.  Try to reconnect.
        } else if (address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                Log.d(TAG, "Connection attempt OK.");
                setConnectionState(State.CONNECTING, true);
                retVal = true;
            } else {
                Log.w(TAG, "Connection attempt failed.");
                setConnectionState(State.DISCONNECTED, true);
                retVal = false;
            }
        } else {
            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            if (device == null) {
                Log.w(TAG, "Device not found.  Unable to connect.");
                retVal = false;
            } else {
                // We want to directly connect to the device, so we are setting the autoConnect
                // parameter to false.
                Log.d(TAG, "Trying to create a new connection.");
                mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
                mBluetoothDeviceAddress = address;
                setConnectionState(State.CONNECTING, true);
                retVal = true;
            }
        }

        return retVal;
    }

    private synchronized void setConnectionState(final State newState, final boolean broadCast) {
        Log.i(TAG, "Setting internal state to " + newState);

        final String broadcastAction;
        switch (newState) {
            case CONNECTED:
                broadcastAction = ACTION_GATT_CONNECTED;
                break;
            case CONNECTING:
                broadcastAction = ACTION_GATT_CONNECTING;
                break;
            case DISCONNECTED:
                broadcastAction = ACTION_GATT_DISCONNECTED;
                break;
            default:
                throw new IllegalArgumentException("Unknown state: " + newState);
        }

        if (broadCast) {
            Log.i(TAG, "Broadcasting " + broadcastAction);
            broadcastUpdate(broadcastAction);
        }

    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();

        // Reusing a Gatt after disconnecting can cause problems
        mBluetoothGatt = null;
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(final Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(final BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(final BluetoothGattCharacteristic characteristic, final boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    private enum State {
        CONNECTED,
        CONNECTING,
        DISCONNECTED
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

}