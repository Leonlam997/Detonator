package com.leon.detonator.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.kfree.Log;
import com.kfree.expd.ExpdDevMgr;
import com.leon.detonator.R;
import com.leon.detonator.base.BaseActivity;
import com.serialport.PSAMAPI;

public class PsamDemoActivity extends BaseActivity {
    private static final String TAG = "PsamDemoActivity";

    //声明库函数
    final int TRUE = 1;
    final int FALSE = 0;

    public PSAMAPI mPSAMAPI = new PSAMAPI();

    Button btnOpenCom;
    Button btnCloseCom;
    Button btnPowerUp;
    Button btnPowerDown;
    Button btnPSAMRst;
    Button btnPSAMCOS;
    Button btnClear;
    Button btnGetVer1;

    EditText etComNo;
    EditText etBaud;
    EditText etSAMNo1;
    EditText etCosInput1;
    EditText etResult1;

    TextView tvInfo;
    TextView tvResult;
    static int nfd = 0;
    static boolean nPowerSta = false;
    static int nIsOpenCom = 0;
    int nStatus = 0;

    private ExpdDevMgr mExpDevMgr;

    private int mCardSpeed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_psam_demo);
        setTitle(R.string.hide_serial_test);

        initDlg();   //后面补充内容，先空着
        //globalVar.setFd(1);
        //globalVar.setIsComOpen(1);

        mExpDevMgr = new ExpdDevMgr(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.add("00对应9600卡");
        adapter.add("01对应38400卡");
        adapter.add("02对应115200卡");
        Spinner spinner = findViewById(R.id.spinner_card_speed);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                Spinner spinner = (Spinner) adapterView;
                String itemContent = (String) adapterView.getItemAtPosition(position);  // 上面都是字符,这里取得也是字符
                Log.d(TAG, "id:" + position + " Txt:" + itemContent);
                mCardSpeed = position;
            }

            public void onNothingSelected(AdapterView<?> view) {
                Log.i(TAG, view.getClass().getName());
            }
        });
    }

    public void rfInitComFun() {
        //nfd=mPSAMAPI.rfInitCom("/dev/ttyS2", 19200);
        String dev = "/dev/" + etComNo.getText().toString().replace(" ", "");
        nfd = mPSAMAPI.rfInitCom(dev, Integer.parseInt(etBaud.getText().toString().replace(" ", "")));
        if (nfd > 0) {
            nIsOpenCom = 1;
            tvInfo.setText(R.string.open_com_OK);
        } else {
            nIsOpenCom = 0;
            tvInfo.setText(R.string.open_com_Err);
        }
    }

    public void rfClosePortFun() {
        if (nIsOpenCom == 0) {
            tvInfo.setText(R.string.btIsComOpen);
            return;
        }
        nStatus = mPSAMAPI.rfClosePort(nfd);
        if (nStatus == TRUE) {
            tvInfo.setText(R.string.close_com_OK);
        } else {
            tvInfo.setText(R.string.close_com_Err);
        }
    }

    public void rfPowerUpFun() {
//        nPowerSta=mPSAMAPI.rfPower("/proc/devicepower/rfidpower", 1);
//        if(nPowerSta>0)
//        {
//            tvInfo.setText("打开电源成功");
//        }
//        else
//        {
//            tvInfo.setText("打开电源失败");
//        }
        mExpDevMgr.setPsamReaderPw(true);
        nPowerSta = mExpDevMgr.isPsamReaderPwOn();
        if (nPowerSta) {
            tvInfo.setText("打开电源成功");
        } else {
            tvInfo.setText("打开电源失败");
        }
    }

    public void rfPowerDownFun() {
//        nPowerSta=mPSAMAPI.rfPower("/proc/devicepower/rfidpower", 0);
//        if(nPowerSta>0)
//        {
//            tvInfo.setText("关闭电源成功");
//        }
//        else
//        {
//            tvInfo.setText("关闭电源失败");
//        }

        mExpDevMgr.setPsamReaderPw(false);
        nPowerSta = mExpDevMgr.isPsamReaderPwOn();
        if (!nPowerSta) {
            tvInfo.setText("关闭电源成功");
        } else {
            tvInfo.setText("关闭电源失败");
        }
    }

    public void rfSAMRstFun() {
        if (nIsOpenCom == 0) {
            tvInfo.setText(R.string.btIsComOpen);
            return;
        }
        byte[] pPara = new byte[100];
        byte[] pData = new byte[100];
        byte[] pMsgLg = new byte[1];

        byte[] cSAMNo = StringToByteArray(etSAMNo1.getText().toString());
        /**
         *cSAMNo[0] 表示的是卡槽的索引, 0 对应卡槽1
         * 00为默认的9600的卡
         * 01对应38400的卡
         * 02对应115200的卡
         */
        //pPara[0]=(byte) (0x00 + cSAMNo[0]* 16);
        pPara[0] = (byte) (mCardSpeed + cSAMNo[0] * 16);
        etResult1.setText("");
        nStatus = mPSAMAPI.rfSamRst(nfd, pPara, (byte) (1), pData, pMsgLg);
        if (nStatus == TRUE) {
            tvInfo.setText(R.string.RfReset_OK);
            etResult1.setText(ByteArrayToString(pData, pMsgLg[0]));
        } else {
            tvInfo.setText(R.string.RfReset_Err);
        }
    }

    public void rfCosCommandFun() {
        if (nIsOpenCom == 0) {
            tvInfo.setText(R.string.btIsComOpen);
            return;
        }
        byte[] pData = new byte[300];
        int[] pMsgLg = new int[1];
        byte[] command = StringToByteArray(etSAMNo1.getText().toString() + etCosInput1.getText().toString());
        nStatus = mPSAMAPI.rfSamCos(nfd, command, (byte) (command.length), pData, pMsgLg);
        if (nStatus == TRUE) {
            tvInfo.setText(R.string.RfCosSend_OK);
            etResult1.setText(ByteArrayToString(pData, pMsgLg[0]));
        } else {
            tvInfo.setText(R.string.RfCosSend_Err);
        }
    }

    public void rfLibVerFun() {
        if (nIsOpenCom == 0) {
            tvInfo.setText(R.string.btIsComOpen);
            return;
        }
        byte[] Ver = new byte[4];
        nStatus = mPSAMAPI.rfLibVer(nfd, Ver);
        if (nStatus == TRUE) {
            tvInfo.setText(R.string.RfGetVer_OK);
            etResult1.setText(ByteArrayToString(Ver, 4));
        } else {
            tvInfo.setText(R.string.RfGetVer_Err);
        }
    }

    View.OnClickListener onClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnOpenCom:
                    rfInitComFun();
                    break;
                case R.id.btnCloseCom:
                    rfClosePortFun();
                    break;
                case R.id.btnPowerUp:
                    rfPowerUpFun();
                    break;
                case R.id.btnPowerDown:
                    rfPowerDownFun();
                    break;
                case R.id.btnReset:
                    rfSAMRstFun();
                    break;
                case R.id.btnSendCos:
                    rfCosCommandFun();
                    break;

                case R.id.btnGetVer:
                    rfLibVerFun();
                    break;

				/*
			case R.id.btnOpenCom:

				break;*/
            }
        }

    };

    public void initDlg() {
        initButton();
        initTextView();
        initEditText();
    }

    public void initTextView() {
        tvInfo = findViewById(R.id.TextViewInfo);
        tvResult = findViewById(R.id.tvResult);
    }

    public void initEditText() {
        etComNo = findViewById(R.id.etComNo);
        etBaud = findViewById(R.id.etBaud);
        etSAMNo1 = findViewById(R.id.etSAMNo);
        etCosInput1 = findViewById(R.id.etCosInput);
        etResult1 = findViewById(R.id.etResult);
    }

    public void initButton() {
        btnOpenCom = findViewById(R.id.btnOpenCom);
        btnOpenCom.setOnClickListener(onClick);

        btnCloseCom = findViewById(R.id.btnCloseCom);
        btnCloseCom.setOnClickListener(onClick);

        btnPowerUp = findViewById(R.id.btnPowerUp);
        btnPowerUp.setOnClickListener(onClick);

        btnPowerDown = findViewById(R.id.btnPowerDown);
        btnPowerDown.setOnClickListener(onClick);

        btnPSAMRst = findViewById(R.id.btnReset);
        btnPSAMRst.setOnClickListener(onClick);

        btnPSAMCOS = findViewById(R.id.btnSendCos);
        btnPSAMCOS.setOnClickListener(onClick);

        btnGetVer1 = findViewById(R.id.btnGetVer);
        btnGetVer1.setOnClickListener(onClick);


    }

    public String ByteArrayToString(byte[] bt_ary, int len) {
        StringBuilder sb = new StringBuilder();
        int i;
        if (bt_ary != null)
            if (len < bt_ary.length) {
                for (i = 0; i < len; i++) {
                    sb.append(String.format("%02X ", bt_ary[i]));
                }
            } else {
                for (byte b : bt_ary) {
                    sb.append(String.format("%02X ", b));
                }
            }
        return sb.toString();
    }

    public byte[] StringToByteArray(String str) {
        str = str.replaceAll(" ", "");
        int n = str.length() / 2;
        String[] str_ary = new String[n];
        for (int i = 0; i < n; i++) {
            str_ary[i] = str.substring(i * 2, i * 2 + 2);
        }
        byte[] bt_ary = new byte[n];
        for (int i = 0; i < n; i++)
            bt_ary[i] = (byte) Integer.parseInt(str_ary[i], 16);
        return bt_ary;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mExpDevMgr.deInit();
    }
}