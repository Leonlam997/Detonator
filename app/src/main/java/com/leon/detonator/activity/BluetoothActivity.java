package com.leon.detonator.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.leon.detonator.R;
import com.leon.detonator.adapter.BluetoothListAdapter;
import com.leon.detonator.base.BaseActivity;
import com.leon.detonator.base.BaseApplication;
import com.leon.detonator.bean.BluetoothBean;
import com.leon.detonator.bean.DetonatorBean;
import com.leon.detonator.bean.SchemeBean;
import com.leon.detonator.bluetooth.BluetoothService;
import com.leon.detonator.bluetooth.BluetoothUtil;
import com.leon.detonator.bluetooth.PairBluetoothListener;
import com.leon.detonator.bluetooth.SearchBluetoothListener;
import com.leon.detonator.database.DbUtil;
import com.leon.detonator.dialog.MyProgressDialog;
import com.leon.detonator.util.CRC16;
import com.leon.detonator.util.KeyUtils;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressLint("MissingPermission")
public class BluetoothActivity extends BaseActivity {
    private List<DetonatorBean> detonatorList;
    public final static int STATUS_CONNECTED = 1;
    public final static int STATUS_DATA = 2;
    public final static int STATUS_ERROR = 3;
    private final int STATUS_LIST = 4;
    private List<SchemeBean> schemeList;
    private List<BluetoothBean> list;
    private BluetoothListAdapter adapter;
    private BluetoothUtil bluetoothUtil;
    private BluetoothAdapter btAdapter;
    private BluetoothService btService;
    private PopupWindow popupMenu;
    private MyProgressDialog pDialog;
    private StringBuilder receiveData;
    private boolean searching;
    private boolean sender = false;
    private int schemeSize;
    private int rescanLine;
    private int lastTouchX;
    private int clickIndex;
    private int sendIndex;

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

    private final Handler myHandler = new Handler(msg -> {
        final String success = "Success";
        final String resend = "Resend";
        final int STATUS_RECEIVE_FINISH = 5;
        final int STATUS_NEXT_LIST = 6;
        switch (msg.what) {
            case STATUS_CONNECTED:
                myApp.myToast(BluetoothActivity.this, String.format(Locale.getDefault(), getString(R.string.bt_connected_device), msg.obj));
            case STATUS_NEXT_LIST:
                if (sender)
                    sendData();
                break;
            case STATUS_DATA:
                if (msg.arg1 > 0) {
                    if (sender) {
                        msg.getTarget().removeMessages(STATUS_ERROR);
                        String data = new String(Arrays.copyOfRange((byte[]) msg.obj, 0, msg.arg1));
                        switch (data) {
                            case success:
                                getNextList();
                                if (detonatorList.size() > 0)
                                    msg.getTarget().sendEmptyMessageDelayed(STATUS_NEXT_LIST, 500);
                                else {
                                    myApp.myToast(BluetoothActivity.this, R.string.message_send_success);
                                    sender = false;
                                    if (null != pDialog && pDialog.isShowing())
                                        pDialog.dismiss();
                                }
                                break;
                            case resend:
                                if (null != pDialog && pDialog.isShowing())
                                    sendData();
                                break;
                        }
                    } else {
                        msg.getTarget().removeMessages(STATUS_RECEIVE_FINISH);
                        if (searching) {
                            searching = false;
                            btAdapter.cancelDiscovery();
                        }
                        receiveData.append(new String(Arrays.copyOfRange((byte[]) msg.obj, 0, msg.arg1)));
                        msg.getTarget().sendEmptyMessageDelayed(STATUS_RECEIVE_FINISH, 100);
                    }
                }
                break;
            case STATUS_RECEIVE_FINISH:
                final String data = receiveData.toString();
                receiveData = new StringBuilder();
                if (data.startsWith(CRC16.getTableCRC(data.substring(4).getBytes()))) {
                    btService.write(success.getBytes());
                    saveData(data);
                } else
                    btService.write(resend.getBytes());
                break;
            case STATUS_ERROR:
                msg.getTarget().removeMessages(STATUS_ERROR);
                myApp.myToast(BluetoothActivity.this, (String) msg.obj);
                if (null != pDialog && pDialog.isShowing())
                    pDialog.dismiss();
                break;
            case STATUS_LIST:
                BtStatus status = BtStatus.values()[msg.arg1];
                BluetoothBean bean;
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
                        btService = new BluetoothService(msg.getTarget(), BluetoothActivity.this);
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
                        com.leon.detonator.bluetooth.BluetoothBean bt = bean.getBluetooth();
                        bt.setAddress(btAdapter.getName());
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
                break;
        }
        return false;
    });

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (RESULT_OK == result.getResultCode()) {
            sendIndex = -1;
            schemeList = DbUtil.getCurrentSchemeList();
            getNextList();
            if (detonatorList.size() <= 0)
                myApp.myToast(BluetoothActivity.this, R.string.message_list_not_found);
            else {
                if (null != btService) {
                    BluetoothDevice device = btAdapter.getRemoteDevice(list.get(clickIndex).getBluetooth().getAddress());
                    btService.connect(device);
                    sender = true;
                    pDialog = new MyProgressDialog(BluetoothActivity.this);
                    pDialog.setInverseBackgroundForced(false);
                    pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                    pDialog.setCancelable(false);
                    pDialog.setTitle(R.string.progress_title);
                    pDialog.setMessage(getString(R.string.progress_connecting));
                    pDialog.show();
                    myHandler.sendMessageDelayed(myHandler.obtainMessage(STATUS_ERROR, getString(R.string.bt_connect_timeout)), 10000);
                }
            }
        }
    });

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        setTitle(R.string.settings_bt);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothUtil = new BluetoothUtil();
        searching = false;
        receiveData = new StringBuilder();
        schemeSize = DbUtil.getSchemeList().size();
        if (schemeSize == 1)
            schemeList = DbUtil.getCurrentSchemeList();
        initData(false);
        final ListView lvBT = findViewById(R.id.lv_bt);
        adapter = new BluetoothListAdapter(this, list, which -> {
            switch (which) {
                case 0:
                    if (searching) {
                        searching = false;
                        btAdapter.cancelDiscovery();
                    }
                    btAdapter.disable();
                    myHandler.obtainMessage(STATUS_LIST, BtStatus.DISABLING.ordinal(), 0).sendToTarget();
                    new DetectBluetoothStatus(false).start();
                    break;
                case 1:
                    btAdapter.enable();
                    myHandler.obtainMessage(STATUS_LIST, BtStatus.ENABLING.ordinal(), 0).sendToTarget();
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
                final View v = LayoutInflater.from(BluetoothActivity.this).inflate(R.layout.layout_dialog_edit, parent, false);
                final EditText etDelay = v.findViewById(R.id.et_dialog);
                final TextView tvDelay = v.findViewById(R.id.tv_dialog);
                etDelay.setHint(R.string.hint_input_name);
                etDelay.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
                etDelay.setInputType(InputType.TYPE_CLASS_TEXT);
                tvDelay.setVisibility(View.GONE);
                BaseApplication.customDialog(new AlertDialog.Builder(BluetoothActivity.this, R.style.AlertDialog)
                        .setTitle(R.string.dialog_title_edit_name)
                        .setView(v)
                        .setCancelable(false)
                        .setPositiveButton(R.string.button_confirm, (dialog, which) -> {
                            if (!etDelay.getText().toString().isEmpty()) {
                                btAdapter.setName(etDelay.getText().toString());
                                myHandler.obtainMessage(STATUS_LIST, BtStatus.RENAME.ordinal(), 0).sendToTarget();
                            }
                        })
                        .setNegativeButton(R.string.button_cancel, null)
                        .show(), false);
            } else {
                if (searching) {
                    searching = false;
                    btAdapter.cancelDiscovery();
                }
                if (position > rescanLine) {
                    BluetoothBean bean = list.get(position);
                    bean.setScanning(true);
                    list.set(position, bean);
                    myHandler.obtainMessage(STATUS_LIST, BtStatus.PAIRING.ordinal(), 0).sendToTarget();
                    pairDevice(bean.getBluetooth().getAddress());
                }
                if (position < rescanLine && position > 2)
                    showPopupWindow(parent, view, position);
            }
        });
        btService = new BluetoothService(myHandler, BluetoothActivity.this);
        if (btAdapter.isEnabled()) {
            startSearch();
            btService.acceptWait();
        }
    }

    private void sendData() {
        try {
            pDialog.setMessage(getString(R.string.bt_progress_sending));
            StringBuilder sb = new StringBuilder();
            sb.append(schemeList.get(sendIndex).getName()).append("\n").append(BaseApplication.isTunnel).append("\n");
            JSONArray jsonArray = new JSONArray();
            for (DetonatorBean bean : detonatorList)
                jsonArray.put(bean.toJSON());
            sb.append(jsonArray);
            sb.insert(0, CRC16.getTableCRC(sb.toString().getBytes()));
            btService.write(sb.toString().getBytes());
            myHandler.removeMessages(STATUS_DATA);
            myHandler.removeMessages(STATUS_ERROR);
            myHandler.sendMessageDelayed(myHandler.obtainMessage(STATUS_ERROR, getString(R.string.bt_send_timeout)), (sb.length() / 1024 + 10) * 100);
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    private void saveData(String data) {
        String[] scheme = data.substring(4).split("\n");
        if (scheme.length == 3)
            try {
                SchemeBean bean = new SchemeBean();
                List<DetonatorBean> list = new ArrayList<>();
                JSONArray jsonArray = new JSONArray(scheme[2]);
                for (int i = 0; i < jsonArray.length(); i++) {
                    DetonatorBean bean1 = new DetonatorBean();
                    bean1.fromJSON(jsonArray.getJSONObject(i));
                    list.add(bean1);
                }
                bean.setName(scheme[0]);
                DbUtil.updateScheme(bean, Boolean.parseBoolean(scheme[1]));
                for (DetonatorBean bean1 : list)
                    bean1.setSchemeId(bean.getId());
                DbUtil.updateDetonatorList(list);
                myApp.myToast(BluetoothActivity.this, String.format(getString(R.string.message_save_list_success), scheme[0]));
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
    }

    private void showPopupWindow(AdapterView<?> parent, View view, int position) {
        final String[] menu;
        menu = new String[]{getString(R.string.menu_send_list),
                getString(R.string.menu_cancel_pair)};
        for (int i = 0; i < menu.length; i++)
            menu[i] = (i + 1) + "." + menu[i];
        View popupView = BluetoothActivity.this.getLayoutInflater().inflate(R.layout.layout_popup_window, parent, false);
        popupView.findViewById(R.id.tvTitle).setVisibility(View.GONE);
        clickIndex = position;
        ListView lsvMenu = popupView.findViewById(R.id.lvPopupMenu);
        lsvMenu.setAdapter(new ArrayAdapter<>(BluetoothActivity.this, R.layout.layout_item_popup_window, menu));
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
                if (schemeSize > 1) {
                    Intent intent = new Intent(BluetoothActivity.this, SchemeActivity.class);
                    intent.putExtra(KeyUtils.KEY_SELECT_SCHEME, true);
                    launcher.launch(intent);
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

    private void getNextList() {
        detonatorList = new ArrayList<>();
        while (++sendIndex < schemeList.size()) {
            detonatorList = DbUtil.getDetonatorList(schemeList.get(sendIndex).getId());
            if (detonatorList.size() > 0)
                break;
        }
    }

    private void initData(boolean discoverable) {
        if (list == null)
            list = new ArrayList<>();
        else
            list.clear();
        BluetoothBean bean = new BluetoothBean();
        com.leon.detonator.bluetooth.BluetoothBean bt = new com.leon.detonator.bluetooth.BluetoothBean();
        bt.setName(getString(R.string.settings_bt));
        bean.setBluetooth(bt);
        bean.setEnabled(btAdapter.isEnabled());
        list.add(bean);
        rescanLine = -1;
        bean = new BluetoothBean();
        bt = new com.leon.detonator.bluetooth.BluetoothBean();
        bt.setName(getString(R.string.bt_device_name));
        bt.setAddress(btAdapter.getName());
        bean.setBluetooth(bt);
        list.add(bean);
        if (btAdapter.isEnabled()) {
            bean = new BluetoothBean();
            bt = new com.leon.detonator.bluetooth.BluetoothBean();
            bt.setName(getString(R.string.bt_paired_device));
            bean.setBluetooth(bt);
            bean.setRescanLine(true);
            list.add(bean);
            bean = new BluetoothBean();
            bt = new com.leon.detonator.bluetooth.BluetoothBean();
            bt.setName(getString(R.string.bt_available_device));
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
                    myHandler.obtainMessage(STATUS_LIST, BtStatus.SEARCHING.ordinal(), 0).sendToTarget();
                    searching = true;
                }

                @Override
                public void foundDevice(com.leon.detonator.bluetooth.BluetoothBean bluetooth, boolean newDevice) {
                    BluetoothBean bean = new BluetoothBean();
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
                    myHandler.obtainMessage(STATUS_LIST, BtStatus.FOUND.ordinal(), 0).sendToTarget();
                }

                @Override
                public void finishSearch(Map<String, List<com.leon.detonator.bluetooth.BluetoothBean>> blueToothMap) {
                    searching = false;
                    myHandler.obtainMessage(STATUS_LIST, BtStatus.FINISHED.ordinal(), 0).sendToTarget();
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
                    myHandler.obtainMessage(STATUS_LIST, BtStatus.PAIRED.ordinal(), 0).sendToTarget();
                }

                @Override
                public void cancelPair(BluetoothDevice device) {
                    myHandler.obtainMessage(STATUS_LIST, BtStatus.NOT_PAIR.ordinal(), 0).sendToTarget();
                }
            });
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
    }

    @Override
    protected void onDestroy() {
        myHandler.removeCallbacksAndMessages(null);
        if (null != btService)
            btService.cancelAllBtThread();
        if (null != popupMenu && popupMenu.isShowing())
            popupMenu.dismiss();
        if (searching)
            btAdapter.cancelDiscovery();
        if (null != bluetoothUtil) {
            bluetoothUtil.closeBluetoothService();
            bluetoothUtil = null;
        }
        super.onDestroy();
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
                if (btAdapter.isEnabled() == enable)
                    break;
            }
            myHandler.obtainMessage(STATUS_LIST, enable ? BtStatus.ENABLED.ordinal() : BtStatus.DISABLED.ordinal(), 0).sendToTarget();
            interrupt();
        }
    }
}
