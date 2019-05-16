package com.gits.powernap.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by ibun on 05/01/18.
 */

public class PreferenceDb {
    public static String KEY_NAP_DURATION = "KEY_NAP_DURATION";
    public static String KEY_NAP_STATUS = "KEY_NAP_STATUS";
    public static String KEY_START_TIME = "KEY_START_TIME";

    public enum TimerStatus {
        COUNTING,
        STOPPED,
        RINGING
    }


    public static SharedPreferences getPreference(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static long getNapTime(Context context) {
        return getPreference(context).getLong(KEY_NAP_DURATION, 0);
    }

    public static TimerStatus getNapStatus(Context context) {
        return getStatusInt(getPreference(context).getInt(KEY_NAP_STATUS, 0));
    }

    public static long getStartTime(Context context) {
        return getPreference(context).getLong(KEY_START_TIME, 0);
    }

    public static void saveNapDuration(Context context, long millisecond) {
        getPreference(context).edit().putLong(KEY_NAP_DURATION, millisecond).apply();
    }

    public static void saveNapStatus(Context context, TimerStatus status) {
        getPreference(context).edit().putInt(KEY_NAP_STATUS, getStatusInt(status)).apply();
    }

    private static int getStatusInt(TimerStatus status) {
        int statInt = 0;
        switch (status) {
            case STOPPED:
                statInt = 1;
                break;
            case COUNTING:
                statInt = 2;
                break;
            case RINGING:
                statInt = 3;
                break;
        }
        return statInt;
    }

    private static TimerStatus getStatusInt(int status) {
        TimerStatus timerStatus = null;
        switch (status) {
            case 1:
                timerStatus = TimerStatus.STOPPED;
                break;
            case 2:
                timerStatus = TimerStatus.COUNTING;
                break;
            case 3:
                timerStatus = TimerStatus.RINGING;
                break;
        }
        return timerStatus;
    }

    public static void saveStartTime(Context context, long timestamp) {
        getPreference(context).edit().putLong(KEY_START_TIME, timestamp).apply();
    }
}
