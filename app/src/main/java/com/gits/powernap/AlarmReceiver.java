package com.gits.powernap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gits.powernap.main.LocalService;

/**
 * Created by ibun on 05/01/18.
 */

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, LocalService.class);
        context.startService(serviceIntent);
    }
}
