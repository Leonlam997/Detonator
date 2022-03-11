package com.leon.detonator.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.R;
import com.leon.detonator.Serial.SerialCommand;
import com.leon.detonator.Serial.SerialPortUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public class SerialTestActivity extends Activity {
    private final int HANDLE_RECEIVE = 1;
    private final int HANDLE_SEND = 2;
    private final int HANDLE_MESSAGE = 3;
    private final boolean newLG = BaseApplication.readSettings().isNewLG();
    private EditText etSendText;
    private EditText etSendText2;
    private EditText etSendText3;
    private TextView tvReceiveText;
    private TextView tvSendText;
    private final Handler rcvHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int offset;
            switch (msg.what) {
                case HANDLE_RECEIVE:
                    tvReceiveText.setText(String.format(Locale.CHINA, "%s\n%s", tvReceiveText.getText(), msg.getData().getString("Message")));
                    offset = tvReceiveText.getLineCount() * tvReceiveText.getLineHeight();
                    if (offset > tvReceiveText.getHeight()) {
                        tvReceiveText.scrollTo(0, offset - tvReceiveText.getHeight());
                    }
                    break;
                case HANDLE_SEND:
                    tvSendText.setText(String.format(Locale.CHINA, "%s\n%s", tvSendText.getText(), msg.getData().getString("Message")));
                    offset = tvSendText.getLineCount() * tvSendText.getLineHeight();
                    if (offset > tvSendText.getHeight()) {
                        tvSendText.scrollTo(0, offset - tvSendText.getHeight());
                    }
                    break;
                default:
                    Toast.makeText(SerialTestActivity.this, msg.getData().getString("Message"), Toast.LENGTH_LONG).show();
                    break;
            }
            return false;
        }
    });
    private BaseApplication myApp;
    private SerialPortUtil serialPortUtil;
    private long timeCounter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_serial_test);

        myApp = (BaseApplication) getApplication();
        etSendText = findViewById(R.id.etSendText);
        etSendText2 = findViewById(R.id.etSend2);
        etSendText3 = findViewById(R.id.etSend3);
        tvReceiveText = findViewById(R.id.textView);
        tvSendText = findViewById(R.id.tvSendText);
        tvReceiveText.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvSendText.setMovementMethod(ScrollingMovementMethod.getInstance());
        timeCounter = System.currentTimeMillis();

        findViewById(R.id.btnClear).setOnClickListener(v -> {
            tvReceiveText.setText("接收数据：");
            tvReceiveText.scrollTo(0, 0);
            tvSendText.setText("发送数据：");
            tvSendText.scrollTo(0, 0);
        });

        try {
            serialPortUtil = SerialPortUtil.getInstance();

            findViewById(R.id.btnSendCmd).setOnClickListener(v -> {
                if (!etSendText2.getText().toString().isEmpty() || etSendText.getText().length() == 13) {
                    if (etSendText3.getText().toString().isEmpty()) {
                        sendMsg(SerialCommand.CMD_COMMON, HANDLE_SEND);
                    } else {
                        if (Integer.parseInt(etSendText3.getText().toString()) == SerialCommand.ACTION_TYPE.EXPLODE.ordinal())
                            sendMsg(SerialCommand.CMD_EXPLODE, HANDLE_SEND);
                        else
                            sendMsg(SerialCommand.CMD_COMMON, HANDLE_SEND);
                    }
                    if (newLG) {
                        myApp.myToast(this, "1");
                        serialPortUtil.sendCmd(etSendText.getText().toString(), SerialCommand.ACTION_TYPE.values()[Integer.parseInt(etSendText2.getText().toString())], etSendText3.getText().toString().isEmpty() ? 0 : Integer.parseInt(etSendText3.getText().toString()));
                    } else if (!serialPortUtil.sendCmd(transferCommand(etSendText.getText().length() == 13 ? etSendText.getText().toString() : "",
                            etSendText2.getText().toString().isEmpty() ? SerialCommand.ACTION_TYPE.NONE : SerialCommand.ACTION_TYPE.values()[Integer.parseInt(etSendText2.getText().toString())],
                            etSendText3.getText().toString().isEmpty() || Integer.parseInt(etSendText2.getText().toString()) != SerialCommand.ACTION_TYPE.SET_DELAY.ordinal() ? 0 : Integer.parseInt(etSendText3.getText().toString())))) {
                        sendMsg("发送失败！", HANDLE_MESSAGE);
                    }
                } else if (!etSendText.getText().toString().isEmpty()) {
                    if (serialPortUtil.sendCmd(etSendText.getText().toString().replace("\n", "\r\n"))) {
                        sendMsg(etSendText.getText().toString(), HANDLE_SEND);
                        //etSendText.setText("");
                    } else {
                        sendMsg("发送失败！", HANDLE_MESSAGE);
                    }
                }
                etSendText.hasFocus();
            });

            findViewById(R.id.btnExitCmd).setOnClickListener(v -> finish());

            findViewById(R.id.btnClock).setOnClickListener(v -> {
                sendMsg(SerialCommand.CMD_COMMON, HANDLE_SEND);
                serialPortUtil.sendCmd(transferCommand("", SerialCommand.ACTION_TYPE.SYNC_CLOCK, 0));
                final Handler sendDelay = new Handler(msg -> {
                    sendMsg(SerialCommand.CMD_ADJUST_CLOCK, HANDLE_SEND);
                    serialPortUtil.sendCmd(SerialCommand.CMD_ADJUST_CLOCK);
                    return false;
                });
                sendDelay.sendEmptyMessageDelayed(1, 800);
            });

            findViewById(R.id.btnHelp).setOnClickListener(v -> new AlertDialog.Builder(SerialTestActivity.this, R.style.AlertDialog)
                    .setTitle("功能码说明")
                    .setPositiveButton(R.string.btn_confirm, null)
                    .setMessage("1:查询SN(AE)\n2:写SN(A1)\n3:设置延期(A3)\n4:读取延期(AA)\n5:起爆(A6)\n6:打开电容(A7)\n13:状态查询(A8)")
                    .create().show());

            findViewById(R.id.btnResult).setOnClickListener(v -> {
                sendMsg(SerialCommand.CMD_BOOST + etSendText3.getText() + "###", HANDLE_SEND);
                serialPortUtil.sendCmd(SerialCommand.CMD_BOOST + etSendText3.getText() + "###");
                //serialPortUtil.sendCmd(transferCommand("", SerialCommand.ACTION_TYPE.ADJUST_RESULT, 0));
            });

            serialPortUtil.setOnDataReceiveListener(buffer -> {
                String received = new String(buffer);
                sendMsg(buffer.length > 4 && (int) buffer[0] >= 0x20 && (int) buffer[1] >= 0x20 ? received : byteToString(Arrays.copyOfRange(buffer, 0, buffer.length), ","), HANDLE_RECEIVE);
                if (received.contains(SerialCommand.INITIAL_FINISHED)) {
                    sendMsg("初始化完成！(" + ((System.currentTimeMillis() - timeCounter) / 1000.0f) + "s)", HANDLE_MESSAGE);
                    sendMsg(SerialCommand.CMD_DEBUG_ON, HANDLE_SEND);
                    if (!serialPortUtil.sendCmd(SerialCommand.CMD_DEBUG_ON)) {
                        sendMsg("发送命令失败！", HANDLE_MESSAGE);
                    }
                }
            });
        } catch (IOException e) {
            sendMsg("模块打开失败！", HANDLE_MESSAGE);
            finish();
        }
    }

    private void sendMsg(String msg, int what) {
        Message m = rcvHandler.obtainMessage(what);
        Bundle b = new Bundle();
        b.putString("Message", msg);
        m.setData(b);
        rcvHandler.sendMessage(m);
    }

    private String byteToString(byte[] buffer, String separator) {
        StringBuilder buf = new StringBuilder();
        for (byte i : buffer)
            buf.append(String.format("%02X", (int) i & 0xFF)).append(separator);
        if (!separator.equals(""))
            buf.deleteCharAt(buf.lastIndexOf(separator));
        return buf.toString();
    }

    private byte[] transferCommand(String cmd, SerialCommand.ACTION_TYPE action, int timeout) {
        byte[] send_cmd;
        switch (action) {
            case WRITE_SN:
                return writeSn(cmd);
            case EXPLODE:
                send_cmd = SerialCommand.CMD_EXPLODE.getBytes();
                break;
            default:
                send_cmd = SerialCommand.CMD_COMMON.getBytes();
                break;
        }

        byte[] Buffer_send = new byte[send_cmd.length + 20];
        int i;
        for (i = 0; i < send_cmd.length; i++)
            Buffer_send[i] = send_cmd[i];
        Buffer_send[i++] = (byte) 0xFE;
        Buffer_send[i++] = (byte) 0xFE;
        if (action == SerialCommand.ACTION_TYPE.INQUIRE_STATUS)
            Buffer_send[i++] = Objects.requireNonNull(SerialCommand.ACTION_CODE.get(action))[0];
        else
            for (Byte code : Objects.requireNonNull(SerialCommand.ACTION_CODE.get(action)))
                Buffer_send[i++] = code;
        if (cmd != null && cmd.length() > 0) {
            byte[] Buffer_Tmp = cmd.getBytes();
            byte temp = Buffer_Tmp[7];  //Character code
            System.arraycopy(Buffer_Tmp, 8, Buffer_Tmp, 7, 5);
            for (int j = 0; j < 6; j++)
                Buffer_send[i++] = (byte) ((Buffer_Tmp[2 * j] == '*' ? 0xF0 : ((Buffer_Tmp[2 * j] - 0x30) * 0x10)) +
                        (Buffer_Tmp[2 * j + 1] == '*' ? 0x0F : (Buffer_Tmp[2 * j + 1] - 0x30)));
            Buffer_send[i++] = temp == '*' ? (byte) 0xFF : temp;
        }
        if (action == SerialCommand.ACTION_TYPE.INQUIRE_STATUS)
            Buffer_send[i++] = Objects.requireNonNull(SerialCommand.ACTION_CODE.get(action))[1];
        else if (action == SerialCommand.ACTION_TYPE.SET_DELAY) {
            Buffer_send[i++] = (byte) ((timeout & 0xFF00) >> 8);
            Buffer_send[i++] = (byte) ((timeout & 0xFF));
        }
        Buffer_send[i++] = (byte) ('#');
        Buffer_send[i++] = (byte) ('#');
        Buffer_send[i++] = (byte) ('#');
        sendMsg(byteToString(Arrays.copyOfRange(Buffer_send, send_cmd.length, i), " "), HANDLE_SEND);
        return Arrays.copyOfRange(Buffer_send, 0, i);
    }

    private byte[] writeSn(String sn) {
        byte[] Buffer_Tmp = sn.getBytes();
        byte[] presend_cmd = SerialCommand.CMD_COMMON.getBytes();
        byte[] Buffer_send = new byte[presend_cmd.length + 21];
        int i;
        for (i = 0; i < presend_cmd.length; i++)
            Buffer_send[i] = presend_cmd[i];
        Buffer_send[i++] = (byte) 0xFE;
        Buffer_send[i++] = (byte) 0xFE;
        Buffer_send[i++] = Objects.requireNonNull(SerialCommand.ACTION_CODE.get(SerialCommand.ACTION_TYPE.WRITE_SN))[0];

        byte temp = Buffer_Tmp[7];  //Character code
        for (int j = 7; j < 12; j++) {
            Buffer_Tmp[j] = Buffer_Tmp[j + 1];
        }
        if (sn.equals("FFFFFFFFFFFFF")) {
            for (int j = 0; j < 7; j++)
                Buffer_send[i++] = (byte) 0xFF;
        } else {
            for (int j = 0; j < 6; j++)
                Buffer_send[i++] = (byte) (((Buffer_Tmp[2 * j] - 0x30) * 0x10) + (Buffer_Tmp[2 * j + 1] - 0x30));
            Buffer_send[i++] = temp;
        }

        Buffer_send[i++] = (byte) ('#');
        Buffer_send[i++] = (byte) ('#');
        Buffer_send[i++] = (byte) ('#');

        byte[] data = Arrays.copyOfRange(Buffer_send, presend_cmd.length, i);
        sendMsg(byteToString(data, " "), HANDLE_SEND);
        return Buffer_send;
    }

    @Override
    public void onDestroy() {
        serialPortUtil.closeSerialPort();
        super.onDestroy();
    }

}
