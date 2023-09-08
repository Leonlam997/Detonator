package com.leon.detonator.activity;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.ListView;

import androidx.core.app.ActivityCompat;

import com.leon.detonator.R;
import com.leon.detonator.adapter.DeviceListAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.bluetooth.BluetoothBean;
import com.leon.detonator.bluetooth.BluetoothService;
import com.leon.detonator.bluetooth.BluetoothUtil;
import com.leon.detonator.bluetooth.PairBluetoothListener;
import com.leon.detonator.bluetooth.SearchBluetoothListener;
import com.leon.detonator.component.MyButton;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.dialog.MyProgressDialog;
import com.leon.detonator.util.CRC16;
import com.leon.detonator.util.ConstantUtils;
import com.leon.detonator.util.KeyUtils;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SendSchemeActivity extends BaseActivity {
    private List<DetonatorBean> detonatorBeanList;
    private List<BluetoothBean> list;
    private BluetoothAdapter BTAdapter;
    private BluetoothService btService;
    private BluetoothUtil bluetoothUtil;
    private MyProgressDialog pDialog;
    private DeviceListAdapter adapter;
    private SchemeBean currentScheme;
    private MyButton btnScan;
    private MyButton btnSend;
    private ListView listView;

    private final Handler myHandler = new Handler(msg -> {
        switch (msg.what) {
            case ConstantUtils.BT_CONNECTED:
                myApp.myToast(SendSchemeActivity.this, String.format(Locale.getDefault(), getString(R.string.bt_connected_device), msg.obj));
                sendData();
                break;
            case ConstantUtils.BT_DATA:
                if (msg.arg1 > 0) {
                    String data = new String(Arrays.copyOfRange((byte[]) msg.obj, 0, msg.arg1));
                    switch (data) {
                        case ConstantUtils.BT_SUCCESS_ACK:
                            btnSend.setEnabled(true);
                            setProgressVisibility(false);
                            msg.getTarget().removeMessages(ConstantUtils.BT_ERROR);
                            myApp.myToast(SendSchemeActivity.this, R.string.message_send_success);
                            if (null != pDialog && pDialog.isShowing()) {
                                pDialog.dismiss();
                            }
                            btService.cancelAllBtThread();
                            btService.acceptWait();
                            break;
                        case ConstantUtils.BT_RESEND_ACK:
                            sendData();
                            break;
                    }
                }
                break;
            case ConstantUtils.BT_ERROR:
                msg.getTarget().removeMessages(ConstantUtils.BT_ERROR);
                btnSend.setEnabled(true);
                myApp.myToast(SendSchemeActivity.this, (String) msg.obj);
                if (null != pDialog && pDialog.isShowing())
                    pDialog.dismiss();
                btService.cancelAllBtThread();
                btService.acceptWait();
                break;
        }
        return false;
    });

    private void sendData() {
        myApp.setBtSender(true);
        try {
            pDialog.setMessage(getString(R.string.progress_sending));
            StringBuilder sb = new StringBuilder();
            sb.append(currentScheme.getName()).append("\n").append(BaseApplication.settings.isTunnel()).append("\n");
            JSONArray jsonArray = new JSONArray();
            for (DetonatorBean bean : detonatorBeanList) {
                jsonArray.put(bean.toJSON());
            }
            sb.append(jsonArray);
            sb.insert(0, CRC16.getTableCRC(sb.toString().getBytes()));
            btService.write(sb.toString().getBytes());
            myHandler.removeMessages(ConstantUtils.BT_DATA);
            myHandler.removeMessages(ConstantUtils.BT_ERROR);
            myHandler.sendMessageDelayed(myHandler.obtainMessage(ConstantUtils.BT_ERROR, getString(R.string.bt_send_timeout)), (sb.length() / 1024 + 1) * 100);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_scheme);
        myApp = (BaseApplication) getApplication();
        setProgressVisibility(true);
        long id = getIntent().getLongExtra(KeyUtils.KEY_TABLE_ID, -1);
        if (id == -1) {
            finish();
            return;
        }
        currentScheme = DbUtil.getScheme(id);
        if (currentScheme.getId() == -1) {
            finish();
            return;
        }
        detonatorBeanList = DbUtil.getDetonatorList(currentScheme.getId());
        setTitle(String.format(getString(R.string.title_send_scheme), currentScheme.getName()));
        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        btService = new BluetoothService(myHandler);
        bluetoothUtil = new BluetoothUtil();
        btnScan = findViewById(R.id.btn_scan);
        btnScan.setEnabled(false);
        list = new ArrayList<>();
        adapter = new DeviceListAdapter(this, list);
        listView = findViewById(R.id.lv_devices);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(((adapterView, view, i, l) -> {
            list.get(i).setSelected(true);
            for (BluetoothBean bean : list)
                if (!bean.equals(list.get(i)) && bean.isSelected())
                    bean.setSelected(false);
            adapter.updateList(list);
        }));
        btnSend = findViewById(R.id.btn_send);
        btnSend.setOnClickListener(view -> launchFunction(0));
        btnScan.setOnClickListener(view -> launchFunction(1));
        if (!BTAdapter.isEnabled()) {
            ActivityCompat.checkSelfPermission(this, "android.permission.BLUETOOTH_CONNECT");
            BTAdapter.enable();
            new Thread(() -> {
                while (!BTAdapter.isEnabled()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        BaseApplication.writeErrorLog(e);
                    }
                }
                btService.acceptWait();
                startSearch();
            }).start();
        } else {
            btService.acceptWait();
            new Thread(this::startSearch).start();
        }
    }

    private void startSearch() {
        try {
            bluetoothUtil.searchBluetooth(this, new SearchBluetoothListener() {
                @Override
                public void startSearch() {
                }

                @Override
                public void foundDevice(BluetoothBean bluetooth) {
                    if (!list.contains(bluetooth)) {
                        list.add(bluetooth);
                        adapter.updateList(list);
                    }
                }

                @Override
                public void finishSearch(Map<String, List<BluetoothBean>> blueToothMap) {
                    runOnUiThread(() -> {
                        setProgressVisibility(false);
                        btnScan.setEnabled(true);
                    });
                }
            });
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void selectItem(int pos) {
        for (BluetoothBean bean : list)
            bean.setSelected(false);
        list.get(pos).setSelected(true);
        adapter.updateList(list);
        listView.setSelection(pos);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        launchFunction(keyCode);
        return super.onKeyUp(keyCode, event);
    }

    private void launchFunction(int i) {
        switch (i) {
            case KeyEvent.KEYCODE_1:
            case 0:
                if (btnSend.isEnabled()) {
                    for (BluetoothBean bean : list)
                        if (bean.isSelected()) {
                            if (!btnScan.isEnabled()) {
                                ActivityCompat.checkSelfPermission(this, "android.permission.BLUETOOTH_SCAN");
                                if (BTAdapter.isDiscovering())
                                    BTAdapter.cancelDiscovery();
                                btnScan.setEnabled(true);
                            }
                            pDialog = new MyProgressDialog(SendSchemeActivity.this);
                            pDialog.setInverseBackgroundForced(false);
                            pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                            pDialog.setCancelable(false);
                            pDialog.setTitle(R.string.progress_title);
                            pDialog.setMessage(getString(R.string.progress_connecting));
                            pDialog.show();
                            myHandler.sendMessageDelayed(myHandler.obtainMessage(ConstantUtils.BT_ERROR, getString(R.string.bt_connect_timeout)), 5000);
                            btnSend.setEnabled(false);
                            if (bean.isConnected()) {
                                BluetoothDevice device = BTAdapter.getRemoteDevice(bean.getAddress());
                                btService.connect(device);
                            } else {
                                try {
                                    bluetoothUtil.makePair(SendSchemeActivity.this, bean.getAddress(), new PairBluetoothListener() {
                                        @Override
                                        public void whilePair(BluetoothDevice device) {

                                        }

                                        @Override
                                        public void pairingSuccess(BluetoothDevice device) {
                                            btService.connect(device);
                                        }

                                        @Override
                                        public void cancelPair(BluetoothDevice device) {
                                            runOnUiThread(() -> {
                                                myHandler.removeMessages(ConstantUtils.BT_ERROR);
                                                btnSend.setEnabled(true);
                                                if (null != pDialog && pDialog.isShowing())
                                                    pDialog.dismiss();
                                            });
                                        }
                                    });
                                } catch (Exception e) {
                                    BaseApplication.writeErrorLog(e);
                                }
                            }
                            return;
                        }
                    myApp.myToast(SendSchemeActivity.this, R.string.message_select_device);
                }
                break;
            case KeyEvent.KEYCODE_3:
            case 1:
                if (btnScan.isEnabled()) {
                    btnScan.setEnabled(false);
                    list.clear();
                    adapter.updateList(list);
                    new Thread(this::startSearch).start();
                }
                break;
            case KeyEvent.KEYCODE_2:
                listView.requestFocus();
                if (listView.getSelectedItemPosition() > 0)
                    selectItem(listView.getSelectedItemPosition() - 1);
                else
                    selectItem(list.size() - 1);
                break;
            case KeyEvent.KEYCODE_8:
                listView.requestFocus();
                if (listView.getSelectedItemPosition() < list.size() - 1)
                    selectItem(listView.getSelectedItemPosition() + 1);
                else
                    selectItem(0);
                break;

        }
    }

    @Override
    protected void onDestroy() {
        ActivityCompat.checkSelfPermission(this, "android.permission.BLUETOOTH_SCAN");
        if (BTAdapter.isDiscovering())
            BTAdapter.cancelDiscovery();
        btService.cancelAllBtThread();
        bluetoothUtil.closeBluetoothService();
        myApp.setBtSender(false);
        super.onDestroy();
    }
}