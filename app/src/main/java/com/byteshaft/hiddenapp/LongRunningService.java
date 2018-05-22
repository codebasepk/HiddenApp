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
import android.util.Log;

public class LongRunningService extends Service {

    private static final String TAG = LongRunningService.class.getName();

    private MyPhoneStateListener mPhoneStateListener;

    private BroadcastReceiver mPermListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Start listening");
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
        Log.i(TAG, "onStartCommand()");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mPermListener,
                new IntentFilter(MainActivity.INTENT_PHONE_STATE_PERMISSION_GRANTED));
        if (Helpers.hasPhoneStatesPermission(getApplicationContext())) {
            togglePhoneStateListener(true);
        }
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
        Log.i(TAG, "onDestroyed()");
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
                    Log.i(TAG, "State Idle");
                    mIsIncoming = false;
                    if (mJustCreated) {
                        return;
                    }
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                            new Intent(MainActivity.INTENT_FINISH_ACTIVITY));
                    Helpers.setAppVisibility(getApplicationContext(), false);
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.i(TAG, "State ringing");
                    mIsIncoming = true;
                    mJustCreated = false;
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    Log.i(TAG, "State picked");
                    mJustCreated = false;
                    if (mIsIncoming) {
                        // The incoming call was picked
                    } else {
                        // We dialed a number manually
                    }
                    Helpers.setAppVisibility(getApplicationContext(), true);
//                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    break;
            }
        }
    }
}
