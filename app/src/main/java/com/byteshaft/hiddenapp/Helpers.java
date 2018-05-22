package com.byteshaft.hiddenapp;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import static android.Manifest.permission.READ_PHONE_STATE;

public class Helpers {
    private static boolean isServiceRunning(Context ctx) {
        ActivityManager manager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LongRunningService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void startBackgroundService(Context ctx) {
        if (!isServiceRunning(ctx)) {
            ctx.startService(new Intent(ctx, LongRunningService.class));
        }
    }

    public static void setAppVisibility(Context ctx, boolean visible) {
        ComponentName component = new ComponentName(ctx, MainActivity.class);
        PackageManager pm = ctx.getPackageManager();
        pm.setComponentEnabledSetting(
                component,
                visible ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED:
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static boolean hasPhoneStatesPermission(Context ctx) {
        return ActivityCompat.checkSelfPermission(ctx, READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isAppEnabled(Context ctx) {
        ComponentName component = new ComponentName(ctx, MainActivity.class);
        PackageManager pm = ctx.getPackageManager();
        int componentEnabledSetting = pm.getComponentEnabledSetting(component);
        return componentEnabledSetting != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
    }
}
