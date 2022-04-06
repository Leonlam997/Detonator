package com.leon.detonator.Serial;

import com.leon.detonator.Base.BaseApplication;
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
    private final boolean newLG = BaseApplication.readSettings().isNewLG();
    private boolean test = true;
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

    public boolean sendCmd(String cmd, SerialCommand.ACTION_TYPE action, int... timeout) {
        byte[] bufferSend;
        if (newLG) {
            switch (action) {
                case READ_SN:
                case READ_TEST_TIMES:
                    bufferSend = new byte[4];
                    break;
                case GET_DELAY:
                case SELF_TEST:
                case NEW_EXPLODE:
                    bufferSend = new byte[5];
                    break;
                case SET_DELAY:
                    bufferSend = new byte[6];
                    break;
                case OPEN_CAPACITOR:
                    bufferSend = new byte[8];
                    break;
                case WRITE_SN:
                    bufferSend = new byte[11];
                    break;
                case SHORT_CMD1_SET_DELAY:
                case SHORT_CMD2_SET_DELAY:
                    bufferSend = new byte[(action == SerialCommand.ACTION_TYPE.SHORT_CMD1_SET_DELAY ? 11 : 7) + 2 * timeout.length];
                    break;
                default:
                    return false;
            }
            bufferSend[0] = (byte) Integer.parseInt(SerialCommand.DATA_PREFIX, 16);
            bufferSend[1] = (byte) bufferSend.length;
            bufferSend[2] = Objects.requireNonNull(SerialCommand.NEW_ACTION_CODE.get(action));
            switch (action) {
                case GET_DELAY:
                    bufferSend[3] = (byte) 0x73;
                    break;
                case SELF_TEST:
                    bufferSend[3] = (byte) 0xC6;
                    break;
                case NEW_EXPLODE:
                    bufferSend[3] = (byte) 0xEA;
                    break;
                case SET_DELAY:
                    bufferSend[3] = (byte) ((timeout[0] & 0xFF00) >> 8);
                    bufferSend[4] = (byte) (timeout[0] & 0xFF);
                    break;
                case WRITE_SN: {
                    byte[] tmpBuffer = cmd.getBytes();
                    byte temp = tmpBuffer[7];  //Character code
                    System.arraycopy(tmpBuffer, 8, tmpBuffer, 7, 5);
                    int n = 3;
                    for (int j = 0; j < 6; j++) {
                        if (2 == j)
                            bufferSend[n++] = temp;
                        bufferSend[n++] = (byte) (((tmpBuffer[2 * j] - 0x30) * 0x10) + (tmpBuffer[2 * j + 1] - 0x30));
                    }
                    break;
                }
                case OPEN_CAPACITOR:
                    //启动充电AC----------------------------------------------
                    int k, tmp, total, div, stepn, pwmn;
                    float cnt, curr, res, cap, volt, pwmt, pulsediv, fx, fy, fn, fct, ft95;

                    total = 0;     //总充电脉冲数
                    fct = 0;     //总有效充电时间数
                    pwmn = 0;

                    cnt = timeout[0];  //带载数量
                    curr = 50;  //最大充电电流
                    res = 11;   //雷管限流电阻11K
                    cap = 100;  //储能电容100uF
                    volt = timeout[1] / 10f;    //总线电压
                    pwmt = 250; //PWM周期250ms

                    //充电到95%电压需要的时间
                    //Vt = V * (1-exp(-t/RC))     1-exp = 0.95  exp = 0.05
                    fx = (float) Math.log(0.05);
                    //-t/RC = fx; t = -RC * fx
                    fy = res * cap;
                    fx = fx * fy;
                    ft95 = fx * (-1.0f);

                    //计算初始占空比
                    fx = volt / res;
                    fy = curr / fx;
                    pulsediv = cnt / fy;
                    fx = pulsediv + 0.5f;
                    tmp = (int) fx;               //第一空比
                    pulsediv = tmp;
                    if (tmp <= 4) {
                        div = 4;
                        stepn = 5;
                        //ft95 = fn * pwmt/4;
                        fy = pwmt / 4.0f;
                        fn = ft95 / fy;
                        fn = fn + 0.5f;
                        //fn 充电周期数
                        pwmn = (int) fn;
                    } else {
                        //计算多少周期后,占空比可以减去1.0
//                    Vt = V * (1-exp(-t/RC))                     (1)
//                    cnt / curr/((V-Vt)/res) = pulsediv - 1.0    (2)
//                    (1)(2)共同成立解出 t
//                    t = fn * pwmt / pulsediv                    (3)
//                    解出 fn
                        div = tmp;
                        stepn = 0;
                        for (int i = 0; i < 100; i++) {
                            //第一步 由（2）式解出 Vt  在上一步的div下需要多少脉冲能够让 div -1
                            fy = pulsediv - 1.0f;
                            fy = cnt / fy;           //在这个占空比下 等效的雷管数量
                            fy = curr / fy;          //平均到一个雷管下的电流
                            fy = fy * res;           //限流电阻上的压差
                            fy = volt - fy;          //充电电容上的电压 Vt
                            fy = fy / volt;          //fy = 1-exp;
                            fy = 1.0f - fy;           //exp(fx) = 1 - fy;  取对数
                            fx = (float) Math.log(fy);             //fx = -t/RC
                            fy = res * cap;          //RC单位ms  t = -fx * RC
                            fx = fx * fy;
                            fx = -1.0f * fx;          //fx为总充电时间
                            fy = fx - fct;           //减去以前的总充电时间,则为在本div下的充电时间
                            fct = fx;                //总充电时间修改
                            //计算本次充电时间需要的pwm周期数 fy = fn * pwmt/div
                            fx = pwmt / pulsediv;
                            fn = fy / fx;            //本div下充电的脉冲数量
                            fn = fn + 0.5f;
                            fx = pulsediv + 0.5f;
                            k = (int) fx;         //当前占空比
//                            str = inttostr(k) + ' 充电脉冲数=';
                            tmp = (int) fn;
                            total = total + tmp;
                            stepn = stepn + 1;
                            //新的占空比--------------
                            if (k == 4) {             //如果目前空比=4则计算需要在4状态下充电多少脉冲 才可到95%
                                fy = ft95 - fct;     //fy = fn * pwmt/div
                                fx = pwmt / 4.0f;
                                fn = fy / fx;
                                fn = fn + 0.5f;
                                fx = total;
                                fy = stepn;
                                fx = fx / fy;
                                fx = fx + 0.5f;
                                stepn = (int) fx;
                                tmp = (int) fn;
                                total = total + tmp;
                                pwmn = total;
                                break;
                            }
                            pulsediv = pulsediv - 1.0f;
                            if (pulsediv < 4.0)
                                pulsediv = 4.0f;
                        }
                    }
                    //F0 08 AC div step cntH cntL xor
                    //1  2  3  4   5    6    7    8
                    bufferSend[3] = timeout[0] < 20 ? 1 : (byte) (div & 0xFF);
                    bufferSend[4] = (byte) (stepn & 0xFF);
                    bufferSend[5] = (byte) ((pwmn & 0xFF00) >> 8);
                    bufferSend[6] = (byte) (pwmn & 0xFF);
                    break;
                case SHORT_CMD2_SET_DELAY:
                case SHORT_CMD1_SET_DELAY:
                    byte[] tmpBuffer = cmd.getBytes();
                    byte temp = tmpBuffer[7];  //Character code
                    System.arraycopy(tmpBuffer, 8, tmpBuffer, 7, 5);
                    int n = 3;
                    for (int j = (action == SerialCommand.ACTION_TYPE.SHORT_CMD1_SET_DELAY ? 0 : 3); j < 6; j++) {
                        if (2 == j)
                            bufferSend[n++] = temp;
                        bufferSend[n++] = (byte) (((tmpBuffer[2 * j] - 0x30) * 0x10) + (tmpBuffer[2 * j + 1] - 0x30));
                    }
                    for (int i : timeout) {
                        bufferSend[n++] = (byte) ((i & 0xFF00) >> 8);
                        bufferSend[n++] = (byte) (i & 0xFF);
                    }
                    break;
            }
            bufferSend[bufferSend.length - 1] = bufferSend[0];
            for (int i = 1; i < bufferSend.length - 1; i++) {
                bufferSend[bufferSend.length - 1] ^= bufferSend[i];
            }
            return sendCmd(bufferSend);
        } else {
            byte[] bytes;
            int i = 0;
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
                bufferSend[i++] = (byte) ((timeout[0] & 0xFF00) >> 8);
                bufferSend[i++] = (byte) ((timeout[0] & 0xFF));
            }
            bufferSend[i++] = (byte) ('#');
            bufferSend[i++] = (byte) ('#');
            bufferSend[i++] = (byte) ('#');
            return sendCmd(Arrays.copyOfRange(bufferSend, 0, i));
        }
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

    public void setTest(boolean test) {
        this.test = test;
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

    public boolean checkData(String data) {
        int cs = Integer.parseInt(data.substring(0, 2), 16);
        for (int i = 2; i < data.length() - 2; i += 2) {
            cs ^= Integer.parseInt(data.substring(i, i + 2), 16);
        }
        return Integer.parseInt(data.substring(data.length() - 2), 16) == cs;
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