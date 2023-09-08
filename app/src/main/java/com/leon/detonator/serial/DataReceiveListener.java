package com.leon.detonator.serial;

import android.os.Handler;

import com.kfree.expd.ExpdDevMgr;
import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.util.ConstantUtils;

import java.util.Locale;

public class DataReceiveListener implements SerialPortUtil.OnDataReceiveListener {
    private static DataReceiveListener listener;
    private SerialPortUtil serialPortUtil;
    private BaseActivity activity;
    private Handler handler;
    public static final int HANDLER_RECEIVED_DATA = 300;
    private final int HANDLE_STATUS = 1;
    private final int HANDLE_BUS_VOLTAGE = 2;
    private final int HANDLE_START_SHORT_DETECT = 3;
    private byte[] rcvData;
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
    private Handler myHandler = new Handler(msg -> {
        rcvData = new byte[0];
        switch (msg.what) {
            case HANDLE_STATUS:
                serialPortUtil.sendCmd("", SerialCommand.CODE_MEASURE_VALUE, 0);
                msg.getTarget().sendEmptyMessageDelayed(HANDLE_STATUS, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
                break;
            case HANDLE_BUS_VOLTAGE:
                serialPortUtil.sendCmd("", SerialCommand.CODE_BUS_CONTROL, initStep == 1 ? 0 : 0xFF, 0XFF, semiTest || singleConnect ? 0x12 : 0x16);
                msg.getTarget().sendEmptyMessageDelayed(HANDLE_BUS_VOLTAGE, ConstantUtils.RESEND_STATUS_TIMEOUT);
                break;
            case HANDLE_START_SHORT_DETECT:
                startDetectShort = true;
                break;
        }
        return false;
    });

    public static DataReceiveListener getInstance(BaseActivity activity, Handler handler) {
        if (null == listener) {
            listener = new DataReceiveListener();
            listener.onCreate(activity, handler);
        } else if (listener.activity != activity || listener.handler != handler) {
            listener.setStartDetectShort(false);
            listener.setStartAutoDetect(false);
            listener.activity = activity;
            listener.handler = handler;
            if (listener.initFinished)
                handler.obtainMessage(HANDLER_RECEIVED_DATA, new byte[]{SerialCommand.INITIAL_FINISHED}).sendToTarget();
        }
        return listener;
    }

    private void onCreate(BaseActivity activity, Handler handler) {
        this.activity = activity;
        this.handler = handler;
        this.rcvData = new byte[0];
        startAutoDetect = false;
        singleConnect = false;
        semiTest = false;
        startDetectShort = false;
        initStep = 1;
        try {
            serialPortUtil = SerialPortUtil.getInstance(activity);
            serialPortUtil.setDevEventCb(new ExpdDevMgr.IOnExpdDevEventCb() {
                @Override
                public void onSafeSwitchStChg(boolean b) {
                    ((BaseApplication) activity.getApplication()).myToast(activity, b ? R.string.message_safe_switch_on : R.string.message_safe_switch_off);
                }

                @Override
                public void onBackcoverStChg(boolean b) {
                    ((BaseApplication) activity.getApplication()).myToast(activity, b ? R.string.message_back_cover_open : R.string.message_back_cover_close);
                }
            });
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        myHandler.sendEmptyMessageDelayed(HANDLE_BUS_VOLTAGE, ConstantUtils.INITIAL_TIME);
        myHandler.sendEmptyMessageDelayed(HANDLE_START_SHORT_DETECT, 2000);
    }

    public void setSingleConnect(boolean singleConnect) {
        this.singleConnect = singleConnect;
    }

    public void setSemiTest(boolean semiTest) {
        this.semiTest = semiTest;
    }

    public void setStartDetectShort(boolean startDetectShort) {
        this.startDetectShort = startDetectShort;
    }

    public void setStartAutoDetect(boolean startAutoDetect) {
        myHandler.removeMessages(HANDLE_STATUS);
        this.startAutoDetect = startAutoDetect;
        recordCurrentCount = 0;
        if (!startAutoDetect)
            serialPortUtil.setRecordLog(true);
        rcvData = new byte[0];
        if (startAutoDetect)
            myHandler.sendEmptyMessage(HANDLE_STATUS);
    }

    public void setDetonatorAmount(int detonatorAmount) {
        this.detonatorAmount = detonatorAmount;
    }

    @Override
    public void onDataReceive(byte[] buffer) {
        if (initStep == 3) {
            destroy();
            return;
        }
        if (buffer.length > 1 && buffer[0] == SerialCommand.DATA_PREFIX && buffer[1] == SerialCommand.DATA_PREFIX) {
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
                if ((rcvData[SerialCommand.CODE_CHAR_AT + 1] & (1 << 2)) != 0)
                    handler.obtainMessage(HANDLER_RECEIVED_DATA, new byte[]{SerialCommand.ALERT_SHORT_CIRCUIT}).sendToTarget();
            } else if (initFinished) {
                if (!startAutoDetect) {
                    handler.obtainMessage(HANDLER_RECEIVED_DATA, rcvData.clone()).sendToTarget();
                } else {
                    try {
                        if (code == SerialCommand.CODE_MEASURE_VALUE) {
                            float voltage = Float.intBitsToFloat((Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 5]) << 24)
                                    + (Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 4]) << 16)
                                    + (Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 3]) << 8)
                                    + Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 2]));
                            activity.setVoltage(voltage);
                            float data = Float.intBitsToFloat((Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 9]) << 24)
                                    + (Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 8]) << 16)
                                    + (Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 7]) << 8)
                                    + Byte.toUnsignedInt(rcvData[SerialCommand.CODE_CHAR_AT + 6]));
                            activity.setCurrent(data);
                            if (startDetectShort) {
                                if (data > ConstantUtils.SHORT_CIRCUIT_CURRENT) {
                                    BaseApplication.writeFile(String.format(Locale.getDefault(), "短路电流:%.2f, 电压:%.2f", data, voltage));
                                    if (largeCurrentCount++ > ConstantUtils.CURRENT_DETECT_COUNT)
                                        handler.obtainMessage(HANDLER_RECEIVED_DATA, new byte[]{SerialCommand.ALERT_SHORT_CIRCUIT}).sendToTarget();
                                } else {
                                    if (detonatorAmount > 0) {
//                                    if (data > ConstantUtils.CURRENT_PER_DETONATOR * detonatorAmount * ConstantUtils.CURRENT_OVER_PERCENTAGE) {
//                                        if (largeCurrentCount++ > ConstantUtils.CURRENT_DETECT_COUNT)
//                                            handler.obtainMessage(HANDLER_RECEIVED_DATA, new byte[]{SerialCommand.ALERT_LARGE_CURRENT}).sendToTarget();
//                                    } else
                                        if (data < ConstantUtils.CURRENT_BREAK_CIRCUIT) {
                                            if (breakCircuitCount++ > ConstantUtils.CURRENT_DETECT_COUNT)
                                                handler.obtainMessage(HANDLER_RECEIVED_DATA, new byte[]{SerialCommand.ALERT_BREAK_CIRCUIT}).sendToTarget();
                                        } else
                                            breakCircuitCount = 0;
                                    }
                                    largeCurrentCount = 0;
                                }
                                if (recordCurrentCount++ == ConstantUtils.CURRENT_DETECT_COUNT)
                                    serialPortUtil.setRecordLog(false);
                                if (recordCurrentCount <= ConstantUtils.CURRENT_DETECT_COUNT)
                                    if (detonatorAmount > 0)
                                        BaseApplication.writeFile(String.format(Locale.getDefault(), "电流:%.2f, 电压:%.2f, 数量：%d", data, voltage, detonatorAmount));
                                    else
                                        BaseApplication.writeFile(String.format(Locale.getDefault(), "电流:%.2f, 电压:%.2f", data, voltage));
                            } else {
                                breakCircuitCount = 0;
                                largeCurrentCount = 0;
                                if (detonatorAmount > 0)
                                    BaseApplication.writeFile(String.format(Locale.getDefault(), "充电电流:%.2f, 电压:%.2f, 数量：%d", data, voltage, detonatorAmount));
                                else
                                    BaseApplication.writeFile(String.format(Locale.getDefault(), "电流:%.2f, 电压:%.2f", data, voltage));
                            }
                            if (semiTest)
                                handler.obtainMessage(HANDLER_RECEIVED_DATA, rcvData.clone()).sendToTarget();
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
                            handler.obtainMessage(HANDLER_RECEIVED_DATA, new byte[]{SerialCommand.INITIAL_FINISHED}).sendToTarget();
                        }
                    } else
                        handler.obtainMessage(HANDLER_RECEIVED_DATA, new byte[]{SerialCommand.INITIAL_FAIL}).sendToTarget();
                }
            }
            rcvData = new byte[0];
        }
    }

    private void destroy() {
        if (myHandler != null) {
            myHandler.removeCallbacksAndMessages(null);
            myHandler = null;
        }
        serialPortUtil.closeSerialPort();
        listener = null;
    }

    public void closeAllHandler() {
        if (startAutoDetect)
            setStartAutoDetect(false);
        initStep = 3;
        serialPortUtil.sendCmd("", SerialCommand.CODE_BUS_CONTROL, 0, 0, 0x12);
    }
}
