package com.leon.detonator.base;

import android.app.Activity;

import com.leon.detonator.R;

public abstract class CheckRegister extends Thread {
    private final Activity activity;
    private final BaseApplication myApp;

    public CheckRegister(Activity activity) {
        this.activity = activity;
        myApp = (BaseApplication) activity.getApplication();
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
        if (null == BaseApplication.settings || !BaseApplication.settings.isRegistered()) {
            myApp.myToast(activity, activity.getResources().getString(R.string.message_registered_fail));
            onError();
        } else {
            onSuccess();
        }
        super.run();
    }
}
