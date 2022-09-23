package com.leon.detonator.base;

import android.app.Activity;

import com.leon.detonator.bean.LocalSettingBean;
import com.leon.detonator.R;

public abstract class CheckRegister extends Thread {
    private Activity activity;
    private BaseApplication myApp;

    public CheckRegister setActivity(Activity activity) {
        this.activity = activity;
        myApp = (BaseApplication) activity.getApplication();
        return this;
    }

    public abstract void onError();

    public abstract void onSuccess();

    @Override
    public void run() {
        while (!myApp.isRegisterFinished()) {
            try {
                Thread.sleep(50);
            } catch (Exception e) {
                BaseApplication.writeErrorLog(e);
            }
        }
        LocalSettingBean b = BaseApplication.readSettings();
        if (null == b || !b.isRegistered()) {
            myApp.myToast(activity, activity.getResources().getString(R.string.message_registered_fail));
            onError();
        } else {
            onSuccess();
        }
        super.run();
    }
}
