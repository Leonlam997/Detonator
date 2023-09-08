package com.leon.detonator.serial;

import android.content.Context;
import android.os.Handler;

import com.kfree.comm.system.ScanQrControl;
import com.kfree.expd.ExpdDevMgr;
import com.kfree.expd.OnOpenSerialPortListener;
import com.kfree.expd.OnSerialPortDataListener;
import com.kfree.expd.Status;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.util.CRC8;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * 串口操作类
 *
 * @author Leon
 */
public class SerialPortUtil {
    private static SerialPortUtil portUtil;
    private boolean recordLog = true;
    private OnDataReceiveListener onDataReceiveListener = null;
    private byte lastAction;
    private ExpdDevMgr mExpDevMgr;
    private ScanQrControl mScanner = null;
    private final Handler scannerHandler = new Handler(msg -> {
        mScanner.stopScan();
        byte[] buffer = new byte[8];
        buffer[0] = SerialCommand.DATA_PREFIX;
        buffer[1] = SerialCommand.DATA_PREFIX;
        buffer[2] = 2;
        buffer[3] = SerialCommand.CODE_SCAN_CODE;
        buffer[4] = (byte) 0xFF;
        buffer[5] = CRC8.calcCRC(Arrays.copyOfRange(buffer, 3, 3 + buffer[2]));
        buffer[6] = SerialCommand.DATA_SUFFIX;
        buffer[7] = SerialCommand.DATA_SUFFIX;
        writeToFile(buffer, false);
        if (onDataReceiveListener != null)
            onDataReceiveListener.onDataReceive(buffer);
        return false;
    });

    public static SerialPortUtil getInstance(Context context) throws IOException {
        if (null == portUtil) {
            portUtil = new SerialPortUtil();
            try {
                portUtil.onCreate(context);
            } catch (IOException e) {
                portUtil = null;
                throw new IOException();
            }
        }
        return portUtil;
    }

    public void setOnDataReceiveListener(OnDataReceiveListener dataReceiveListener) {
        onDataReceiveListener = dataReceiveListener;
    }

    /**
     * 初始化串口信息
     */
    private void onCreate(Context context) throws IOException {
        try {
            mExpDevMgr = new ExpdDevMgr(context);
            mExpDevMgr.exPowerOn();
            mExpDevMgr.openSerialPort(new OnOpenSerialPortListener() {
                @Override
                public void onSuccess(File portDevFile) {
                }

                @Override
                public void onFail(File portDevFile, Status errMessage) {
                    try {
                        throw new IOException();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, new OnSerialPortDataListener() {
                @Override
                public void onDataReceived(byte[] buffer) {
                    if (onDataReceiveListener != null)
                        onDataReceiveListener.onDataReceive(buffer);
                    writeToFile(buffer, false);
                }

                @Override
                public void onDataSent(byte[] buffer) {
                    writeToFile(buffer, true);
                }
            }, 115200);
            mScanner = new ScanQrControl(context);
            mScanner.registerScanCb(new ScanQrControl.IScan() {
                @Override
                public void onScanStart(int timeoutSec) {

                }

                @Override
                public void onScanResult(boolean isSuccess, String scanResultStr) {
                    scannerHandler.removeMessages(1);
                    BaseApplication.writeFile(scanResultStr + "," + scanResultStr.length());
                    byte[] buffer = new byte[isSuccess ? scanResultStr.length() + 8 : 8];
                    buffer[0] = SerialCommand.DATA_PREFIX;
                    buffer[1] = SerialCommand.DATA_PREFIX;
                    buffer[2] = isSuccess ? (byte) (2 + scanResultStr.length()) : 2;
                    buffer[3] = SerialCommand.CODE_SCAN_CODE;
                    buffer[4] = isSuccess ? 0 : (byte) 0xFF;
                    if (isSuccess)
                        System.arraycopy(scanResultStr.getBytes(), 0, buffer, 5, scanResultStr.length());
                    buffer[buffer.length - 3] = CRC8.calcCRC(Arrays.copyOfRange(buffer, 3, 3 + buffer[2]));
                    buffer[buffer.length - 2] = SerialCommand.DATA_SUFFIX;
                    buffer[buffer.length - 1] = SerialCommand.DATA_SUFFIX;
                    writeToFile(buffer, false);
                    if (onDataReceiveListener != null)
                        onDataReceiveListener.onDataReceive(buffer);
                }
            });
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
            throw new IOException();
        }
    }

    /**
     * 发送指令到串口
     *
     * @param cmd:
     * @return result
     */
    public boolean sendCmd(String cmd) {
        boolean result = true;
        byte[] mBuffer = cmd.getBytes();
        mExpDevMgr.sendBytes(mBuffer);
        writeToFile(cmd);
        return result;
    }

    /**
     * 发送指令到串口
     *
     * @param cmd:
     * @return true:发送成功 false:发送失败
     */
    public boolean sendCmd(byte[] cmd) {
        boolean result = true;

        mExpDevMgr.sendBytes(cmd);
        return result;
    }

    public void writeToFile(byte[] cmd, boolean send) {
        if (recordLog) {
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.", Locale.getDefault());
                BufferedWriter bfw = new BufferedWriter(new FileWriter(FilePath.FILE_SERIAL_LOG, true));
                bfw.write(send ? "S:" : "R:");
                bfw.newLine();
                for (byte i : cmd)
                    bfw.write(String.format("%02X", i) + " ");
                bfw.newLine();
                bfw.write(df.format(new Date()) + String.format(Locale.getDefault(), "%03d", System.currentTimeMillis() % 1000));
                bfw.newLine();
                bfw.newLine();
                bfw.flush();
                bfw.close();
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
    }

    private void writeToFile(String cmd) {
        if (recordLog) {
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.", Locale.getDefault());
                BufferedWriter bfw = new BufferedWriter(new FileWriter(FilePath.FILE_SERIAL_LOG, true));
                bfw.write("S:");
                bfw.newLine();
                bfw.write(cmd);
                bfw.newLine();
                bfw.write(df.format(new Date()) + String.format(Locale.getDefault(), "%03d", System.currentTimeMillis() % 1000));
                bfw.newLine();
                bfw.newLine();
                bfw.flush();
                bfw.close();
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
    }

    public boolean sendCmd(String cmd, byte action, int... timeout) {
        byte[] bufferSend;
        byte len = ConstantUtils.UID_LEN;
        lastAction = action;
        switch (action) {
            case SerialCommand.CODE_READ_FIELD:
            case SerialCommand.CODE_WRITE_FIELD:
                bufferSend = new byte[15 + len];
                break;
            case SerialCommand.CODE_WRITE_PSW:
            case SerialCommand.CODE_CHECK_PSW:
                len = ConstantUtils.PSW_LEN;
            case SerialCommand.CODE_READ_SHELL:
            case SerialCommand.CODE_READ_PARAM:
            case SerialCommand.CODE_WRITE_UID:
            case SerialCommand.CODE_SCAN_UID:
                bufferSend = new byte[8 + len];
                break;
            case SerialCommand.CODE_BRIDGE_RESISTANCE:
            case SerialCommand.CODE_CAPACITOR:
                bufferSend = new byte[9 + len];
                break;
            case SerialCommand.CODE_SINGLE_CHARGE:
                bufferSend = new byte[10 + len];
                break;
            case SerialCommand.CODE_WRITE_SHELL:
                len = 13;
                bufferSend = new byte[7 + len];
                break;
            case SerialCommand.CODE_SINGLE_WRITE_FIELD:
                bufferSend = new byte[14];
                break;
            case SerialCommand.CODE_EXPLODE:
                bufferSend = new byte[11];
                break;
            case SerialCommand.CODE_DELAY:
            case SerialCommand.CODE_BUS_CONTROL:
            case SerialCommand.CODE_SINGLE_WRITE_CONFIG:
            case SerialCommand.CODE_CHECK_CONFIG:
                bufferSend = new byte[10];
                break;
            case SerialCommand.CODE_RESEND:
            case SerialCommand.CODE_CHECK_ONLINE:
            case SerialCommand.CODE_CHECK_STATUS:
            case SerialCommand.CODE_SCAN_CODE:
                bufferSend = new byte[9];
                break;
            case SerialCommand.CODE_RESET:
            case SerialCommand.CODE_CHARGE:
            case SerialCommand.CODE_SINGLE_WRITE_PARAM:
            case SerialCommand.CODE_SINGLE_CAPACITOR:
            case SerialCommand.CODE_SINGLE_BRIDGE_RESISTANCE:
            case SerialCommand.CODE_MEASURE_VALUE:
            case SerialCommand.CODE_SET_POLARITY:
            case SerialCommand.CODE_SET_PARAM_LEVEL:
                bufferSend = new byte[8];
                break;
            default:
                bufferSend = new byte[7];
                break;
        }
        int i = 0;
        bufferSend[i++] = SerialCommand.DATA_PREFIX;
        bufferSend[i++] = SerialCommand.DATA_PREFIX;
        bufferSend[i++] = (byte) (bufferSend.length - 6);
        bufferSend[i++] = action;
        switch (action) {
            case SerialCommand.CODE_WRITE_FIELD:
            case SerialCommand.CODE_READ_SHELL:
            case SerialCommand.CODE_READ_PARAM:
            case SerialCommand.CODE_BRIDGE_RESISTANCE:
            case SerialCommand.CODE_CAPACITOR:
            case SerialCommand.CODE_SINGLE_CHARGE:
            case SerialCommand.CODE_WRITE_UID:
            case SerialCommand.CODE_WRITE_PSW:
            case SerialCommand.CODE_CHECK_PSW:
            case SerialCommand.CODE_READ_FIELD:
            case SerialCommand.CODE_SCAN_UID:
                bufferSend[i++] = len;
            case SerialCommand.CODE_WRITE_SHELL:
                if (cmd.length() < len)
                    for (int j = 0; j < len; j++)
                        bufferSend[i++] = 0;
                else {
                    byte[] data = cmd.substring(cmd.length() - len).getBytes();
                    if (len == ConstantUtils.UID_LEN && data[1] > 0x39)
                        data[1] -= 0x40;
                    for (byte uid : data)
                        bufferSend[i++] = uid;
                }
                break;
        }
        int j = 0;
        switch (action) {
            case SerialCommand.CODE_SINGLE_WRITE_FIELD:
            case SerialCommand.CODE_WRITE_FIELD:
            case SerialCommand.CODE_READ_FIELD:
                bufferSend[i++] = (byte) (timeout[j] >> 8 & 0xFF);
                bufferSend[i++] = (byte) (timeout[j++] & 0xFF);
                bufferSend[i++] = (byte) (timeout[j] >> 16 & 0xFF);
                bufferSend[i++] = (byte) (timeout[j] >> 8 & 0xFF);
                bufferSend[i++] = (byte) (timeout[j++] & 0xFF);
            case SerialCommand.CODE_CHECK_ONLINE:
            case SerialCommand.CODE_CHECK_STATUS:
            case SerialCommand.CODE_SCAN_CODE:
                bufferSend[i++] = (byte) (timeout[j] >> 8 & 0xFF);
                bufferSend[i++] = (byte) (timeout[j] & 0xFF);
                break;
            case SerialCommand.CODE_BUS_CONTROL:
            case SerialCommand.CODE_SINGLE_WRITE_CONFIG:
            case SerialCommand.CODE_CHECK_CONFIG:
                bufferSend[i++] = (byte) (timeout[j++] & 0xFF);
            case SerialCommand.CODE_SINGLE_CHARGE:
                bufferSend[i++] = (byte) (timeout[j++] & 0xFF);
            case SerialCommand.CODE_SINGLE_WRITE_PARAM:
            case SerialCommand.CODE_SINGLE_CAPACITOR:
            case SerialCommand.CODE_BRIDGE_RESISTANCE:
            case SerialCommand.CODE_CAPACITOR:
            case SerialCommand.CODE_RESET:
            case SerialCommand.CODE_CHARGE:
            case SerialCommand.CODE_SINGLE_BRIDGE_RESISTANCE:
            case SerialCommand.CODE_MEASURE_VALUE:
            case SerialCommand.CODE_SET_POLARITY:
            case SerialCommand.CODE_SET_PARAM_LEVEL:
                bufferSend[i++] = (byte) (timeout[j] & 0xFF);
                break;
            case SerialCommand.CODE_EXPLODE:
                bufferSend[i++] = (byte) (timeout[j++] & 0xFF);
            case SerialCommand.CODE_DELAY:
                bufferSend[i++] = (byte) (timeout[j] >> 16 & 0xFF);
                bufferSend[i++] = (byte) (timeout[j] >> 8 & 0xFF);
                bufferSend[i++] = (byte) (timeout[j] & 0xFF);
                break;
            case SerialCommand.CODE_RESEND:
                bufferSend[i++] = (byte) 0xA5;
                bufferSend[i++] = (byte) 0x5A;
                break;
        }
        bufferSend[i++] = CRC8.calcCRC(Arrays.copyOfRange(bufferSend, 3, i - 1));
        bufferSend[i++] = SerialCommand.DATA_SUFFIX;
        bufferSend[i] = SerialCommand.DATA_SUFFIX;
        if (recordLog) {
            StringBuilder data = new StringBuilder("发送：");
            switch (lastAction) {
                case SerialCommand.CODE_WRITE_FIELD:
                    data.append("写延期:").append(timeout[1]).append(",序号：").append(timeout[2]);
                    break;
                case SerialCommand.CODE_READ_FIELD:
                    data.append("读延期");
                    break;
                case SerialCommand.CODE_READ_SHELL:
                    data.append("读管壳码");
                    break;
                case SerialCommand.CODE_BRIDGE_RESISTANCE:
                    data.append("读桥丝");
                    break;
                case SerialCommand.CODE_CAPACITOR:
                    data.append("读电容");
                    break;
                case SerialCommand.CODE_CLEAR_READ_STATUS:
                    data.append("清除");
                    break;
                case SerialCommand.CODE_INITIAL:
                    data.append("雷管初始化");
                    break;
                case SerialCommand.CODE_EXPLODE:
                    data.append("起爆");
                    break;
                case SerialCommand.CODE_RESET:
                    data.append("复位");
                    break;
                case SerialCommand.CODE_SCAN_UID:
                    data.append("点名");
                    break;
                case SerialCommand.CODE_DELAY:
                    data.append("延期标定");
                    break;
                case SerialCommand.CODE_CHECK_ONLINE:
                    data.append("在线检测");
                    break;
                case SerialCommand.CODE_CHECK_STATUS:
                    data.append("逐发检测");
                    break;
                case SerialCommand.CODE_CHECK_PSW:
                    data.append("密码检验");
                    break;
                case SerialCommand.CODE_GET_ALL_STATUS:
                    data.append("检查状态");
                    break;
                case SerialCommand.CODE_CHARGE:
                    if (timeout[0] == 0)
                        data.append("放电");
                    else
                        data.append("充电").append(timeout[0]).append("V");
                    break;
                case SerialCommand.CODE_CHECK_CONFIG:
                    data.append("检查配置信息");
                    break;
                case SerialCommand.CODE_LOCK:
                    data.append("锁码");
                    break;
                case SerialCommand.CODE_WRITE_UID:
                    data.append("写UID");
                    break;
                case SerialCommand.CODE_WRITE_PSW:
                    data.append("写密码");
                    break;
                case SerialCommand.CODE_WRITE_SHELL:
                    data.append("写管壳码");
                    break;
                case SerialCommand.CODE_SINGLE_WRITE_CONFIG:
                    data.append("单发写配置");
                    break;
                case SerialCommand.CODE_SINGLE_READ_CONFIG:
                    data.append("单发读配置");
                    break;
                case SerialCommand.CODE_SINGLE_WRITE_FIELD:
                    data.append("单发写延期");
                    break;
                case SerialCommand.CODE_WRITE_CLOCK:
                    data.append("写时钟");
                    break;
                case SerialCommand.CODE_BUS_CONTROL:
                    data.append("总线：").append(timeout[0] == 0 ? "断电" : "供电")
                            .append(timeout[0] == 0 ? "" : String.format("%X", timeout[2])).append("V");
                    break;
                case SerialCommand.CODE_SCAN_CODE:
                    data.append("扫码");
                    break;
                default:
                    data = null;
            }
            if (data != null)
                BaseApplication.writeFile(data.toString());
        }
        if (action == SerialCommand.CODE_SCAN_CODE) {
            mScanner.startScan();
            scannerHandler.sendEmptyMessageDelayed(1, timeout[0]);
            writeToFile(bufferSend, true);
            return true;
        } else
            return sendCmd(bufferSend);
    }

    public void setDevEventCb(ExpdDevMgr.IOnExpdDevEventCb eventCb) {
        mExpDevMgr.setExpdDevEventCb(eventCb);
    }

    public boolean isSafeSwitchOpened() {
        return mExpDevMgr.isSafeSwitchOpen();
    }

    public void setRecordLog(boolean recordLog) {
        this.recordLog = recordLog;
    }

    public boolean checkData(byte[] data) {
        try {
            return data.length >= 5 + data[2] && data[0] == SerialCommand.DATA_PREFIX && data[1] == SerialCommand.DATA_PREFIX
                    && (data[3] == lastAction || data[3] == SerialCommand.CODE_ERROR)
                    && data[3 + data[2]] == CRC8.calcCRC(Arrays.copyOfRange(data, 3, 3 + data[2]));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        mExpDevMgr.closeSerialPort();
        if (portUtil != null)
            portUtil = null;
        mScanner.unregisterScanCb();
        mExpDevMgr.deInit();
        mExpDevMgr.exPowerOff();
    }

    public interface OnDataReceiveListener {
        void onDataReceive(byte[] buffer);
    }

}