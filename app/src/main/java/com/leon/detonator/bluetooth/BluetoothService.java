package com.leon.detonator.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.leon.detonator.R;
import com.leon.detonator.activity.BluetoothActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * 蓝牙服务类，包括蓝牙连接监听线程、连接线程、已连接线程
 *
 * @author Administrator
 */
public class BluetoothService {
    //蓝牙状态常量
    public static final int IDLE = 0;       // 闲置
    public static final int LISTENING = 1;  // 监听
    public static final int CONNECTING = 2; // 正在连接
    public static final int CONNECTED = 3;  // 已连接
    private static final String TAG = "Service";
    private static final boolean DEBUG = false;
    // 蓝牙端口名
    private static final String BT_NAME = "ZhongBao";
    // 获取设备UUID
    private static final UUID MY_UUID = UUID.fromString(BluetoothBean.getMyUuid());
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int BtState;
    private final Context mContext;

    /**
     * @param handler 在线程与UI间通讯
     */
    public BluetoothService(Handler handler, Context context) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        BtState = IDLE;
        mHandler = handler;
        mContext = context;
    }

    /**
     * 获取当前蓝牙状态
     *
     * @return 当前蓝牙状态
     */
    public synchronized int getState() {
        return BtState;
    }

    /**
     * 设置当前蓝牙状态
     *
     * @param state 当前蓝牙状态
     */
    private synchronized void setState(int state) {
        BtState = state;
    }

    /**
     * 启动本地蓝牙接收监听
     */
    public synchronized void acceptWait() {
        if (DEBUG) Log.e(TAG, "进入acceptWait");
        // 开启外主蓝牙接收监听线程
        if (mAcceptThread == null && mConnectedThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(LISTENING);
    }

    /**
     * 开启连接线程方法
     *
     * @param device 欲连接的设备
     */
    public synchronized void connect(BluetoothDevice device) {
        if (DEBUG) Log.e(TAG, "正在连接" + device);

        //关闭所有可能的蓝牙服务线程以便开启连接线程
        cancelAllBtThread();
        // 开启连接线程
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(CONNECTING);
    }

    /**
     * 开启已连接线程的方法
     *
     * @param socket 已建立连接的蓝牙端口
     * @param device 已连接的蓝牙设备
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (DEBUG) Log.e(TAG, "connected");

        //关闭所有可能的蓝牙服务线程以便开启已连接线程
        cancelAllBtThread();

        // 开启已连接线程
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        //发送已连接设备名回UI
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        sendString2UI(BluetoothActivity.STATUS_CONNECTED, device.getName());
        setState(CONNECTED);
    }

    /**
     * 关闭所有蓝牙服务线程
     */
    public synchronized void cancelAllBtThread() {
        if (DEBUG) Log.e(TAG, "cancelAllBtThread方法");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(IDLE);
    }

    /**
     * 写输出数据
     *
     * @param out 输出字节流
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        //                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              object
        ConnectedThread r;
        // 同步
        synchronized (this) {
            if (BtState != CONNECTED) return;
            r = mConnectedThread;
        }
        r.write(out);
    }

    /**
     * 连接失败处理方法
     */
    private void connectionFailed() {
        setState(LISTENING);
        mConnectedThread = null;
        BluetoothService.this.acceptWait();
        //向UI发送连接失败通知
        sendString2UI(BluetoothActivity.STATUS_ERROR, "连接失败！");
    }

    /**
     * 发送字符串会UI
     *
     * @param str 字符串
     */
    private void sendString2UI(int what, String str) {
        Message msg = mHandler.obtainMessage(what);
        msg.obj = str;
        mHandler.sendMessage(msg);
    }

    /**
     * 连接断开处理方法
     */
    private void connectionBreak() {
        setState(LISTENING);
        mConnectedThread = null;
        BluetoothService.this.acceptWait();
        // 向UI发送连接断开通知
        //sendString2UI(BluetoothActivity.STATUS_ERROR, "连接断开！");
    }

    /**
     * 监听外部主蓝牙设备线程
     */
    private class AcceptThread extends Thread {

        private final BluetoothServerSocket mBtServerSocket;

        public AcceptThread() {
            BluetoothServerSocket bss = null;
            // 获取蓝牙监听端口
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                try {
                    bss = mAdapter.listenUsingRfcommWithServiceRecord(BT_NAME, MY_UUID);
                } catch (IOException e) {
                    Log.e(TAG, "listen() failed", e);
                }
            }
            mBtServerSocket = bss;
        }

        public void run() {
            if (DEBUG) Log.e(TAG, "Begin mAcceptThread");
            setName("AcceptThread");
            BluetoothSocket socket;
            // 监听端口直到连接上
            while (BtState != CONNECTED) {
                try {
                    //成功连接时退出循环
                    socket = mBtServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }
                // 成功接收主设备
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (BtState) {
                            case LISTENING:
                            case CONNECTING:
                                // 启动已连接线程
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case IDLE:
                            case CONNECTED:
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            if (DEBUG) Log.e(TAG, "End mAcceptThread");
        }

        public void cancel() {
            if (DEBUG) Log.e(TAG, "cancel " + this);
            try {
                mBtServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }

    /**
     * 连接蓝牙设备的线程
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mBtSocket;
        private final BluetoothDevice mBtDevice;

        public ConnectThread(BluetoothDevice device) {
            mBtDevice = device;
            BluetoothSocket bs = null;

            // 根据UUID获取欲连接设备
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                try {
                    bs = device.createRfcommSocketToServiceRecord(MY_UUID);
                } catch (IOException e) {
                    Log.e(TAG, "create() failed", e);
                }
            }
            mBtSocket = bs;
        }

        public void run() {
            if (DEBUG) Log.e(TAG, "Begin mConnectThread");
            setName("ConnectThread");
            // 尝试连接蓝牙端口
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                try {
                    mBtSocket.connect();
                } catch (IOException e) {
                    try {
                        mBtSocket.close();
                        // 当连接失败或异常
                        connectionFailed();
                    } catch (IOException e2) {
                        Log.e(TAG, "close() fail", e2);
                    }
                    // 重新开启连接监听线程并退出连接线程
                    BluetoothService.this.acceptWait();
                    if (DEBUG) Log.d(TAG, "End mConnectThread");
                    return;
                }
            }

            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }
            // 启动已连接线程
            connected(mBtSocket, mBtDevice);
            if (DEBUG) Log.d(TAG, "End mConnectThread");
        }

        public void cancel() {
            if (DEBUG) Log.e(TAG, "cancel " + this);
            try {
                mBtSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() fail", e);
            }
        }
    }

    /**
     * 已连接的相关处理线程
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mBtSocket;
        private final InputStream mInputStream;
        private final OutputStream mOutputStream;

        public ConnectedThread(BluetoothSocket socket) {
            if (DEBUG) Log.d(TAG, "construct ConnectedThread");
            mBtSocket = socket;
            InputStream is = null;
            OutputStream os = null;

            // 获取输入输出流
            try {
                is = socket.getInputStream();
                os = socket.getOutputStream();
            } catch (IOException e) {
                Log.i(TAG, "get Stream fail", e);
            }

            mInputStream = is;
            mOutputStream = os;
        }

        public void run() {
            if (DEBUG) Log.i(TAG, "Begin mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // 监听输入流以备获取数据
            while (true) {
                try {
                    if (mInputStream != null) {
                        bytes = mInputStream.read(buffer);
                        // 将接受数据发回UI处理
                        if (bytes != -1) {
                            mHandler.obtainMessage(BluetoothActivity.STATUS_DATA, bytes, -1, buffer).sendToTarget();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "connection break", e);
                    connectionBreak();
                    break;
                }
                try {
                    //线程睡眠20ms以避免过于频繁工作  50ms->20ms 2017.12.2
                    //导致UI处理发回的数据不及时而阻塞
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (DEBUG) Log.i(TAG, "End mConnectedThread");
        }

        /**
         * 写输出流以发送数据
         *
         * @param buffer 欲输出字节流
         */
        public void write(byte[] buffer) {
            try {
                mOutputStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            if (DEBUG) Log.e(TAG, "cancel " + this);
            try {
                mBtSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() fail", e);
            }
        }
    }
}