package com.leon.detonator.Serial;

import com.leon.detonator.Base.BaseApplication;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SerialCommand {
    public final static String CMD_COMMON = "bypass,000000,lg,";
    public final static String CMD_BOOST = "bypass,000000,dac,";
    public final static String CMD_EXPLODE = "bypass,000000,ex,";
    public final static String CMD_READ_VOLTAGE = "bypass,000000,adc,0###";
    public final static String CMD_READ_CURRENT = "bypass,000000,adc,1###";
    public final static String CMD_DEBUG_ON = "bypass,000000,debug,1###";
    public final static String CMD_ADJUST_CLOCK = "bypass,000000,pluse,1000###";
    public final static String CMD_OPEN_BUS = " AT+BUSON=%d\r\n";
    public final static String CMD_INITIAL_VOLTAGE = " AT+DIRON=%d,%d\r\n";
    public final static String CMD_WORK_VOLTAGE = " AT+BWKV=%.1f\r\n";
    public final static String CMD_SIGNAL_WAVEFORM = " AT+RECMD=%d\r\n";
    public final static String CMD_MODE = " ++++mode:%d\r\n";
    public final static String CMD_SAVE_SETTINGS = " AT+SVBUS\r\n";
    public final static String AT_CMD_RESPOND = "OK";
    public final static String SCAN_RESPOND = "AR:";
    public final static String CMD_BUS_STATUS = "+BUS:";
    public final static String BUS_STATUS_ERR = "Err";
    public final static String INITIAL_FINISHED = "start!!";//"0xef4017";
    public final static String INITIAL_FAIL = "end!!";
    public final static String RESPOND_FAIL = "Fail!";
    public final static String RESPOND_SUCCESS = "success\r\n";
    public final static String RESPOND_VOLTAGE = "votage = ";
    public final static String RESPOND_CURRENT = "Im:";
    public final static String RESPOND_EXPLODE = "Explode!";
    public final static String RESPOND_CHARGE = "Charge!";
    public final static String RESPOND_CHARGE_FINISHED = "Done!";
    public final static String ALERT_SHORT_CIRCUIT = "Short!";
    public final static Map<ACTION_TYPE, Byte[]> ACTION_CODE = new HashMap<ACTION_TYPE, Byte[]>() {
        {
            put(ACTION_TYPE.READ_SN, new Byte[]{(byte) 0xAE});
            put(ACTION_TYPE.WRITE_SN, new Byte[]{(byte) 0xA1});
            put(ACTION_TYPE.SET_DELAY, new Byte[]{(byte) 0xA3});
            put(ACTION_TYPE.GET_DELAY, new Byte[]{(byte) 0xAA});
            put(ACTION_TYPE.EXPLODE, new Byte[]{(byte) 0xA6});
            put(ACTION_TYPE.OPEN_CAPACITOR, new Byte[]{(byte) 0xA7});
            put(ACTION_TYPE.ADJUST_CLOCK, new Byte[]{(byte) 0xA9});
            put(ACTION_TYPE.ADJUST_RESULT, new Byte[]{(byte) 0xAF});
            put(ACTION_TYPE.SELF_TEST, new Byte[]{(byte) 0xAC});
            put(ACTION_TYPE.READ_TEST_TIMES, new Byte[]{(byte) 0xAD});
            put(ACTION_TYPE.INSTANT_OPEN_CAPACITOR, new Byte[]{(byte) 0xA4, (byte) 0x01});
            put(ACTION_TYPE.SYNC_CLOCK, new Byte[]{(byte) 0xB5, (byte) 0x01});
            put(ACTION_TYPE.NEW_EXPLODE, new Byte[]{(byte) 0xB7, (byte) 0x02});
            put(ACTION_TYPE.INQUIRE_STATUS, new Byte[]{(byte) 0xA8, (byte) 0x07});
            put(ACTION_TYPE.CHECK_CAPACITOR, new Byte[]{(byte) 0xA8, (byte) 0x08});
            put(ACTION_TYPE.SET_NUMBER, new Byte[]{(byte) 0xB8});
            put(ACTION_TYPE.GET_NUMBER, new Byte[]{(byte) 0xA8, (byte) 0x0A});
            put(ACTION_TYPE.FAST_DETECT, new Byte[]{(byte) 0xAB});
            put(ACTION_TYPE.SHORT_CMD1_GET_DELAY, new Byte[]{(byte) 0xBA});
            put(ACTION_TYPE.SHORT_CMD2_GET_DELAY, new Byte[]{(byte) 0xBB});
            put(ACTION_TYPE.SHORT_CMD1_SET_DELAY, new Byte[]{(byte) 0xB8});
            put(ACTION_TYPE.SHORT_CMD2_SET_DELAY, new Byte[]{(byte) 0xB9});
        }
    };
    public final static byte XOR_DATA = (byte) 0xAA;
    public final static byte[] DATA_END = {(byte) 0x16, (byte) 0x73};
    public final static String STRING_DATA_END = String.format("%02X%02X", DATA_END[0], DATA_END[1]);
    public final static Map<ACTION_TYPE, Byte> NEW_ACTION_CODE = new HashMap<ACTION_TYPE, Byte>() {
        {
            put(ACTION_TYPE.WRITE_SN, (byte) 0xA1);
            put(ACTION_TYPE.WRITE_PASSWORD, (byte) 0xA2);
            put(ACTION_TYPE.READ_SN, (byte) 0xA4);
            put(ACTION_TYPE.READ_PASSWORD, (byte) 0xA5);
            put(ACTION_TYPE.SET_DELAY, (byte) 0xAF);
            put(ACTION_TYPE.GET_DELAY, (byte) 0xB0);
            put(ACTION_TYPE.OPEN_CAPACITOR, (byte) 0xAC);
            put(ACTION_TYPE.NEW_EXPLODE, (byte) 0xAE);
            put(ACTION_TYPE.SELF_TEST, (byte) 0xAD);
            put(ACTION_TYPE.READ_TEST_TIMES, (byte) 0xB1);
            put(ACTION_TYPE.SHORT_CMD1_SET_DELAY, (byte) 0xA6);
            put(ACTION_TYPE.SHORT_CMD2_SET_DELAY, (byte) 0xA7);
            put(ACTION_TYPE.SHORT_CMD1_GET_DELAY, (byte) 0xA8);
            put(ACTION_TYPE.SHORT_CMD2_GET_DELAY, (byte) 0xA9);
        }
    };
    public final static String RESPOND_CONNECTED = "AA";
    public final static String DATA_PREFIX = "F0";
    private final static boolean newLG = BaseApplication.readSettings().isNewLG();
    public final static String CMD_SCAN = newLG ? " AT+BAR=3\r\n" : "bypass,000000,bar###";
    private final static String RESPOND_COMMON = "55";
    public final static Map<ACTION_TYPE, String> RESPOND_CONFIRM = new HashMap<ACTION_TYPE, String>() {
        {
            put(ACTION_TYPE.READ_SN, RESPOND_COMMON + String.format("%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.READ_SN))[0]));
            put(ACTION_TYPE.WRITE_SN, RESPOND_COMMON + String.format("%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.WRITE_SN))[0]));
            put(ACTION_TYPE.SET_DELAY, RESPOND_COMMON + String.format("%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.SET_DELAY))[0]));
            put(ACTION_TYPE.GET_DELAY, RESPOND_COMMON + String.format("%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.GET_DELAY))[0]));
            put(ACTION_TYPE.ADJUST_CLOCK, RESPOND_COMMON + String.format("%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.GET_DELAY))[0]));
            put(ACTION_TYPE.SELF_TEST, RESPOND_COMMON + String.format("%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.SELF_TEST))[0]));
            put(ACTION_TYPE.READ_TEST_TIMES, RESPOND_COMMON + String.format("%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.READ_TEST_TIMES))[0]));
            put(ACTION_TYPE.SYNC_CLOCK, RESPOND_COMMON + String.format("%02X%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.SYNC_CLOCK))[0], Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.SYNC_CLOCK))[1]));
            put(ACTION_TYPE.INQUIRE_STATUS, RESPOND_COMMON + String.format("%02X%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.INQUIRE_STATUS))[0], Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.INQUIRE_STATUS))[1]));
            put(ACTION_TYPE.CHECK_CAPACITOR, RESPOND_COMMON + String.format("%02X%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.CHECK_CAPACITOR))[0], Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.CHECK_CAPACITOR))[1]));
            put(ACTION_TYPE.SET_NUMBER, RESPOND_COMMON + String.format("%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.SET_NUMBER))[0]));
            put(ACTION_TYPE.GET_NUMBER, RESPOND_COMMON + String.format("%02X%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.GET_NUMBER))[0], Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.GET_NUMBER))[1]));
            put(ACTION_TYPE.FAST_DETECT, RESPOND_COMMON + String.format("%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.FAST_DETECT))[0]));
            put(ACTION_TYPE.SHORT_CMD1_GET_DELAY, RESPOND_COMMON + String.format("%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.SHORT_CMD1_GET_DELAY))[0]));
            put(ACTION_TYPE.SHORT_CMD2_GET_DELAY, RESPOND_COMMON + String.format("%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.SHORT_CMD2_GET_DELAY))[0]));
            put(ACTION_TYPE.SHORT_CMD1_SET_DELAY, RESPOND_COMMON + String.format("%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.SHORT_CMD1_SET_DELAY))[0]));
            put(ACTION_TYPE.SHORT_CMD2_SET_DELAY, RESPOND_COMMON + String.format("%02X", Objects.requireNonNull(ACTION_CODE.get(ACTION_TYPE.SHORT_CMD2_SET_DELAY))[0]));
        }
    };
    private final static byte RESPOND_AND_BYTE = (byte) 0x80;
    public final static Map<ACTION_TYPE, String> NEW_RESPOND_CONFIRM = new HashMap<ACTION_TYPE, String>() {
        {
            put(ACTION_TYPE.WRITE_SN, String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.WRITE_SN)) - RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.WRITE_PASSWORD, String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.WRITE_PASSWORD)) - RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.READ_SN, String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.READ_SN)) - RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.READ_PASSWORD, String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.READ_PASSWORD)) - RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.GET_DELAY, String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.GET_DELAY)) - RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.READ_TEST_TIMES, String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.READ_TEST_TIMES)) - RESPOND_AND_BYTE) & 0xff)));
        }
    };

    public enum ACTION_TYPE {
        NONE,
        READ_SN,
        WRITE_SN,
        SET_DELAY,
        GET_DELAY,
        EXPLODE,
        OPEN_CAPACITOR,
        DETECT_ALL,
        SELF_TEST,
        READ_TEST_TIMES,
        ADJUST_CLOCK,
        ADJUST_RESULT,
        SYNC_CLOCK,
        NEW_EXPLODE,
        INQUIRE_STATUS,
        CHECK_CAPACITOR,
        SET_NUMBER,
        GET_NUMBER,
        FAST_DETECT,
        INSTANT_OPEN_CAPACITOR,
        WRITE_PASSWORD,
        READ_PASSWORD,
        SHORT_CMD1_GET_DELAY,
        SHORT_CMD2_GET_DELAY,
        SHORT_CMD1_SET_DELAY,
        SHORT_CMD2_SET_DELAY
    }
}
