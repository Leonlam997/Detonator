package com.leon.detonator.Serial;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.leon.detonator.Base.BaseActivity;
import com.leon.detonator.Base.BaseApplication;
import com.leon.detonator.Dialog.MyProgressDialog;
import com.leon.detonator.R;
import com.leon.detonator.Util.ConstantUtils;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class SerialDataReceiveListener implements SerialPortUtil.OnDataReceiveListener {
    private final Context mContext;
    private final Runnable runnable;
    private final boolean newLG = BaseApplication.readSettings().isNewLG();
    private final int HANDLE_PROGRESS = 1,
            HANDLE_STATUS = 2,
            HANDLE_INITIAL_FINISHED = 3;
    private Handler handler;
    //private long timeCounter;
    private StringBuilder rcvData;
    private SerialPortUtil serialPortUtil;
    private int timeCount, maxCurrent = ConstantUtils.MAXIMUM_CURRENT;
    private boolean scanMode, startAutoDetect, initFinished, firstReadStatus, feedback, semiDetect;
    private MyProgressDialog pDialog;

    public SerialDataReceiveListener(Context mContext, Runnable runnable) {
        this.mContext = mContext;
        this.handler = new Handler();
        this.runnable = runnable;
        startAutoDetect = false;
        feedback = false;
        semiDetect = false;
        rcvData = new StringBuilder();
        //timeCounter = System.currentTimeMillis();
        try {
            serialPortUtil = SerialPortUtil.getInstance();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        timeCount = 0;
        initFinished = false;
        if (newLG)
            myHandler.sendEmptyMessageDelayed(HANDLE_INITIAL_FINISHED, 100);
        else {
            pDialog = new MyProgressDialog(mContext);
            pDialog.setInverseBackgroundForced(false);
            pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pDialog.setCancelable(false);
            pDialog.setTitle(R.string.progress_title);
            pDialog.setMessage(mContext.getResources().getString(R.string.progress_text));
            pDialog.setMax(100);
            pDialog.setProgress(0);
            pDialog.show();
            myHandler.sendEmptyMessageDelayed(HANDLE_PROGRESS, 100);
        }
    }

    private Handler myHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NotNull Message msg) {
            switch (msg.what) {
                case HANDLE_PROGRESS:
                    timeCount += 100;
                    if (timeCount < ConstantUtils.INITIAL_TIME - 100) {
                        pDialog.setProgress(100 * timeCount / ConstantUtils.INITIAL_TIME);
                    }
                    if (pDialog.isShowing()) {
                        if (timeCount > ConstantUtils.INITIAL_TIME) {
                            pDialog.dismiss();
                            rcvData.append(SerialCommand.INITIAL_FAIL);
                            handler.post(runnable);
                        } else {
                            myHandler.sendEmptyMessageDelayed(HANDLE_PROGRESS, 100);
                        }
                    }
                    break;
                case HANDLE_STATUS:
                    myHandler.removeMessages(HANDLE_STATUS);
                    if (newLG) {
                        if (firstReadStatus)
                            serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_MODE, 0));
                        else
                            serialPortUtil.sendCmd(String.format(Locale.CHINA, SerialCommand.CMD_MODE, 2));
                    } else
                        serialPortUtil.sendCmd(SerialCommand.CMD_READ_VOLTAGE);
                    myHandler.sendEmptyMessageDelayed(HANDLE_STATUS, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
                    break;
                case HANDLE_INITIAL_FINISHED:
                    if (newLG)
                        initFinished = true;
                    myHandler.removeMessages(HANDLE_PROGRESS);
                    if (null != pDialog && pDialog.isShowing())
                        pDialog.dismiss();
                    firstReadStatus = false;
                    rcvData.append(SerialCommand.INITIAL_FINISHED);
                    handler.post(runnable);
                    break;
            }
            return false;
        }
    });

    public SerialDataReceiveListener(Context mContext, Runnable runnable, boolean init) {
        this.mContext = mContext;
        this.handler = new Handler();
        this.runnable = runnable;
        startAutoDetect = false;
        feedback = false;
        rcvData = new StringBuilder();
        //timeCounter = System.currentTimeMillis();
        try {
            serialPortUtil = SerialPortUtil.getInstance();
        } catch (Exception e) {
            BaseApplication.writeErrorLog(e);
        }
        if (init) {
            timeCount = 0;
            initFinished = false;
            if (newLG)
                myHandler.sendEmptyMessageDelayed(HANDLE_INITIAL_FINISHED, 100);
            else {
                pDialog = new MyProgressDialog(mContext);
                pDialog.setInverseBackgroundForced(false);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setCancelable(false);
                pDialog.setTitle(R.string.progress_title);
                pDialog.setMessage(mContext.getResources().getString(R.string.progress_text));
                pDialog.setMax(100);
                pDialog.setProgress(0);
                pDialog.show();
                myHandler.sendEmptyMessageDelayed(HANDLE_PROGRESS, 100);
            }
        } else {
            initFinished = true;
            rcvData.append(SerialCommand.INITIAL_FINISHED);
            handler.post(runnable);
        }
    }

    public String getRcvData() {
        return rcvData.toString();
    }

    public void setRcvData(String data) {
        this.rcvData = new StringBuilder(data);
    }

    public void setScanMode(boolean scanMode) {
        this.scanMode = scanMode;
    }

    public void setFeedback(boolean feedback) {
        this.feedback = feedback;
    }

    public void setSemiDetect(boolean semiDetect) {
        this.semiDetect = semiDetect;
    }

    public void setStartAutoDetect(boolean startAutoDetect) {
        this.startAutoDetect = startAutoDetect;
        serialPortUtil.setTest(!startAutoDetect);
        if (newLG)
            firstReadStatus = startAutoDetect;
        if (startAutoDetect) {
            myHandler.sendEmptyMessage(HANDLE_STATUS);
        } else {
            myHandler.removeMessages(HANDLE_STATUS);
        }
    }

    public void setMaxCurrent(int maxCurrent) {
        this.maxCurrent = maxCurrent;
    }

    @Override
    public void onDataReceive(byte[] buffer) {
        if (null == handler)
            return;
        String data = new String(buffer);

//        ((BaseApplication) mContext.getApplicationContext()).myToast(mContext, data);
        Log.d("ZBEST", data);
        if (initFinished) {
            if (!startAutoDetect && !firstReadStatus) {
                if (scanMode || (newLG && data.startsWith(" "))) {
                    rcvData.append(data);
                    handler.post(runnable);
                    return;
                } else if (newLG || !data.contains(SerialCommand.RESPOND_SUCCESS)) {
                    handler.removeCallbacks(runnable);
                    for (byte i : buffer)
                        rcvData.append(String.format("%02X", i));
                    handler.postDelayed(runnable, 40);
                    return;
                }
            }
            if (!newLG && feedback && buffer[0] == (byte) 0xAA) {
                setStartAutoDetect(false);
                rcvData = new StringBuilder(SerialCommand.RESPOND_CONNECTED);
                handler.post(runnable);
            } else {
                try {
                    if (newLG) {
                        if (data.contains(SerialCommand.CMD_BUS_STATUS)) {
                            //+BUS:On,I=203uA,V=11.8V
                            String[] status = data.split(",");
                            if (3 == status.length) {
                                float t = Float.parseFloat(status[2].substring(2, status[2].length() - 3));
                                if (feedback) {
                                    rcvData = new StringBuilder("V" + t);
                                    handler.post(runnable);
                                } else if (!semiDetect)
                                    ((BaseActivity) mContext).setVoltage(t);
                                t = Float.parseFloat(status[1].substring(2, status[1].length() - 2));
                                if (feedback) {
                                    rcvData = new StringBuilder("A" + t);
                                    handler.post(runnable);
                                } else if (!semiDetect)
                                    ((BaseActivity) mContext).setCurrent(t);
                                if (t > maxCurrent) {
                                    ((BaseApplication) ((BaseActivity) mContext).getApplication()).myToast(mContext, t + "mA");
                                    rcvData = new StringBuilder(SerialCommand.ALERT_SHORT_CIRCUIT);
                                    handler.post(runnable);
                                }
                            } else if (data.contains(SerialCommand.BUS_STATUS_ERR)) {
                                rcvData = new StringBuilder(SerialCommand.ALERT_SHORT_CIRCUIT);
                                handler.post(runnable);
                            }
                        } else if (data.contains(SerialCommand.AT_CMD_RESPOND)) {
                            if (startAutoDetect && firstReadStatus) {
                                firstReadStatus = false;
                                myHandler.sendEmptyMessage(HANDLE_STATUS);
                            }
                        }
                    } else if (data.contains("\r")) {
                        data = data.replace(SerialCommand.RESPOND_SUCCESS, "");
                        if (data.contains(SerialCommand.RESPOND_VOLTAGE) && data.indexOf("\r") > data.indexOf(SerialCommand.RESPOND_VOLTAGE)) {
                            int voltage = Integer.parseInt(data.substring(data.indexOf(SerialCommand.RESPOND_VOLTAGE) + SerialCommand.RESPOND_VOLTAGE.length(), data.indexOf("\r")));
//                            if (!semiDetect && voltage < ConstantUtils.MINIMUM_VOLTAGE && voltage > 0) {
//                                rcvData = new StringBuilder(SerialCommand.ALERT_SHORT_CIRCUIT);
//                                ((BaseApplication) ((BaseActivity) mContext).getApplication()).myToast(mContext, (voltage / 100.0f) + "V");
//                                handler.post(runnable);
//                            } else
                            if (feedback) {
                                rcvData = new StringBuilder("V" + (voltage > 0 ? voltage + 10 : 0));
                                handler.post(runnable);
                            } else
                                ((BaseActivity) mContext).setVoltage(voltage / 100.0f);
                            if (firstReadStatus) {
                                myHandler.removeMessages(HANDLE_INITIAL_FINISHED);
                                myHandler.sendEmptyMessageDelayed(HANDLE_INITIAL_FINISHED, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
                            }
                            serialPortUtil.sendCmd(SerialCommand.CMD_READ_CURRENT);
                            if (startAutoDetect) {
                                myHandler.removeMessages(HANDLE_STATUS);
                                myHandler.sendEmptyMessageDelayed(HANDLE_STATUS, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
                            }
                        } else if (data.contains(SerialCommand.RESPOND_CURRENT) && data.indexOf("\r") > data.indexOf(SerialCommand.RESPOND_CURRENT)) {
                            data = data.substring(data.indexOf(SerialCommand.RESPOND_CURRENT) + SerialCommand.RESPOND_CURRENT.length(), data.indexOf("\r"));
                            if (data.length() > 0) {
                                try {
                                    int c = Integer.parseInt(data);
                                    int count = (int) ((c + 2) / 3.4f) + 1;
                                    float current = 0.0f;
                                    if (c == 1)
                                        current = 25;
                                    else if (c > 0 && count > 0)
                                        current = count * 25 + ((c + 2) - (count - 1) * 3.4f) / 3.4f;
                                    if (feedback) {
                                        rcvData = new StringBuilder("A" + current);
                                        handler.post(runnable);
                                    } else
                                        ((BaseActivity) mContext).setCurrent(current);

                                    if (current > maxCurrent) {
                                        ((BaseApplication) ((BaseActivity) mContext).getApplication()).myToast(mContext, (current / 1000.0f) + "mA");
                                        rcvData = new StringBuilder(SerialCommand.ALERT_SHORT_CIRCUIT);
                                        handler.post(runnable);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            if (firstReadStatus) {
                                myHandler.removeMessages(HANDLE_INITIAL_FINISHED);
                                myHandler.sendEmptyMessageDelayed(HANDLE_INITIAL_FINISHED, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
                            }
                            if (startAutoDetect) {
                                myHandler.removeMessages(HANDLE_STATUS);
                                myHandler.sendEmptyMessageDelayed(HANDLE_STATUS, ConstantUtils.REFRESH_STATUS_BAR_PERIOD);
                            }
                        }
                    }
                } catch (Exception e) {
                    BaseApplication.writeErrorLog(e);
                }
            }
            //sendMsg(data);
        } else if (!newLG && data.contains(SerialCommand.INITIAL_FINISHED)) {
            //sendMsg("初始化完成！(" + ((System.currentTimeMillis() - timeCounter) / 1000.0f) + "s)");
            initFinished = true;
            firstReadStatus = true;
            serialPortUtil.sendCmd(SerialCommand.CMD_DEBUG_ON);
            myHandler.sendEmptyMessageDelayed(HANDLE_INITIAL_FINISHED, 100);
        }
    }

    public void closeAllHandler() {
        startAutoDetect = false;
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        if (myHandler != null) {
            myHandler.removeCallbacksAndMessages(null);
            myHandler = null;
        }
    }


}
