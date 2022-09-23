package com.leon.detonator.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputFilter;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.leon.detonator.adapter.BluetoothListAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.BluetoothListBean;
import com.leon.detonator.bean.DetonatorInfoBean;
import com.leon.detonator.bluetooth.BluetoothBean;
import com.leon.detonator.bluetooth.BluetoothService;
import com.leon.detonator.bluetooth.BluetoothUtil;
import com.leon.detonator.bluetooth.PairBluetoothListener;
import com.leon.detonator.bluetooth.SearchBluetoothListener;
import com.leon.detonator.dialog.MyProgressDialog;
import com.leon.detonator.R;
import com.leon.detonator.util.CRC16;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BluetoothActivity extends BaseActivity {
    public final static int STATUS_CONNECTED = 1,
            STATUS_DATA = 2,
            STATUS_RECEIVE_FINISH = 3,
            STATUS_ERROR = 4;
    private List<BluetoothListBean> list;
    private BluetoothListAdapter adapter;
    private BluetoothAdapter BTAdapter;
    private BluetoothUtil bluetoothUtil;
    private BluetoothService btService;
    private AlertDialog alertDialog;
    private BaseApplication myApp;
    private PopupWindow popupMenu;
    private MyProgressDialog pDialog;
    private boolean searching, sender = false;
    private int rescanLine, lastTouchX, clickIndex;
    private StringBuilder receiveData;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            finish();
        }
        setTitle(R.string.settings_bt);
        myApp = (BaseApplication) getApplication();
        BTAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothUtil = new BluetoothUtil();
        searching = false;
        receiveData = new StringBuilder();
        initData(false);
        final ListView lvBT = findViewById(R.id.lv_bt);
        adapter = new BluetoothListAdapter(this, list);
        adapter.setOnButtonClickListener(which -> {
            switch (which) {
                case 0:
                    if (searching) {
                        searching = false;
                        BTAdapter.cancelDiscovery();
                    }
                    BTAdapter.disable();
                    sendMsg(BtStatus.DISABLING);
                    new DetectBluetoothStatus(false).start();
                    break;
                case 1:
                    BTAdapter.enable();
                    sendMsg(BtStatus.ENABLING);
                    new DetectBluetoothStatus(true).start();
                    break;
                case 2:
                    initData(false);
                    startSearch();
                    break;
            }
        });

        lvBT.setAdapter(adapter);
        lvBT.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = (int) event.getX();
                    break;
                case MotionEvent.ACTION_UP:
                    lvBT.performClick();
                    break;
            }
            return false;
        });
        lvBT.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 1) {
                final View v = LayoutInflater.from(BluetoothActivity.this).inflate(R.layout.layout_edit_dialog, parent, false);
                final EditText etDelay = v.findViewById(R.id.et_dialog);
                final TextView tvDelay = v.findViewById(R.id.tv_dialog);
                etDelay.setHint(R.string.hint_input_name);
                etDelay.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
                etDelay.setInputType(InputType.TYPE_CLASS_TEXT);
                tvDelay.setVisibility(View.GONE);

                new AlertDialog.Builder(BluetoothActivity.this, R.style.AlertDialog)
                        .setTitle(R.string.dialog_title_edit_name)
                        .setView(v)
                        .setPositiveButton(R.string.btn_confirm, (dialog, which) -> {
                            if (!etDelay.getText().toString().isEmpty()) {
                                BTAdapter.setName(etDelay.getText().toString());
                                sendMsg(BtStatus.RENAME);
                            }
                        })
                        .setNegativeButton(R.string.btn_cancel, null)
                        .create().show();
            } else {
                if (searching) {
                    searching = false;
                    BTAdapter.cancelDiscovery();
                }
                if (position > rescanLine) {
                    BluetoothListBean bean = list.get(position);
                    bean.setScanning(true);
                    list.set(position, bean);
                    sendMsg(BtStatus.PAIRING);
                    pairDevice(bean.getBluetooth().getAddress());
                }
                if (position < rescanLine && position > 2) {
                    showPopupWindow(parent, view, position);
                }
            }
        });

        btService = new BluetoothService(connectHandler, BluetoothActivity.this);
        if (BTAdapter.isEnabled()) {
            startSearch();
            btService.acceptWait();
        }
    }

    private Handler connectHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (ActivityCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            switch (msg.what) {
                case STATUS_CONNECTED:
                    if (sender)
                        sendData();
                    myApp.myToast(BluetoothActivity.this,
                            String.format(Locale.CHINA, getResources().getString(R.string.bt_connected_device), msg.obj));
                    break;
                case STATUS_DATA:
                    if (!sender) {
                        if (null == connectHandler) {
                            myApp.myToast(BluetoothActivity.this, R.string.message_receive_data_please_into_bt);
                            break;
                        }
                        if (msg.arg1 > 0) {
                            connectHandler.removeMessages(STATUS_RECEIVE_FINISH);
                            if (searching) {
                                searching = false;
                                BTAdapter.cancelDiscovery();
                            }
                            receiveData.append(new String(Arrays.copyOfRange((byte[]) msg.obj, 0, msg.arg1)));
                            connectHandler.sendEmptyMessageDelayed(STATUS_RECEIVE_FINISH, 100);
                        }
                    } else {
                        if (msg.arg1 > 0) {
                            String data = new String(Arrays.copyOfRange((byte[]) msg.obj, 0, msg.arg1));
                            switch (data) {
                                case "Success":
                                    connectHandler.removeMessages(STATUS_ERROR);
                                    myApp.myToast(BluetoothActivity.this, R.string.message_send_success);
                                    sender = false;
                                    if (null != pDialog && pDialog.isShowing()) {
                                        pDialog.dismiss();
                                    }
                                    btService.cancelAllBtThread();
                                    btService.acceptWait();
                                    break;
                                case "Resend":
                                    if (null != pDialog && pDialog.isShowing())
                                        sendData();
                                    break;
                            }
                        }
                    }
                    break;
                case STATUS_RECEIVE_FINISH:
                    final String data = receiveData.toString();
                    receiveData = new StringBuilder();
//                    myApp.myToast(BluetoothActivity.this, data.substring(0, 4) + "," + CRC16.getTableCRC(data.substring(4).getBytes())
//                            + "," + data.length());
//                    BaseApplication.writeFile(data);

                    if (!data.startsWith(CRC16.getTableCRC(data.substring(4).getBytes()))) {
                        btService.write("Resend".getBytes());
                        //myApp.myToast(BluetoothActivity.this, "数据包错误！");
                    } else {
                        btService.write("Success".getBytes());
                        btService.cancelAllBtThread();
                        btService.acceptWait();
                        myApp.myToast(BluetoothActivity.this, R.string.message_receive_success);
                        List<DetonatorInfoBean> beanList = new ArrayList<>();
                        myApp.readFromFile(myApp.getListFile(), beanList, DetonatorInfoBean.class);
                        if (beanList.size() > 0) {
                            if (null != alertDialog && alertDialog.isShowing())
                                alertDialog.dismiss();
                            alertDialog = new AlertDialog.Builder(BluetoothActivity.this, R.style.AlertDialog)
                                    .setTitle(R.string.progress_title)
                                    .setMessage(R.string.dialog_cover_list)
                                    .setCancelable(false)
                                    .setNegativeButton(R.string.btn_cancel, null)
                                    .setNeutralButton(R.string.btn_append, (d, which) -> {
                                        try {
                                            JSONArray jsonArray = new JSONArray(data.substring(4));
                                            for (int i = 0; i < jsonArray.length(); i++) {
                                                DetonatorInfoBean bean = new DetonatorInfoBean();
                                                bean.fromJSON(jsonArray.getJSONObject(i));
                                                beanList.add(bean);
                                            }
                                            jsonArray = new JSONArray();
                                            for (DetonatorInfoBean bean : beanList) {
                                                jsonArray.put(bean.toJSON());
                                            }
                                            saveData("0000" + jsonArray.toString());
                                        } catch (Exception e1) {
                                            BaseApplication.writeErrorLog(e1);
                                        }
                                    })
                                    .setPositiveButton(R.string.btn_cover, (d, which) -> saveData(data))
                                    .create();
                            alertDialog.show();
                        } else {
                            saveData(data);
                        }
                    }
                    break;
                case STATUS_ERROR:
                    connectHandler.removeMessages(STATUS_ERROR);
                    myApp.myToast(BluetoothActivity.this, (String) msg.obj);
                    if (null != pDialog && pDialog.isShowing())
                        pDialog.dismiss();
                    btService.cancelAllBtThread();
                    btService.acceptWait();
                    break;
            }
            return false;
        }
    });

    private void sendData() {
        try {
            File file = new File(myApp.getListFile());
            if (file.exists()) {
                FileReader fr = new FileReader(file);
                BufferedReader br = new BufferedReader(fr);
                String tempString;
                StringBuilder sb = new StringBuilder();
                while ((tempString = br.readLine()) != null) {
                    sb.append(tempString);
                }
                br.close();
                fr.close();
                sb.insert(0, CRC16.getTableCRC(sb.toString().getBytes()));
                btService.write(sb.toString().getBytes());
                connectHandler.removeMessages(STATUS_DATA);
            }
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private final Handler refreshList = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            if (ActivityCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            BtStatus status = BtStatus.values()[msg.what];
            BluetoothListBean bean;
            switch (status) {
                case ENABLING://打开蓝牙
                    initData(false);
                    bean = list.get(0);
                    bean.setChangingStatus(true);
                    bean.setEnabled(true);
                    list.set(0, bean);
                    break;
                case DISABLING://关闭蓝牙
                    if (null != btService) {
                        btService.cancelAllBtThread();
                        btService = null;
                    }
                    initData(false);
                    bean = list.get(0);
                    bean.setChangingStatus(true);
                    bean.setEnabled(false);
                    list.set(0, bean);
                    break;
                case ENABLED:
                    btService = new BluetoothService(connectHandler, BluetoothActivity.this);
                    btService.acceptWait();
                    searching = true;
                    initData(true);
                    startSearch();
                    break;
                case DISABLED:
                    initData(false);
                    bean = list.get(0);
                    bean.setChangingStatus(false);
                    list.set(0, bean);
                    break;
                case SEARCHING:
                    if (rescanLine > 0 && list.size() > rescanLine) {
                        bean = list.get(rescanLine);
                        bean.setScanning(true);
                        list.set(rescanLine, bean);
                    }
                    break;
                case FINISHED:
                    if (rescanLine > 0 && list.size() > rescanLine) {
                        bean = list.get(rescanLine);
                        bean.setScanning(false);
                        list.set(rescanLine, bean);
                    }
                    break;
                case RENAME:
                    bean = list.get(1);
                    BluetoothBean bt = bean.getBluetooth();
                    bt.setAddress(BTAdapter.getName());
                    bean.setBluetooth(bt);
                    list.set(1, bean);
                    break;
                case NOT_PAIR:
                    myApp.myToast(BluetoothActivity.this, R.string.bt_pair_fail);
                case PAIRED:
                    initData(false);
                    startSearch();
                    break;
                default:
                    break;
            }
            adapter.updateList(list);
            return false;
        }
    });

    private void saveData(String data) {
        final File file = new File(myApp.getListFile());
        if (file.exists() && !file.delete()) {
            myApp.myToast(BluetoothActivity.this, R.string.message_delete_fail);
            return;
        }
        try {
            PrintWriter out = new PrintWriter(file);
            out.write(data.substring(4));
            out.flush();
            out.close();
            myApp.myToast(BluetoothActivity.this, R.string.message_save_list_success);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void showPopupWindow(AdapterView<?> parent, View view, int position) {
        final String[] menu;
        menu = new String[]{getResources().getString(R.string.menu_send_list),
                getResources().getString(R.string.menu_cancel_pair)};
        View popupView = BluetoothActivity.this.getLayoutInflater().inflate(R.layout.layout_popupwindow, parent, false);
        popupView.findViewById(R.id.tvTitle).setVisibility(View.GONE);
        clickIndex = position;
        ListView lsvMenu = popupView.findViewById(R.id.lvPopupMenu);
        lsvMenu.setAdapter(new ArrayAdapter<>(BluetoothActivity.this, R.layout.layout_popupwindow_menu, menu));
        lsvMenu.setOnItemClickListener((parent1, view1, position1, id) -> launchMenu(position1));
        lsvMenu.setOnKeyListener((v, keyCode, event) -> {
            launchMenu(keyCode - KeyEvent.KEYCODE_1);
            return false;
        });
        popupMenu = new PopupWindow(popupView, 150, 40 * menu.length);
        popupMenu.setAnimationStyle(R.style.popup_window_anim);
        popupMenu.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupMenu.setFocusable(true);
        popupMenu.setOutsideTouchable(true);
        popupMenu.update();
        popupMenu.showAsDropDown(view, lastTouchX > 75 ? lastTouchX - 75 : 0, 0);
    }

    private void launchMenu(int position) {
        switch (position) {
            case 0:
                List<DetonatorInfoBean> beanList = new ArrayList<>();
                myApp.readFromFile(myApp.getListFile(), beanList, DetonatorInfoBean.class);
                if (beanList.size() <= 0) {
                    myApp.myToast(BluetoothActivity.this, R.string.message_list_not_found);
                } else {
                    if (null != btService) {
                        BluetoothDevice device = BTAdapter.getRemoteDevice(list.get(clickIndex).getBluetooth().getAddress());
                        btService.connect(device);
                        sender = true;
                        pDialog = new MyProgressDialog(BluetoothActivity.this);
                        pDialog.setInverseBackgroundForced(false);
                        pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                        pDialog.setCancelable(false);
                        pDialog.setTitle(R.string.progress_title);
                        pDialog.setMessage(getResources().getString(R.string.progress_connecting));
                        pDialog.show();
                        connectHandler.sendMessageDelayed(connectHandler.obtainMessage(STATUS_ERROR, getResources().getString(R.string.bt_connect_timeout)), 10000);
                    }
                }
                break;
            case 1:
                try {
                    bluetoothUtil.unPair(list.get(clickIndex).getBluetooth().getAddress());
                    initData(false);
                    startSearch();
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                }
                break;
        }
        popupMenu.dismiss();
    }

    private void sendMsg(BtStatus status) {
        refreshList.sendMessage(refreshList.obtainMessage(status.ordinal()));
    }

    private void initData(boolean discoverable) {
        if (list == null) {
            list = new ArrayList<>();
        } else {
            list.clear();
        }
        BluetoothListBean bean = new BluetoothListBean();
        BluetoothBean bt = new BluetoothBean();
        bt.setName(getResources().getString(R.string.settings_bt));
        bean.setBluetooth(bt);
        bean.setEnabled(BTAdapter.isEnabled());
        list.add(bean);
        rescanLine = -1;
        if (ActivityCompat.checkSelfPermission(BluetoothActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bean = new BluetoothListBean();
        bt = new BluetoothBean();
        bt.setName(getResources().getString(R.string.bt_device_name));
        bt.setAddress(BTAdapter.getName());
        bean.setBluetooth(bt);
        list.add(bean);
        if (BTAdapter.isEnabled()) {
            bean = new BluetoothListBean();
            bt = new BluetoothBean();
            bt.setName(getResources().getString(R.string.bt_paired_device));
            bean.setBluetooth(bt);
            bean.setRescanLine(true);
            list.add(bean);
            bean = new BluetoothListBean();
            bt = new BluetoothBean();
            bt.setName(getResources().getString(R.string.bt_available_device));
            bean.setBluetooth(bt);
            bean.setRescanLine(true);
            if (discoverable) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                startActivity(discoverableIntent);
            }
            if (searching)
                bean.setScanning(true);
            list.add(bean);
            rescanLine = 3;
        }
    }

    private void startSearch() {
        try {
            bluetoothUtil.searchBluetooth(this, new SearchBluetoothListener() {
                @Override
                public void startSearch() {
                    sendMsg(BtStatus.SEARCHING);
                    searching = true;
                }

                @Override
                public void foundDevice(BluetoothBean bluetooth, boolean newDevice) {
                    BluetoothListBean bean = new BluetoothListBean();
                    bean.setBluetooth(bluetooth);

                    if (newDevice) {
                        list.add(bean);
                    } else {
                        boolean exist = false;
                        for (int i = 3; i <= rescanLine; i++) {
                            if (null != list.get(i).getBluetooth().getAddress() && list.get(i).getBluetooth().getAddress().equals(bluetooth.getAddress())) {
                                list.get(i).setBluetooth(bluetooth);
                                exist = true;
                                break;
                            }
                        }

                        if (!exist) {
                            list.add(rescanLine, bean);
                            rescanLine++;
                        }
                    }
                    sendMsg(BtStatus.FOUND);
                }

                @Override
                public void finishSearch(Map<String, List<BluetoothBean>> blueToothMap) {
                    searching = false;
                    sendMsg(BtStatus.FINISHED);
                }
            });
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void pairDevice(String address) {
        try {
            bluetoothUtil.makePair(this, address, new PairBluetoothListener() {
                @Override
                public void whilePair(BluetoothDevice device) {

                }

                @Override
                public void pairingSuccess(BluetoothDevice device) {
                    sendMsg(BtStatus.PAIRED);
                }

                @Override
                public void cancelPair(BluetoothDevice device) {
                    sendMsg(BtStatus.NOT_PAIR);
                }
            });
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    @Override
    protected void onDestroy() {
        if (null != btService)
            btService.cancelAllBtThread();
//        unbindService(mServiceConnection);
//        if (null != mGattUpdateReceiver) {
//            unregisterReceiver(mGattUpdateReceiver);
//        }
//        if (null != leService)
//            leService.close();
        if (null != alertDialog)
            alertDialog.dismiss();
        if (null != popupMenu && popupMenu.isShowing())
            popupMenu.dismiss();
        if (searching)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                BTAdapter.cancelDiscovery();
            }
        if (null != bluetoothUtil) {
            bluetoothUtil.closeBluetoothService();
            bluetoothUtil = null;
        }
        if (connectHandler != null) {
            connectHandler.removeCallbacksAndMessages(null);
            connectHandler = null;
        }
        super.onDestroy();
    }

    private enum BtStatus {
        ENABLING,
        ENABLED,
        DISABLING,
        DISABLED,
        SEARCHING,
        FOUND,
        FINISHED,
        RENAME,
        PAIRING,
        PAIRED,
        NOT_PAIR
    }

    private class DetectBluetoothStatus extends Thread {
        private final boolean enable;

        public DetectBluetoothStatus(boolean enable) {
            this.enable = enable;
        }

        @Override
        public void run() {
            super.run();
            while (true) {
                if (BTAdapter.isEnabled() == enable)
                    break;
            }
            sendMsg(enable ? BtStatus.ENABLED : BtStatus.DISABLED);
            interrupt();
        }
    }


}
