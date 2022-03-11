package com.leon.detonator.Serial;

import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Util.ConstantUtils;
import com.leon.detonator.Util.FilePath;

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
import java.util.Objects;

/**
 * 串口操作类
 *
 * @author Leon
 */
public class SerialPortUtil {
    private static SerialPortUtil portUtil;
    private final boolean test = true;
    private final boolean newLG = BaseApplication.readSettings().isNewLG();
    private SerialPort mSerialPort;
    private OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private OnDataReceiveListener onDataReceiveListener = null;
    private boolean isStop = false;

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
        if (newLG)
            return true;
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

    private void writeToFile(byte[] cmd, boolean send) {
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

    public boolean sendCmd(String cmd, SerialCommand.ACTION_TYPE action, int timeout) {
        byte[] bytes;
        int i = 0;
        byte[] bufferSend;
        if (!newLG) {
            switch (action) {
                case WRITE_SN:
                    return writeSn(cmd);
                case EXPLODE:
                case NEW_EXPLODE:
                    bytes = SerialCommand.CMD_EXPLODE.getBytes();
                    break;
                default:
                    bytes = SerialCommand.CMD_COMMON.getBytes();
                    break;
            }

            bufferSend = new byte[bytes.length + 20];
            for (; i < bytes.length; i++)
                bufferSend[i] = bytes[i];
            bufferSend[i++] = (byte) 0xFE;
            bufferSend[i++] = (byte) 0xFE;
            if ((byte) 0xA8 == Objects.requireNonNull(SerialCommand.ACTION_CODE.get(action))[0])
                bufferSend[i++] = Objects.requireNonNull(SerialCommand.ACTION_CODE.get(action))[0];
            else
                for (Byte code : Objects.requireNonNull(SerialCommand.ACTION_CODE.get(action)))
                    bufferSend[i++] = code;
            if (cmd != null && cmd.length() > 0) {
                byte[] tmpBuffer = cmd.getBytes();
                byte temp = tmpBuffer[7];  //Character code
                System.arraycopy(tmpBuffer, 8, tmpBuffer, 7, 5);
                if (SerialCommand.ACTION_TYPE.SHORT_CMD1_GET_DELAY == action || SerialCommand.ACTION_TYPE.SHORT_CMD1_SET_DELAY == action) {
                    bufferSend[i++] = (byte) (((tmpBuffer[10] - 0x30) * 0x10) + (tmpBuffer[11] - 0x30));
                } else if (SerialCommand.ACTION_TYPE.SHORT_CMD2_GET_DELAY == action || SerialCommand.ACTION_TYPE.SHORT_CMD2_SET_DELAY == action) {
                    bufferSend[i++] = (byte) (((tmpBuffer[8] - 0x30) * 0x10) + (tmpBuffer[9] - 0x30));
                    bufferSend[i++] = (byte) (((tmpBuffer[10] - 0x30) * 0x10) + (tmpBuffer[11] - 0x30));
                } else {
                    for (int j = 0; j < 6; j++)
                        bufferSend[i++] = (byte) ((tmpBuffer[2 * j] == '*' ? 0xF0 : ((tmpBuffer[2 * j] - 0x30) * 0x10)) +
                                (tmpBuffer[2 * j + 1] == '*' ? 0x0F : (tmpBuffer[2 * j + 1] - 0x30)));
                    bufferSend[i++] = temp == '*' ? (byte) 0xFF : temp;
                }
            }
            if ((byte) 0xA8 == Objects.requireNonNull(SerialCommand.ACTION_CODE.get(action))[0])
                bufferSend[i++] = Objects.requireNonNull(SerialCommand.ACTION_CODE.get(action))[1];
            else if (SerialCommand.ACTION_TYPE.SET_DELAY == action || SerialCommand.ACTION_TYPE.SHORT_CMD1_SET_DELAY == action
                    || SerialCommand.ACTION_TYPE.SHORT_CMD2_SET_DELAY == action) {
                bufferSend[i++] = (byte) ((timeout & 0xFF00) >> 8);
                bufferSend[i++] = (byte) ((timeout & 0xFF));
            }
            bufferSend[i++] = (byte) ('#');
            bufferSend[i++] = (byte) ('#');
            bufferSend[i++] = (byte) ('#');
        } else {
            if (null == SerialCommand.NEW_ACTION_CODE.get(action)) {
                return true;
            }
            bufferSend = new byte[30];
            bufferSend[i++] = (byte) 0xFE;
            bufferSend[i++] = (byte) 0xFE;
            switch (action) {
                case SET_BAUD:
                    bufferSend[i++] = Objects.requireNonNull(SerialCommand.NEW_ACTION_CODE.get(action));
                    bufferSend[i++] = 1;
                    bufferSend[i++] = 4;
                    break;
                case SET_VOLTAGE:
                case OPEN_BUS:
                    bufferSend[i++] = Objects.requireNonNull(SerialCommand.NEW_ACTION_CODE.get(action));
                    bufferSend[i++] = 1;
                    bufferSend[i++] = (byte) timeout;
                    break;
                case CLOSE_BUS:
                case SELF_TEST:
                case TEST_RESULT:
                case GET_STATUS:
                case SCAN_CODE:
                    bufferSend[i++] = Objects.requireNonNull(SerialCommand.NEW_ACTION_CODE.get(action));
                    bufferSend[i++] = 0;
                    break;
                default:
                    bufferSend[i++] = Objects.requireNonNull(SerialCommand.NEW_ACTION_CODE.get(SerialCommand.ACTION_TYPE.SEND_COMMAND));
                    byte[] data = new byte[20];
                    int k = 0;
                    data[k++] = (byte) 0xFE;
                    data[k++] = (byte) 0xFE;
                    data[k++] = Objects.requireNonNull(SerialCommand.NEW_ACTION_CODE.get(action));
                    data[k++] = 0;
                    if (cmd != null && cmd.length() > 0) {
                        data[3] = 7;
                        byte[] tmpBuffer = cmd.getBytes();
                        byte temp = tmpBuffer[7];  //Character code
                        System.arraycopy(tmpBuffer, 8, tmpBuffer, 7, 5);
                        for (int j = 0; j < 6; j++)
                            data[k++] = tmpBuffer[2 * j] == '*' && tmpBuffer[2 * j + 1] == '*' ? 0 :
                                    (byte) (((tmpBuffer[2 * j] - (tmpBuffer[2 * j] > 0x39 ? 0x37 : 0x30)) * 0x10
                                            + tmpBuffer[2 * j + 1] - (tmpBuffer[2 * j + 1] > 0x39 ? 0x37 : 0x30)) ^ SerialCommand.XOR_DATA);
                        data[k++] = temp == '*' ? 0 : (byte) (temp ^ SerialCommand.XOR_DATA);
                    }
                    if (SerialCommand.ACTION_TYPE.SET_DELAY == action) {
                        data[3] += 2;
                        timeout += ConstantUtils.PRESET_DELAY;
                        data[k++] = (byte) ((timeout & 0xFF) ^ SerialCommand.XOR_DATA);
                        data[k++] = (byte) (((timeout & 0xFF00) >> 8) ^ SerialCommand.XOR_DATA);
                    } else if (SerialCommand.ACTION_TYPE.OPEN_CAPACITOR == action) {
                        data[3] = 2;
                        data[k++] = (byte) ((timeout & 0xFF) ^ SerialCommand.XOR_DATA);
                        data[k++] = (byte) (0xF0 ^ SerialCommand.XOR_DATA);
                    }
                    data[k++] = checkSum(Arrays.copyOfRange(data, 2, k));
                    data[k++] = SerialCommand.DATA_END[0];
                    data[k++] = SerialCommand.DATA_END[1];
                    bufferSend[i++] = (byte) k;
                    System.arraycopy(data, 0, bufferSend, i, k);
                    i += k;
                    break;
            }
            bufferSend[i++] = checkSum(Arrays.copyOfRange(bufferSend, 2, i));
            bufferSend[i++] = SerialCommand.DATA_END[0];
            bufferSend[i++] = SerialCommand.DATA_END[1];
        }
        return sendCmd(Arrays.copyOfRange(bufferSend, 0, i));
    }

    private byte checkSum(byte[] data) {
        byte result = 0;
        for (byte datum : data) result += datum;
        return result;

    }

    public boolean sendCmd(String cmd, int timeout, int number) {
        byte[] bytes;

        bytes = SerialCommand.CMD_COMMON.getBytes();

        byte[] bufferSend = new byte[bytes.length + 20];
        int i;
        for (i = 0; i < bytes.length; i++)
            bufferSend[i] = bytes[i];
        bufferSend[i++] = (byte) 0xFE;
        bufferSend[i++] = (byte) 0xFE;
        bufferSend[i++] = Objects.requireNonNull(SerialCommand.ACTION_CODE.get(SerialCommand.ACTION_TYPE.SET_NUMBER))[0];

        if (cmd != null && cmd.length() > 0) {
            byte[] tmpBuffer = cmd.getBytes();
            byte temp = tmpBuffer[7];  //Character code
            System.arraycopy(tmpBuffer, 8, tmpBuffer, 7, 5);
            for (int j = 0; j < 6; j++)
                bufferSend[i++] = (byte) ((tmpBuffer[2 * j] == '*' ? 0xF0 : ((tmpBuffer[2 * j] - 0x30) * 0x10)) +
                        (tmpBuffer[2 * j + 1] == '*' ? 0x0F : (tmpBuffer[2 * j + 1] - 0x30)));
            bufferSend[i++] = temp == '*' ? (byte) 0xFF : temp;
        }

        bufferSend[i++] = (byte) ((timeout & 0xFF00) >> 8);
        bufferSend[i++] = (byte) ((timeout & 0xFF));

        bufferSend[i++] = (byte) ((number & 0xFF00) >> 8);
        bufferSend[i++] = (byte) ((number & 0xFF));

        bufferSend[i++] = (byte) ('#');
        bufferSend[i++] = (byte) ('#');
        bufferSend[i++] = (byte) ('#');
        return sendCmd(Arrays.copyOfRange(bufferSend, 0, i));
    }

    private boolean writeSn(String sn) {
        byte[] tmpBuffer = sn.getBytes();
        byte[] bytes = SerialCommand.CMD_COMMON.getBytes();
        byte[] bufferSend = new byte[bytes.length + 21];
        int i;
        for (i = 0; i < bytes.length; i++)
            bufferSend[i] = bytes[i];
        bufferSend[i++] = (byte) 0xFE;
        bufferSend[i++] = (byte) 0xFE;
        bufferSend[i++] = Objects.requireNonNull(SerialCommand.ACTION_CODE.get(SerialCommand.ACTION_TYPE.WRITE_SN))[0];

        byte temp = tmpBuffer[7];  //Character code
        System.arraycopy(tmpBuffer, 8, tmpBuffer, 7, 5);
        if (sn.equals("FFFFFFFFFFFFF")) {
            for (int j = 0; j < 7; j++)
                bufferSend[i++] = (byte) 0xFF;
        } else {
            for (int j = 0; j < 6; j++)
                bufferSend[i++] = (byte) (((tmpBuffer[2 * j] - 0x30) * 0x10) + (tmpBuffer[2 * j + 1] - 0x30));
            bufferSend[i++] = temp;
        }

        bufferSend[i++] = (byte) ('#');
        bufferSend[i++] = (byte) ('#');
        bufferSend[i++] = (byte) ('#');
        return sendCmd(Arrays.copyOfRange(bufferSend, 0, i));
    }

    public boolean sendBuffer(byte[] mBuffer) {
        boolean result = true;
        String tail = "\r\n";
        byte[] tailBuffer = tail.getBytes();
        byte[] mBufferTemp = new byte[mBuffer.length + tailBuffer.length];
        System.arraycopy(mBuffer, 0, mBufferTemp, 0, mBuffer.length);
        System.arraycopy(tailBuffer, 0, mBufferTemp, mBuffer.length, tailBuffer.length);

        try {
            if (mOutputStream != null) {
                mOutputStream.write(mBufferTemp);
            } else {
                result = false;
            }
        } catch (IOException e) {
            BaseApplication.writeErrorLog(e);
            result = false;
        }
        return result;
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