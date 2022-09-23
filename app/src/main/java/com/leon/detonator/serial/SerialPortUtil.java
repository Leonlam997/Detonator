package com.leon.detonator.serial;

import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.util.CRC8;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.FilePath;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    private boolean test = true;
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private OnDataReceiveListener onDataReceiveListener = null;
    private boolean isStop = false;
    private byte lastAction;

    public static SerialPortUtil getInstance() throws IOException {
        if (null == portUtil) {
            portUtil = new SerialPortUtil();
            try {
                portUtil.onCreate();
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
    public void onCreate() throws IOException {
        try {
            String path = "/dev/ttyMT0";
            final int baudRate = 115200;
            mSerialPort = new SerialPort(new File(path), baudRate);

            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();

            mReadThread = new ReadThread();
            isStop = false;
            mReadThread.start();

        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
            throw new IOException();
        }
        //initBle();
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

        try {
            if (mOutputStream != null) {
                mOutputStream.write(mBuffer);
            } else {
                result = false;
            }
        } catch (IOException e) {
            BaseApplication.writeErrorLog(e);
            result = false;
        }
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

        try {
            if (mOutputStream != null) {
                mOutputStream.write(cmd);
            } else {
                result = false;
            }
        } catch (IOException e) {
            BaseApplication.writeErrorLog(e);
            result = false;
        }
        writeToFile(cmd, true);
        return result;
    }

    public void writeToFile(byte[] cmd, boolean send) {
        if (test) {
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.", Locale.CHINA);
                BufferedWriter bfw = new BufferedWriter(new FileWriter(FilePath.FILE_SERIAL_LOG, true));
                bfw.write(send ? "S:" : "R:");
                bfw.newLine();
                for (byte i : cmd)
                    bfw.write(String.format("%02X", i) + " ");
                bfw.newLine();
                bfw.write(df.format(new Date()) + String.format(Locale.CHINA, "%03d", System.currentTimeMillis() % 1000));
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
        if (test) {
            try {
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.", Locale.CHINA);
                BufferedWriter bfw = new BufferedWriter(new FileWriter(FilePath.FILE_SERIAL_LOG, true));
                bfw.write("S:");
                bfw.newLine();
                bfw.write(cmd);
                bfw.newLine();
                bfw.write(df.format(new Date()) + String.format(Locale.CHINA, "%03d", System.currentTimeMillis() % 1000));
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
        switch (action) {
            case SerialCommand.CODE_WRITE_FIELD:
            case SerialCommand.CODE_READ_FIELD:
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
            case SerialCommand.CODE_READ_FIELD:
            case SerialCommand.CODE_READ_SHELL:
            case SerialCommand.CODE_READ_PARAM:
            case SerialCommand.CODE_BRIDGE_RESISTANCE:
            case SerialCommand.CODE_CAPACITOR:
            case SerialCommand.CODE_SINGLE_CHARGE:
            case SerialCommand.CODE_WRITE_UID:
            case SerialCommand.CODE_WRITE_PSW:
            case SerialCommand.CODE_CHECK_PSW:
            case SerialCommand.CODE_SCAN_UID:
                bufferSend[i++] = len;
            case SerialCommand.CODE_WRITE_SHELL:
                if (cmd.length() < len)
                    for (int j = 0; j < len; j++)
                        bufferSend[i++] = 0;
                else
                    for (byte uid : cmd.substring(cmd.length() - len).getBytes())
                        bufferSend[i++] = uid;
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
        lastAction = action;
        return sendCmd(bufferSend);
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public boolean checkData(byte[] data) {
        try {
            return data.length >= 5 + data[2] && data[0] == SerialCommand.DATA_PREFIX && data[1] == SerialCommand.DATA_PREFIX && data[3] == lastAction
                    && data[3 + data[2]] == CRC8.calcCRC(Arrays.copyOfRange(data, 3, 3 + data[2]));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        isStop = true;
        if (mReadThread != null) {
            mReadThread.interrupt();
        }
        if (mSerialPort != null) {
            mSerialPort.close();
        }
        if (portUtil != null)
            portUtil = null;
    }

    public interface OnDataReceiveListener {
        void onDataReceive(byte[] buffer);
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isStop && !isInterrupted()) {
                int size;
                try {
                    if (mInputStream == null)
                        return;
                    byte[] buffer = new byte[512];
                    size = mInputStream.read(buffer);
                    if (size > 0) {
                        writeToFile(Arrays.copyOfRange(buffer, 0, size), false);
                        if (null != onDataReceiveListener) {
                            onDataReceiveListener.onDataReceive(Arrays.copyOfRange(buffer, 0, size));
                        }
                    }
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                    isStop = true;
                    interrupt();
                    return;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    isStop = true;
                    interrupt();
                }
            }
        }
    }

}