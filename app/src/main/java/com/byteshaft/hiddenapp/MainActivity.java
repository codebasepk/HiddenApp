package com.byteshaft.hiddenapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    public static final String INTENT_FINISH_ACTIVITY = "com.byteshaft.hiddenapp.close_activity";
    public static final String INTENT_PHONE_STATE_PERMISSION_GRANTED =
            "com.byteshaft.hideapp.phone_state_granted";

    private static final int REQUEST_CODE_NO_BATTERY_OPTIMIZATIONS = 1000;
    private static final int REQUEST_CODE_PHONE_STATES = 1001;
    private static final int SUCCESS_CODE = -1;

    private BroadcastReceiver mFinishBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Helpers.startBackgroundService(getApplicationContext());

        if (!Helpers.hasPhoneStatesPermission(getApplicationContext())) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE_PHONE_STATES);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                mFinishBroadcastReceiver, new IntentFilter(INTENT_FINISH_ACTIVITY));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                mFinishBroadcastReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_NO_BATTERY_OPTIMIZATIONS) {
            if (resultCode == SUCCESS_CODE) {
                Intent intent = new Intent(LongRunningService.INTENT_TOGGLE_VISIBILITY);
                intent.putExtra("enable_app", false);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            } else {

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PHONE_STATES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(
                        new Intent(INTENT_PHONE_STATE_PERMISSION_GRANTED)
                );
            }

            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivityForResult(intent, REQUEST_CODE_NO_BATTERY_OPTIMIZATIONS);
                }
            }
        }
    }
}
