/*
 * Copyright (C) 2023 ArrowOS
 * 
 * SPDX-License-Identifier: Apache-2.0
 */

package com.arrow.dubaicameraservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED))
            return;

        DubaiCameraService.startService(context);
    }
}
