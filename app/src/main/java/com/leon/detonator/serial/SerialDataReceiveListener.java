package com.leon.detonator.serial;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.util.ConstantUtils;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

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
    private int detonatorAmount;
    private int largeCurrentCount;
    private int breakCircuitCount;
    private int recordCurrentCount;

    public SerialDataReceiveListener(Context mContext, Runnable runnable) {
        this(mContext, runnable, true);
    }

    public SerialDataReceiveListener(Context mContext, Runnable runnable, boolean init) {
        this.mContext = mContext;
        this.handler = new Handler();
        this.runnable = runnable;
        this.rcvData = new byte[0];
        startAutoDetect = false;
        singleConnect = false;
        semiTest = false;
        startDetectShort = false;
        initStep = 1;
        try {
            serialPortUtil = SerialPortUtil.getInstance();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        initFinished = !init;
        if (init) {
            myHandler.sendEmptyMessageDelayed(HANDLE_BUS_VOLTAGE, ConstantUtils.INITIAL_TIME);
            myHandler.sendEmptyMessageDelayed(HANDLE_SHORT_DETECT, 2000);
        } else {
            rcvData = new byte[]{SerialCommand.INITIAL_FINISHED};
            handler.post(runnable);
        }
    }

    private Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            if (myHandler != null) {
                rcvData = new byte[0];
                switch (msg.what) {
                    case HANDLE_STATUS:
                        serialPortUtil.sendCmd("", SerialCommand.CODE_MEASURE_VALUE, 0);
                        myHandler.sendEmptyMessageDelayed(HANDLE_STATUS, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
                        break;
                    case HANDLE_BUS_VOLTAGE:
                        if (handler != null) {
                            serialPortUtil.sendCmd("", SerialCommand.CODE_BUS_CONTROL, initStep == 1 ? 0 : 0xFF, 0XFF, semiTest || singleConnect ? 0x12 : 0X16);
                            myHandler.sendEmptyMessageDelayed(HANDLE_BUS_VOLTAGE, ConstantUtils.RESEND_STATUS_TIMEOUT);
                        }
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
        myHandler.removeMessages(HANDLE_STATUS);
        if (initFinished)
            myHandler.removeMessages(HANDLE_BUS_VOLTAGE);
        this.startAutoDetect = startAutoDetect;
        initStep = 1;
        recordCurrentCount = 0;
        if (!startAutoDetect)
            serialPortUtil.setRecordLog(true);
        rcvData = new byte[0];
        if (startAutoDetect)
            myHandler.sendEmptyMessage(HANDLE_STATUS);
    }

    @Override
    public void onDataReceive(byte[] buffer) {
        if (null == handler)
            return;

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

        if (serialPortUtil.checkData(rcvData)) {
            byte code = rcvData[SerialCommand.CODE_CHAR_AT];
            if (code == SerialCommand.CODE_ERROR) {
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
                            float voltage = Float.intBitsToFloat((Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 5]) << 24)
                                    + (Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 4]) << 16)
                                    + (Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 3]) << 8)
                                    + Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 2]));
                            ((BaseActivity) mContext).setVoltage(voltage);
                            float data = Float.intBitsToFloat((Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 9]) << 24)
                                    + (Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 8]) << 16)
                                    + (Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 7]) << 8)
                                    + Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 6]));
                            ((BaseActivity) mContext).setCurrent(data);
                            if (startDetectShort) {
                                if (data > ConstantUtils.SHORT_CIRCUIT_CURRENT) {
                                    rcvData = new byte[]{SerialCommand.ALERT_SHORT_CIRCUIT};
                                    handler.post(runnable);
                                } else if (detonatorAmount > 0) {
//                                    if (data > ConstantUtils.CURRENT_PER_DETONATOR * detonatorAmount * ConstantUtils.CURRENT_OVER_PERCENTAGE) {
//                                        if (largeCurrentCount++ > ConstantUtils.CURRENT_DETECT_COUNT) {
//                                            rcvData = new byte[]{SerialCommand.ALERT_LARGE_CURRENT};
//                                            handler.post(runnable);
//                                        }
//                                    } else
                                    if (data < ConstantUtils.CURRENT_BREAK_CIRCUIT) {
                                        if (breakCircuitCount++ > ConstantUtils.CURRENT_DETECT_COUNT) {
                                            rcvData = new byte[]{SerialCommand.ALERT_BREAK_CIRCUIT};
                                            handler.post(runnable);
                                        }
                                    } else {
                                        breakCircuitCount = 0;
                                        largeCurrentCount = 0;
                                    }
                                }
                                if (recordCurrentCount++ == ConstantUtils.CURRENT_DETECT_COUNT)
                                    serialPortUtil.setRecordLog(false);
                                if (recordCurrentCount <= ConstantUtils.CURRENT_DETECT_COUNT)
                                    if (detonatorAmount > 0)
                                        BaseApplication.writeFile(String.format(Locale.getDefault(), "电流:%.2f, 电压:%.2f, 数量：%d", data, voltage, detonatorAmount));
                                    else
                                        BaseApplication.writeFile(String.format(Locale.getDefault(), "电流:%.2f, 电压:%.2f", data, voltage));
                            } else {
                                if (detonatorAmount > 0)
                                    BaseApplication.writeFile(String.format(Locale.getDefault(), "充电电流:%.2f, 电压:%.2f, 数量：%d", data, voltage, detonatorAmount));
                                else
                                    BaseApplication.writeFile(String.format(Locale.getDefault(), "电流:%.2f, 电压:%.2f", data, voltage));
                            }
                            if (semiTest) {
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

    public void setDetonatorAmount(int detonatorAmount) {
        this.detonatorAmount = detonatorAmount;
    }

    public void setStartDetectShort(boolean startDetectShort) {
        this.startDetectShort = startDetectShort;
    }

    public void closeAllHandler() {
        if (startAutoDetect)
            setStartAutoDetect(false);
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
