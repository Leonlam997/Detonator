package com.leon.detonator.serial;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.util.ConstantUtils;

import org.jetbrains.annotations.NotNull;

public class SerialDataReceiveListener implements SerialPortUtil.OnDataReceiveListener {
    private final Context mContext;
    private final Runnable runnable;
    private final int HANDLE_STATUS = 1;
    private final int HANDLE_BUS_VOLTAGE = 2;
    private final int HANDLE_SHORT_DETECT = 3;
    private Handler handler;
    private byte[] rcvData;
    private SerialPortUtil serialPortUtil;
    private boolean startAutoDetect;
    private boolean initFinished;
    private boolean singleConnect;
    private boolean semiTest;
    private boolean startDetectShort;
    private int initStep;
    private byte currentDetectType;

    public SerialDataReceiveListener(Context mContext, Runnable runnable) {
        this.mContext = mContext;
        this.handler = new Handler();
        this.runnable = runnable;
        this.rcvData = new byte[0];
        startAutoDetect = false;
        singleConnect = false;
        semiTest = false;
        startDetectShort = false;
        initStep = 1;
        currentDetectType = SerialCommand.MEASURE_CURRENT;
        try {
            serialPortUtil = SerialPortUtil.getInstance();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        initFinished = false;
        myHandler.sendEmptyMessageDelayed(HANDLE_BUS_VOLTAGE, ConstantUtils.INITIAL_TIME);
        myHandler.sendEmptyMessageDelayed(HANDLE_SHORT_DETECT, 2000);
    }

    private Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            if (myHandler != null) {
                myHandler.removeMessages(msg.what);
                rcvData = new byte[0];
                switch (msg.what) {
                    case HANDLE_STATUS:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_MEASURE_VALUE, initStep == 1 ? SerialCommand.MEASURE_VOLTAGE : currentDetectType);
                        break;
                    case HANDLE_BUS_VOLTAGE:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_BUS_CONTROL, initStep == 1 ? 0 : 0xFF, 0XFF, semiTest || singleConnect ? 0x12 : 0X16);
                        myHandler.sendEmptyMessageDelayed(msg.what, ConstantUtils.RESEND_STATUS_TIMEOUT);
                        break;
                    case HANDLE_SHORT_DETECT:
                        startDetectShort = true;
                        break;
                }
            }
            return false;
        }
    });

    public byte[] getRcvData() {
        byte[] data = rcvData.length == 0 ? new byte[]{0} : rcvData.clone();
        rcvData = new byte[0];
        return data;
    }

    public void setSingleConnect(boolean singleConnect) {
        this.singleConnect = singleConnect;
    }

    public void setSemiTest(boolean semiTest) {
        this.semiTest = semiTest;
    }

    public void setStartAutoDetect(boolean startAutoDetect) {
        this.startAutoDetect = startAutoDetect;
        initStep = 1;
        serialPortUtil.setTest(!startAutoDetect);
        rcvData = new byte[0];
        if (startAutoDetect) {
            myHandler.sendEmptyMessage(HANDLE_STATUS);
        } else {
            myHandler.removeMessages(HANDLE_STATUS);
        }
    }

    @Override
    public void onDataReceive(byte[] buffer) {
        if (null == handler)
            return;

        StringBuilder stringBuilder = new StringBuilder();
        if (buffer[0] == SerialCommand.DATA_PREFIX && buffer[1] == SerialCommand.DATA_PREFIX) {
            rcvData = new byte[buffer.length];
            rcvData = buffer.clone();
        } else {
            byte[] tmp = rcvData.clone();
            rcvData = new byte[tmp.length + buffer.length];
            System.arraycopy(tmp, 0, rcvData, 0, tmp.length);
            System.arraycopy(buffer, 0, rcvData, tmp.length, buffer.length);
        }
        for (int i = 5; i <= rcvData.length - 4; i++) {
            if (rcvData[i] == SerialCommand.DATA_SUFFIX && rcvData[i + 1] == SerialCommand.DATA_SUFFIX
                    && rcvData[i + 2] == SerialCommand.DATA_PREFIX && rcvData[i + 3] == SerialCommand.DATA_PREFIX) {
                System.arraycopy(rcvData.clone(), 0, rcvData, 0, i + 2);
            }
        }
        for (byte i : rcvData)
            stringBuilder.append(String.format("0x%02X ", i));
        Log.d("ZBEST", stringBuilder.toString());
        if (serialPortUtil.checkData(rcvData)) {
            byte code = rcvData[SerialCommand.CODE_CHAR_AT];
            if (code == (byte) 0xFF) {
                if ((rcvData[SerialCommand.CODE_CHAR_AT + 1] & (1 << 2)) != 0) {
                    rcvData = new byte[]{SerialCommand.ALERT_SHORT_CIRCUIT};
                    handler.post(runnable);
                }
            } else if (initFinished) {
                if (!startAutoDetect) {
                    handler.post(runnable);
                } else {
                    try {
                        if (code == SerialCommand.CODE_MEASURE_VALUE) {
                            float data = Float.intBitsToFloat((Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 5]) << 24)
                                    + (Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 4]) << 16)
                                    + (Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 3]) << 8)
                                    + Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 2]));
                            if (initStep == 1) {
                                initStep = 2;
                                ((BaseActivity) mContext).setVoltage(data);
                                myHandler.sendEmptyMessageDelayed(HANDLE_STATUS, ConstantUtils.COMMAND_DELAY_TIME);
                            } else {
                                initStep = 1;
                                ((BaseActivity) mContext).setCurrent(data);
                                if (data < 3000)
                                    currentDetectType = SerialCommand.MEASURE_CURRENT_LOW;
                                else if (data > 30000)
                                    currentDetectType = SerialCommand.MEASURE_CURRENT_HIGH;
                                else
                                    currentDetectType = SerialCommand.MEASURE_CURRENT;
                                if (data > ConstantUtils.SHORT_CIRCUIT_CURRENT && startDetectShort) {
                                    rcvData = new byte[]{SerialCommand.ALERT_SHORT_CIRCUIT};
                                    handler.post(runnable);
                                }
                                myHandler.sendEmptyMessageDelayed(HANDLE_STATUS, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
                            }
                            if (semiTest) {
                                rcvData[SerialCommand.CODE_CHAR_AT + 1] = (byte) initStep;
                                handler.post(runnable);
                            }
                        }
                    } catch (Exception e) {
                        BaseApplication.writeErrorLog(e);
                    }
                }
            } else {
                if (code == SerialCommand.CODE_BUS_CONTROL) {
                    myHandler.removeMessages(HANDLE_BUS_VOLTAGE);
                    if (0 == rcvData[SerialCommand.CODE_CHAR_AT + 1]) {
                        if (initStep == 1) {
                            initStep = 2;
                            myHandler.sendEmptyMessageDelayed(HANDLE_BUS_VOLTAGE, ConstantUtils.COMMAND_DELAY_TIME);
                        } else {
                            initFinished = true;
                            rcvData = new byte[]{SerialCommand.INITIAL_FINISHED};
                            handler.post(runnable);
                        }
                    } else {
                        rcvData = new byte[]{SerialCommand.INITIAL_FAIL};
                        handler.post(runnable);
                    }
                }
            }
        }
    }

    public void closeAllHandler() {
        startAutoDetect = false;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        if (myHandler != null) {
            myHandler.removeCallbacksAndMessages(null);
            myHandler = null;
        }
    }
}
