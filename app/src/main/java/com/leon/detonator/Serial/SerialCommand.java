package com.leon.detonator.Serial;

import com.leon.detonator.Base.BaseApplication;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SerialCommand {
    public final static String CMD_COMMON = "bypass,000000,lg,";
    public final static String CMD_BOOST = "bypass,000000,dac,";
    public final static String CMD_EXPLODE = "bypass,000000,ex,";
    public final static String CMD_SCAN = "bypass,000000,bar###";
    public final static String CMD_READ_VOLTAGE = "bypass,000000,adc,0###";
    public final static String CMD_READ_CURRENT = "bypass,000000,adc,1###";
    public final static String CMD_DEBUG_ON = "bypass,000000,debug,1###";
    public final static String CMD_ADJUST_CLOCK = "bypass,000000,pluse,1000###";
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
            put(ACTION_TYPE.WRITE_SN, (byte) 0x10);
            put(ACTION_TYPE.WRITE_UUID, (byte) 0x11);
            put(ACTION_TYPE.WRITE_PASSWORD, (byte) 0x12);
            put(ACTION_TYPE.SET_DELAY, (byte) 0x13);
            put(ACTION_TYPE.READ_SN, (byte) 0x20);
            put(ACTION_TYPE.READ_UUID, (byte) 0x21);
            put(ACTION_TYPE.READ_PASSWORD, (byte) 0x22);
            put(ACTION_TYPE.GET_DELAY, (byte) 0x23);
            put(ACTION_TYPE.OPEN_CAPACITOR, (byte) 0x50);
            put(ACTION_TYPE.RELEASE_CAPACITOR, (byte) 0x51);
            put(ACTION_TYPE.NEW_EXPLODE, (byte) 0x55);
            put(ACTION_TYPE.COMM_TEST, (byte) 0x70);
            put(ACTION_TYPE.READ_CAPACITOR_VOLTAGE, (byte) 0x73);
            put(ACTION_TYPE.ADJUST_CLOCK, (byte) 0x74);
            put(ACTION_TYPE.ADJUST_RESULT, (byte) 0x75);
            put(ACTION_TYPE.SET_BAUD, (byte) 0x10);
            put(ACTION_TYPE.SET_VOLTAGE, (byte) 0x11);
            put(ACTION_TYPE.SEND_COMMAND, (byte) 0x12);
            put(ACTION_TYPE.OPEN_BUS, (byte) 0x20);
            put(ACTION_TYPE.CLOSE_BUS, (byte) 0x21);
            put(ACTION_TYPE.GET_STATUS, (byte) 0x22);
            put(ACTION_TYPE.SCAN_CODE, (byte) 0x30);
            put(ACTION_TYPE.SELF_TEST, (byte) 0x31);
            put(ACTION_TYPE.TEST_RESULT, (byte) 0x32);
            put(ACTION_TYPE.FAST_TEST, (byte) 0x76);
        }
    };
    private final static boolean newLG = BaseApplication.readSettings().isNewLG();
    public final static String RESPOND_CONNECTED = newLG ? "FEAA00AA" : "AA";
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
    private final static String NEW_RESPOND_COMMON = "FE";
    private final static byte RESPOND_AND_BYTE = (byte) 0x80;
    public final static Map<ACTION_TYPE, String> NEW_RESPOND_CONFIRM = new HashMap<ACTION_TYPE, String>() {
        {
            put(ACTION_TYPE.WRITE_SN, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.WRITE_SN)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.WRITE_UUID, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.WRITE_UUID)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.WRITE_PASSWORD, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.WRITE_PASSWORD)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.READ_SN, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.READ_SN)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.READ_UUID, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.READ_UUID)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.READ_PASSWORD, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.READ_PASSWORD)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.SET_DELAY, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.SET_DELAY)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.GET_DELAY, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.GET_DELAY)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.OPEN_CAPACITOR, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.OPEN_CAPACITOR)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.RELEASE_CAPACITOR, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.RELEASE_CAPACITOR)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.NEW_EXPLODE, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.NEW_EXPLODE)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.COMM_TEST, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.COMM_TEST)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.SELF_TEST, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.SELF_TEST)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.READ_CAPACITOR_VOLTAGE, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.READ_CAPACITOR_VOLTAGE)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.ADJUST_CLOCK, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.ADJUST_CLOCK)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.ADJUST_RESULT, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.ADJUST_RESULT)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.SET_BAUD, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.SET_BAUD)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.SET_VOLTAGE, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.SET_VOLTAGE)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.SEND_COMMAND, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.SEND_COMMAND)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.OPEN_BUS, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.OPEN_BUS)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.CLOSE_BUS, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.CLOSE_BUS)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.GET_STATUS, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.GET_STATUS)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.SCAN_CODE, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.SCAN_CODE)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.TEST_RESULT, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.TEST_RESULT)) + RESPOND_AND_BYTE) & 0xff)));
            put(ACTION_TYPE.FAST_TEST, NEW_RESPOND_COMMON + String.format("%02X", ((Objects.requireNonNull(NEW_ACTION_CODE.get(ACTION_TYPE.FAST_TEST)) + RESPOND_AND_BYTE) & 0xff)));
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
        WRITE_UUID,
        READ_UUID,
        WRITE_PASSWORD,
        READ_PASSWORD,
        RELEASE_CAPACITOR,
        COMM_TEST,
        READ_CAPACITOR_VOLTAGE,
        SET_BAUD,
        SET_VOLTAGE,
        OPEN_BUS,
        CLOSE_BUS,
        SEND_COMMAND,
        SCAN_CODE,
        TEST_RESULT,
        GET_STATUS,
        FAST_TEST,
        SHORT_CMD1_GET_DELAY,
        SHORT_CMD2_GET_DELAY,
        SHORT_CMD1_SET_DELAY,
        SHORT_CMD2_SET_DELAY
    }
}
