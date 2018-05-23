package com.byteshaft.hiddenapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LongRunningService extends Service {

    public static final String INTENT_TOGGLE_VISIBILITY =
            "com.byteshaft.hiddenapp.toggle_visiblity";

    private static final String TAG = LongRunningService.class.getName();

    private MyPhoneStateListener mPhoneStateListener;
    private LocalBroadcastManager mBroadcaster;

    private BroadcastReceiver mLocalBroadcastListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            if (action.equals(MainActivity.INTENT_PHONE_STATE_PERMISSION_GRANTED)) {
                Log.i(TAG, "Start listening");
                togglePhoneStateListener(true);
            } else if (action.equals(INTENT_TOGGLE_VISIBILITY)) {
                boolean enable = intent.getBooleanExtra("enable_app", true);
                if (!enable) {
                    togglePhoneStateListener(false);
                    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
                    executorService.schedule(() -> togglePhoneStateListener(true), 12, TimeUnit.SECONDS);
                }

                Helpers.setAppVisibility(getApplicationContext(), enable);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mPhoneStateListener = new MyPhoneStateListener();
        mBroadcaster = LocalBroadcastManager.getInstance(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand()");
        mBroadcaster.registerReceiver(mLocalBroadcastListener,
                new IntentFilter(INTENT_TOGGLE_VISIBILITY));
        mBroadcaster.registerReceiver(mLocalBroadcastListener,
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
        mBroadcaster.unregisterReceiver(mLocalBroadcastListener);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        togglePhoneStateListener(false);
        mBroadcaster.unregisterReceiver(mLocalBroadcastListener);
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
                    mBroadcaster.sendBroadcast(new Intent(MainActivity.INTENT_FINISH_ACTIVITY));
                    togglePhoneStateListener(false);
                    ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
                    exec.schedule(() -> togglePhoneStateListener(true), 12, TimeUnit.SECONDS);
                    Helpers.setAppVisibility(getApplicationContext(), false);
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    Log.i(TAG, "State ringing");
                    mIsIncoming = true;
                    mJustCreated = false;
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    mJustCreated = false;
                    if (mIsIncoming) {
                        // The incoming call was picked
                        Log.i(TAG, "State picked");
                    } else {
                        // We dialed a number manually
                        Log.i(TAG, "State dialing");
                    }
                    Helpers.setAppVisibility(getApplicationContext(), true);
                    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
                    executorService.schedule(() -> startActivity(new Intent(getApplicationContext(), MainActivity.class)), 12, TimeUnit.SECONDS);
                    break;
                default:
                    System.out.println("CALLED");
            }
        }
    }
}
