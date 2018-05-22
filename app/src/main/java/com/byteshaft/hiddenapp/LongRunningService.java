package com.byteshaft.hiddenapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class LongRunningService extends Service {

    private MyPhoneStateListener mPhoneStateListener;

    private BroadcastReceiver mPermListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("Start lisening");
            togglePhoneStateListener(true);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mPhoneStateListener = new MyPhoneStateListener();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mPermListener,
                new IntentFilter(MainActivity.INTENT_PHONE_STATE_PERMISSION_GRANTED));
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        togglePhoneStateListener(false);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                mPermListener);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        togglePhoneStateListener(false);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                mPermListener);
    }

    private void togglePhoneStateListener(boolean enable) {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            return;
        }
        telephonyManager.listen(
                mPhoneStateListener,
                enable ? PhoneStateListener.LISTEN_CALL_STATE: PhoneStateListener.LISTEN_NONE);
    }

    private class MyPhoneStateListener extends PhoneStateListener {

        private boolean mIsIncoming;
        private boolean mJustCreated = true;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    mIsIncoming = false;
                    if (mJustCreated) {
                        return;
                    }
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                            new Intent(MainActivity.INTENT_FINISH_ACTIVITY));
                    Helpers.setAppVisibility(getApplicationContext(), true);
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    mIsIncoming = true;
                    mJustCreated = false;
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    mJustCreated = false;
                    if (mIsIncoming) {
                        // The incoming call was picked
                    } else {
                        // We dialed a number manually
                    }
                    System.out.println("CALL PICKED");
                    Helpers.setAppVisibility(getApplicationContext(), true);
//                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    break;
            }
        }
    }
}
