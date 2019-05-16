package com.gits.powernap.main;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.gits.powernap.AlarmEvent;
import com.gits.powernap.R;
import com.gits.powernap.db.NapTheory;
import com.gits.powernap.db.PreferenceDb;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

public class LocalService extends IntentService {
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();
    //    private Timer mTimer;
    private static MediaPlayer mMediaPlayer = new MediaPlayer();
    private static Vibrator mVibrator;


    public LocalService() {
        super("PowerNapService");
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    class LocalBinder extends Binder {
        LocalService getService() {
            // Return this instance of LocalService so clients can call public methods
            return LocalService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        startSound();
        showRingingNotification();

        PreferenceDb.saveNapStatus(this, PreferenceDb.TimerStatus.RINGING);
        EventBus.getDefault().post(new AlarmEvent(PreferenceDb.TimerStatus.RINGING));
    }

    /**
     * method for clients
     */

    private void startSound() {
        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        if (alert == null) {
            // alert is null, using backup
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            // I can't see this ever being null (as always have a default notification)
            // but just incase
            if (alert == null) {
                // alert backup is null, using 2nd backup
                alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }
        if (mMediaPlayer == null)
            mMediaPlayer = new MediaPlayer();
        else {
            try {
                mMediaPlayer.stop();
            } catch (Exception e) {
                Log.d("MediaPlayer", e.getMessage());
            }
        }
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        try {
            mMediaPlayer.setDataSource(this, alert);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.start();

        if (mVibrator == null) {
            mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        long[] thrice = {0, 400, 200, 400};
        if (mVibrator != null) {
            mVibrator.vibrate(thrice, 0);
        }
    }

    public void stopSoundAndVibrate() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mVibrator == null) {
            mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
        if (mVibrator != null) {
            mVibrator.cancel();
        }
    }

    public void showRingingNotification() {
        NapTheory.NapTime napTime = NapTheory.getNapByTime(PreferenceDb.getNapTime(this));

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, "1")
                        .setSmallIcon(R.drawable.icon_notif)
                        .setContentTitle("Wake Up!")
                        .setContentText("Your " + (napTime != null ? napTime.getLabel() : "nap") + " time is finish")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setOngoing(true);

        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);
        // Sets an ID for the notification
        int mNotificationId = 1;
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        if (mNotifyMgr != null) {
            mNotifyMgr.notify(mNotificationId, mBuilder.build());
        }
    }


}