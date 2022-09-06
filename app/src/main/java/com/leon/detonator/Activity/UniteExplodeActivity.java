package com.leon.detonator.Activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;

import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Base.MyButton;
import com.leon.detonator.Bean.DetonatorInfoBean;
import com.leon.detonator.Bean.LocalSettingBean;
import com.leon.detonator.Dialog.MTModuleDialog;
import com.leon.detonator.R;
import com.leon.detonator.Serial.SerialCommand;
import com.leon.detonator.Serial.SerialPortUtil;
import com.leon.detonator.Util.ConstantUtils;
import com.leon.detonator.Util.FilePath;
import com.leon.detonator.Util.KeyUtils;
import com.minew.modulekit.MTModule;
import com.minew.modulekit.MTModuleManager;
import com.minew.modulekit.interfaces.ScanMTModuleCallback;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class UniteExplodeActivity extends BaseActivity {
    private final int STATUS_SEARCHING = 1;
    private final int STATUS_CONNECTING = 2;
    private final int STATUS_CONNECTING_ELLIPSIS = 3;
    private final int STATUS_UNITE_ELLIPSIS = 4;
    private final int STATUS_CONNECTED = 5;
    private final int STATUS_CONNECT_FAIL = 6;
    private final int STATUS_UNITE_WAITING = 7;
    private final int STATUS_SEARCH_FAIL = 8;
    private final int STATUS_UNITE_FAIL = 9;
    private final int STATUS_UNITE_CONNECTED = 10;
    private final int STATUS_UNITE_CONNECT_FAIL = 11;
    private final int STATUS_HANDSHAKE = 12;
    private final int STATUS_DISCONNECT = 13;
    private final int STATUS_CHARGE_FINISHED = 14;
    private BluetoothAdapter BTAdapter;
    private MTModuleManager mtModuleManager;
    private MTModule mtModule;
    private MTModuleDialog mtDialog;
    private String mtMac, exploderID;
    private MyButton btnReconnect, btnRescan;
    private ImageView ivStatus1, ivStatus2;
    private TextView tvConnect;
    private TextView tvUnite;
    private TextView tvEllipsis1;
    private TextView tvEllipsis2;
    private RelativeLayout rlUnite;
    private int amount;
    private boolean stopScan;
    private boolean enterExplode;
    private boolean charging = false;
    private BaseApplication myApp;
    private final Handler refreshStatus = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message message) {
            int CONNECT_TIMEOUT = 30000;
            switch (message.what) {
                case STATUS_SEARCHING:
                    tvConnect.setText(R.string.unite_searching_devices);
                    btnReconnect.setEnabled(false);
                    tvEllipsis1.setText("");
                    stopScan = false;
                    setProgressVisibility(true);
                    rlUnite.setVisibility(View.INVISIBLE);
                    ivStatus1.setVisibility(View.INVISIBLE);
                    ivStatus2.setVisibility(View.INVISIBLE);
                    refreshStatus.removeMessages(STATUS_CONNECTING_ELLIPSIS);
                    refreshStatus.removeMessages(STATUS_UNITE_ELLIPSIS);
                    refreshStatus.removeMessages(STATUS_SEARCH_FAIL);
                    refreshStatus.removeMessages(STATUS_HANDSHAKE);
                    refreshStatus.removeMessages(STATUS_UNITE_CONNECT_FAIL);
                    refreshStatus.sendEmptyMessage(STATUS_CONNECTING_ELLIPSIS);
                    refreshStatus.sendEmptyMessageDelayed(STATUS_SEARCH_FAIL, CONNECT_TIMEOUT);
                    break;
                case STATUS_SEARCH_FAIL:
                    mtModuleManager.stopScan();
                    setProgressVisibility(false);
                    tvConnect.setText(R.string.unite_device_not_found);
                    tvEllipsis1.setText("");
                    refreshStatus.removeMessages(STATUS_CONNECTING_ELLIPSIS);
                    ivStatus1.setImageResource(R.mipmap.ic_wrong);
                    ivStatus1.setVisibility(View.VISIBLE);
                    btnRescan.setEnabled(true);
                    break;
                case STATUS_CONNECTING:
                    tvConnect.setText(R.string.unite_connecting_device);
                    tvEllipsis1.setText("");
                    refreshStatus.removeMessages(STATUS_SEARCH_FAIL);
                    refreshStatus.removeMessages(STATUS_CONNECTING_ELLIPSIS);
                    refreshStatus.sendEmptyMessage(STATUS_CONNECTING_ELLIPSIS);
                    break;
                case STATUS_CONNECTED:
                    tvConnect.setText(R.string.unite_connected);
                    tvEllipsis1.setText("");
                    refreshStatus.removeMessages(STATUS_SEARCH_FAIL);
                    refreshStatus.removeMessages(STATUS_CONNECTING_ELLIPSIS);
                    ivStatus1.setImageResource(R.mipmap.ic_right);
                    ivStatus1.setVisibility(View.VISIBLE);
                    rlUnite.setVisibility(View.VISIBLE);
                    btnRescan.setEnabled(true);
                    btnReconnect.setEnabled(true);
                    tvUnite.setText(R.string.unite_connecting_host);
                    refreshStatus.sendEmptyMessage(STATUS_UNITE_ELLIPSIS);
                    break;
                case STATUS_CONNECT_FAIL:
                    tvConnect.setText(R.string.unite_connect_fail);
                    tvEllipsis1.setText("");
                    btnRescan.setEnabled(true);
                    setProgressVisibility(false);
                    refreshStatus.removeMessages(STATUS_SEARCH_FAIL);
                    refreshStatus.removeMessages(STATUS_CONNECTING_ELLIPSIS);
                    ivStatus1.setImageResource(R.mipmap.ic_wrong);
                    ivStatus1.setVisibility(View.VISIBLE);
                    break;
                case STATUS_DISCONNECT:
                    tvConnect.setText(R.string.unite_disconnected);
                    tvEllipsis1.setText("");
                    btnRescan.setEnabled(true);
                    btnReconnect.setEnabled(false);
                    rlUnite.setVisibility(View.INVISIBLE);
                    setProgressVisibility(false);
                    refreshStatus.removeMessages(STATUS_CONNECTING_ELLIPSIS);
                    refreshStatus.removeMessages(STATUS_UNITE_ELLIPSIS);
                    refreshStatus.removeMessages(STATUS_SEARCH_FAIL);
                    refreshStatus.removeMessages(STATUS_HANDSHAKE);
                    refreshStatus.removeMessages(STATUS_UNITE_CONNECT_FAIL);
                    refreshStatus.sendEmptyMessage(STATUS_CONNECTING_ELLIPSIS);
                    ivStatus1.setImageResource(R.mipmap.ic_wrong);
                    ivStatus1.setVisibility(View.VISIBLE);
                    break;
                case STATUS_UNITE_WAITING:
                    tvUnite.setText(R.string.unite_waiting);
                    tvEllipsis2.setText("");
                    refreshStatus.removeMessages(STATUS_UNITE_ELLIPSIS);
                    refreshStatus.sendEmptyMessage(STATUS_UNITE_ELLIPSIS);
                    refreshStatus.sendEmptyMessageDelayed(STATUS_UNITE_CONNECT_FAIL, CONNECT_TIMEOUT);
                    break;
                case STATUS_UNITE_CONNECT_FAIL:
                    setProgressVisibility(false);
                    tvUnite.setText(R.string.unite_host_no_answer);
                    tvEllipsis2.setText("");
                    btnRescan.setEnabled(true);
                    ivStatus2.setImageResource(R.mipmap.ic_wrong);
                    ivStatus2.setVisibility(View.VISIBLE);
                    refreshStatus.removeMessages(STATUS_UNITE_ELLIPSIS);
                    refreshStatus.removeMessages(STATUS_HANDSHAKE);
                    break;
                case STATUS_UNITE_CONNECTED:
                    tvUnite.setText(R.string.unite_connected_host);
                    setProgressVisibility(false);
                    btnRescan.setEnabled(true);
                    tvEllipsis2.setText("");
                    refreshStatus.removeMessages(STATUS_UNITE_CONNECT_FAIL);
                    refreshStatus.removeMessages(STATUS_UNITE_ELLIPSIS);
                    refreshStatus.removeMessages(STATUS_HANDSHAKE);
                    ivStatus2.setImageResource(R.mipmap.ic_right);
                    ivStatus2.setVisibility(View.VISIBLE);
                    break;
                case STATUS_UNITE_FAIL:
                    setProgressVisibility(false);
                    tvUnite.setText(R.string.unite_communication_fail);
                    btnRescan.setEnabled(true);
                    tvEllipsis2.setText("");
                    ivStatus2.setImageResource(R.mipmap.ic_wrong);
                    ivStatus2.setVisibility(View.VISIBLE);
                    refreshStatus.removeMessages(STATUS_UNITE_CONNECT_FAIL);
                    refreshStatus.removeMessages(STATUS_UNITE_ELLIPSIS);
                    refreshStatus.removeMessages(STATUS_HANDSHAKE);
                    break;
                case STATUS_CONNECTING_ELLIPSIS:
                    switch (tvEllipsis1.getText().length()) {
                        case 0:
                            tvEllipsis1.setText("。");
                            break;
                        case 1:
                            tvEllipsis1.setText("。。");
                            break;
                        case 2:
                            tvEllipsis1.setText("。。。");
                            break;
                        case 3:
                            tvEllipsis1.setText("。。。。");
                            break;
                        default:
                            tvEllipsis1.setText("");
                            break;
                    }
                    refreshStatus.removeMessages(STATUS_CONNECTING_ELLIPSIS);
                    refreshStatus.sendEmptyMessageDelayed(STATUS_CONNECTING_ELLIPSIS, 500);
                    break;
                case STATUS_UNITE_ELLIPSIS:
                    switch (tvEllipsis2.getText().length()) {
                        case 0:
                            tvEllipsis2.setText("。");
                            break;
                        case 1:
                            tvEllipsis2.setText("。。");
                            break;
                        case 2:
                            tvEllipsis2.setText("。。。");
                            break;
                        case 3:
                            tvEllipsis2.setText("。。。。");
                            break;
                        default:
                            tvEllipsis2.setText("");
                            break;
                    }
                    refreshStatus.removeMessages(STATUS_UNITE_ELLIPSIS);
                    refreshStatus.sendEmptyMessageDelayed(STATUS_UNITE_ELLIPSIS, 500);
                    break;
                case STATUS_HANDSHAKE:
                    refreshStatus.removeMessages(STATUS_HANDSHAKE);
                    mtModule.writeData(checksum(amount + ""), (b, e) -> {
                        if (!b) {
                            refreshStatus.sendEmptyMessage(STATUS_UNITE_FAIL);
                        }
                    });
                    refreshStatus.sendEmptyMessageDelayed(STATUS_HANDSHAKE, ConstantUtils.RESEND_CMD_TIMEOUT);
                    break;
                case STATUS_CHARGE_FINISHED:
                    mtModule.writeData(checksum(SerialCommand.RESPOND_CHARGE_FINISHED), null);
                    refreshStatus.sendEmptyMessageDelayed(STATUS_CHARGE_FINISHED, ConstantUtils.RESEND_CMD_TIMEOUT);
                    break;
            }
            return false;
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unite_explode);

        setTitle(R.string.detonate_cooperate);
        myApp = (BaseApplication) getApplication();
        btnRescan = findViewById(R.id.btn_rescan);
        btnRescan.setEnabled(false);
        btnRescan.setOnClickListener(view -> reconnect(true));
        btnReconnect = findViewById(R.id.btn_reconnect);
        btnReconnect.setEnabled(false);
        btnReconnect.setOnClickListener(view -> reconnect(false));
        ivStatus1 = findViewById(R.id.iv_status1);
        ivStatus2 = findViewById(R.id.iv_status2);
        ivStatus1.setVisibility(View.INVISIBLE);
        ivStatus2.setVisibility(View.INVISIBLE);
        tvConnect = findViewById(R.id.tv_connect);
        tvUnite = findViewById(R.id.tv_unite);
        rlUnite = findViewById(R.id.rl_unite);
        rlUnite.setVisibility(View.INVISIBLE);
        tvEllipsis1 = findViewById(R.id.tv_ellipsis1);
        tvEllipsis2 = findViewById(R.id.tv_ellipsis2);
        enterExplode = false;

        initManager();
        BaseApplication.acquireWakeLock(this);
        LocalSettingBean bean = BaseApplication.readSettings();
        List<DetonatorInfoBean> list = new ArrayList<>();
        myApp.readFromFile(FilePath.FILE_LIST[myApp.isTunnel() ? 0 : 1][ConstantUtils.LIST_TYPE.END.ordinal()],
                list, DetonatorInfoBean.class);
        amount = list.size();
        mtMac = bean.getMtMac();
        exploderID = bean.getExploderID();
        if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(UniteExplodeActivity.this, Manifest.permission.BLUETOOTH)
                && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(UniteExplodeActivity.this, Manifest.permission.BLUETOOTH_ADMIN)) {
            BTAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!BTAdapter.isEnabled()) {
                BTAdapter.enable();
                new Thread() {
                    @Override
                    public void run() {
                        while (!BTAdapter.isEnabled()) {
                            try {
                                Thread.sleep(10);
                            } catch (Exception e) {
                                BaseApplication.writeErrorLog(e);
                            }
                        }
                        if (mtModuleManager != null)
                            mtModuleManager.startScan(scanMTModuleCallback);
                        super.run();
                    }
                }.start();
            } else
                mtModuleManager.startScan(scanMTModuleCallback);
        }
        refreshStatus.sendEmptyMessage(STATUS_SEARCHING);
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (RESULT_OK == result.getResultCode())
            refreshStatus.sendEmptyMessage(STATUS_CHARGE_FINISHED);
        else
            finish();
    });

    private void initManager() {
        mtModuleManager = MTModuleManager.getInstance(this);

        MTModuleManager.getInstance(this).setModuleChangeConnection((device, status) -> {
            if (stopScan) {
                switch (status) {
                    case DeviceLinkStatus_Connected:
                        refreshStatus.sendEmptyMessage(STATUS_CONNECTED);
                        mtMac = mtModule.getMacAddress();
                        LocalSettingBean bean = BaseApplication.readSettings();
                        bean.setMtMac(mtMac);
                        myApp.saveSettings(bean);
                        mtModule.writeData("LGE".getBytes(), null);
                        refreshStatus.sendEmptyMessage(STATUS_UNITE_WAITING);
                        refreshStatus.sendEmptyMessageDelayed(STATUS_HANDSHAKE, 500);
                        mtModule.setMTModuleListener(bytes -> {
                            String rec = new String(bytes);
                            myApp.myToast(UniteExplodeActivity.this, rec);
                            if (rec.contains((null == exploderID || exploderID.isEmpty() ? mtMac : exploderID))) {
                                if (charging) {
                                    refreshStatus.removeMessages(STATUS_CHARGE_FINISHED);
                                    charging = false;
                                } else {
                                    refreshStatus.removeMessages(STATUS_HANDSHAKE);
                                    refreshStatus.sendEmptyMessage(STATUS_UNITE_CONNECTED);
                                }
                            } else if (rec.contains(SerialCommand.RESPOND_EXPLODE)) {
                                Intent intent = new Intent(UniteExplodeActivity.this, DetonateStep4Activity.class);
                                intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LAT, 0));
                                intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LNG, 0));
                                intent.putExtra(KeyUtils.KEY_EXPLODE_UNITE, true);
                                startActivity(intent);
                                enterExplode = true;
                                finish();
                            } else if (rec.contains(SerialCommand.RESPOND_CHARGE) && !charging) {
                                Intent intent = new Intent(UniteExplodeActivity.this, DetonateStep3Activity.class);
                                intent.putExtra(KeyUtils.KEY_EXPLODE_VOLTAGE, getIntent().getFloatExtra(KeyUtils.KEY_EXPLODE_VOLTAGE, 21));
                                intent.putExtra(KeyUtils.KEY_BYPASS_EXPLODE, getIntent().getBooleanExtra(KeyUtils.KEY_BYPASS_EXPLODE, false));
                                intent.putExtra(KeyUtils.KEY_EXPLODE_ONLINE, getIntent().getBooleanExtra(KeyUtils.KEY_EXPLODE_ONLINE, false));
                                intent.putExtra(KeyUtils.KEY_EXPLODE_LAT, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LAT, 0));
                                intent.putExtra(KeyUtils.KEY_EXPLODE_LNG, getIntent().getDoubleExtra(KeyUtils.KEY_EXPLODE_LNG, 0));
                                intent.putExtra(KeyUtils.KEY_EXPLODE_UNITE, true);
                                launcher.launch(intent);
                                charging = true;
                            }
                        });
                        break;
                    case DeviceLinkStatus_ConnectFailed:
                        refreshStatus.sendEmptyMessage(STATUS_CONNECT_FAIL);
                        break;
                    case DeviceLinkStatus_Disconnect:
                        refreshStatus.sendEmptyMessage(STATUS_DISCONNECT);
                        break;
                }
            }
        });

        MTModuleManager.getInstance(this).checkBluetoothState();
//        switch (bluetoothState) {
//            case BluetoothStateNotSupported:
//                break;
//            case BluetoothStatePowerOff:
//                break;
//            case BluetoothStatePowerOn:
////                mtModuleManager.startScan(scanMTModuleCallback);
//                break;
//        }
    }

    ScanMTModuleCallback scanMTModuleCallback = new ScanMTModuleCallback() {
        @Override
        public void onScannedMTModule(LinkedList<MTModule> linkedList) {
            if (!stopScan) {
                refreshStatus.removeMessages(STATUS_SEARCH_FAIL);

                if (null == mtDialog || !mtDialog.isShowing()) {
                    mtDialog = new MTModuleDialog(UniteExplodeActivity.this, linkedList, mtMac);
                    mtDialog.setCanceledOnTouchOutside(false);
                    mtDialog.setListener1(view -> {
                        mtModuleManager.stopScan();
                        mtDialog.dismiss();
                        stopScan = true;
                        refreshStatus.sendEmptyMessage(STATUS_CONNECTING);
                        mtModule = mtDialog.getSelectedMTModule();
                        mtModuleManager.connect(mtModule);
                    });
                    mtDialog.setListener2(view -> {
                        mtModuleManager.stopScan();
                        mtDialog.dismiss();
                        refreshStatus.sendEmptyMessage(STATUS_SEARCH_FAIL);
                    });
                } else {
                    mtDialog.setList(linkedList);
                }
                mtDialog.show();
            }
        }
    };

    private byte[] checksum(String s) {
        String data = (null == exploderID || exploderID.isEmpty() ? mtMac : exploderID) + "," + s + ",";
        int checksum = 0;
        for (int i = 0; i < data.length(); i++)
            checksum += data.charAt(i);
        String send = String.format("%02X", checksum);
        return (data + send.substring(send.length() - 2)).getBytes();
    }

    private void reconnect(boolean scan) {
        refreshStatus.removeCallbacksAndMessages(null);
        if (scan) {
            if (null != mtModule) {
                mtModuleManager.disconnect(mtModule);
            }
            mtModuleManager.reStartScan(scanMTModuleCallback);
            refreshStatus.sendEmptyMessage(STATUS_SEARCHING);
        } else {
            ivStatus2.setVisibility(View.INVISIBLE);
            refreshStatus.sendEmptyMessage(STATUS_UNITE_WAITING);
            refreshStatus.sendEmptyMessage(STATUS_HANDSHAKE);
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mtDialog != null && !mtDialog.isShowing()) {
            if (btnRescan.isEnabled() && KeyEvent.KEYCODE_1 == keyCode) {
                reconnect(true);
            } else if (btnReconnect.isEnabled() && KeyEvent.KEYCODE_2 == keyCode) {
                reconnect(false);
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initManager();
    }

    @Override
    protected void onStart() {
        super.onStart();
        initManager();
    }

    @Override
    protected void onDestroy() {
        BaseApplication.releaseWakeLock(UniteExplodeActivity.this);
        if (!enterExplode) {
            try {
                SerialPortUtil.getInstance().closeSerialPort();
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
        if (PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(UniteExplodeActivity.this, Manifest.permission.BLUETOOTH)
                && PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(UniteExplodeActivity.this, Manifest.permission.BLUETOOTH_ADMIN)) {
            if (BTAdapter.isEnabled())
                BTAdapter.disable();
        }
        if (null != mtModule)
            mtModuleManager.disconnect(mtModule);
        stopScan = true;
        mtModuleManager.clearAllOperation();
        mtModuleManager = null;
        refreshStatus.removeCallbacksAndMessages(null);
        super.onDestroy();
    }


}
