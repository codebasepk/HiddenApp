///////////////////////////////////////////////////////////////////////////////
//
//   XBR is Gold - https://xbr.network
//
//   Copyright (c) Crossbar.io Technologies GmbH. All rights reserved
//   Licensed under the GPL 3.0 https://opensource.org/licenses/GPL-3.0
//
///////////////////////////////////////////////////////////////////////////////

package com.byteshaft.hiddenapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            ctx.startService(new Intent(ctx.getApplicationContext(), LongRunningService.class));
        }
    }
}
