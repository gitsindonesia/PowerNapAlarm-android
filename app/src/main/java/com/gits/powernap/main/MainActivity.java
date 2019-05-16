package com.gits.powernap.main;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.gits.powernap.AlarmEvent;
import com.gits.powernap.AlarmReceiver;
import com.gits.powernap.R;
import com.gits.powernap.db.NapTheory;
import com.gits.powernap.db.PreferenceDb;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {
    LocalService mService;
    boolean mBound = false;

    private Button mBtnAction;
    private TextView mBtnChange;
    private TextView tvCountDown;
    private TextView tvDescription;

    private NapTheory.NapTime selectedNapTime;

    public MutableLiveData<PreferenceDb.TimerStatus> timerStatus = new MutableLiveData<>();

    private AlarmManager mAlarmMgr;
    private Timer mTimer;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Fabric.with(this, new Crashlytics());
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        tvCountDown = findViewById(R.id.tvCountdown);
        tvDescription = findViewById(R.id.tvDescription);
        mBtnAction = findViewById(R.id.btnAction);
        mBtnChange = findViewById(R.id.btnChange);

        mBtnAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAction();
            }
        });
        mBtnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showListDialog();
            }
        });

        mAlarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        if (PreferenceDb.getNapTime(this) == 0) {
            PreferenceDb.saveNapDuration(this, NapTheory.listNapTime[0].getDuration());
        }
        selectedNapTime = NapTheory.getNapByTime(PreferenceDb.getNapTime(this));

        initLiveDataObserver();

        if (PreferenceDb.getNapStatus(this) == null) {
//            PreferenceDb.saveNapStatus(this, PreferenceDb.TimerStatus.STOPPED);
            timerStatus.setValue(PreferenceDb.TimerStatus.STOPPED);
        } else {
            timerStatus.setValue(PreferenceDb.getNapStatus(this));
        }

        setDescription();

        PreferenceDb.TimerStatus timerStatus = PreferenceDb.getNapStatus(this);
        if (timerStatus == PreferenceDb.TimerStatus.RINGING) {
            setCountdownText(0);
        } else if (timerStatus == PreferenceDb.TimerStatus.COUNTING) {
            startCountdownTimer();
        } else if (timerStatus == PreferenceDb.TimerStatus.STOPPED) {
            setCountdownText(selectedNapTime.getDuration());
        }

        clearNotification();

    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, LocalService.class);

        // Bind to LocalService
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        if (PreferenceDb.getNapStatus(this) == PreferenceDb.TimerStatus.COUNTING) {
            showOngoingNotification();
        }

        unbindService(mConnection);
        mBound = false;

        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    private void onAction() {
        Log.d("status", timerStatus + "");
        if (timerStatus.getValue() == PreferenceDb.TimerStatus.COUNTING || timerStatus.getValue() == PreferenceDb.TimerStatus.RINGING) {
            PreferenceDb.saveStartTime(this, 0);
            timerStatus.setValue(PreferenceDb.TimerStatus.STOPPED);
        } else {
            Log.d("status now", "COUNTING");
            PreferenceDb.saveStartTime(this, new Date().getTime());
            PreferenceDb.saveNapDuration(this, selectedNapTime.getDuration());
            timerStatus.setValue(PreferenceDb.TimerStatus.COUNTING);

            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, selectedNapTime.getDuration() + "");
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, selectedNapTime.getLabel());
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "nap_type");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        }
    }


    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            LocalService.LocalBinder binder = (LocalService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    private void startCountdownTimer() {
        if (PreferenceDb.getNapStatus(this) == PreferenceDb.TimerStatus.COUNTING) {
            stopCountdownTimer();
            final long timerStart = PreferenceDb.getStartTime(this);
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    final long diff = selectedNapTime.getDuration() - (new Date().getTime() - timerStart);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (diff > 0) {
                                setCountdownText(diff + 1000);
                            } else {
                                setCountdownText(0);
                            }
                        }
                    });
                }
            }, new Date(timerStart), 1000);
//        }
        }
    }

    private void stopCountdownTimer() {
        if (mTimer != null) {
            try {
                mTimer.cancel();
                mTimer.purge();
            } catch (Exception e) {
                Log.d("Timer", e.getMessage());
            }
        }
    }

    private void initLiveDataObserver() {
        timerStatus.observe(this, new Observer<PreferenceDb.TimerStatus>() {
            @Override
            public void onChanged(@Nullable PreferenceDb.TimerStatus status) {
                PreferenceDb.saveNapStatus(MainActivity.this, status);
                setActionButtonText(status);
                if (status == PreferenceDb.TimerStatus.STOPPED) {
                    if (mBound) {
                        mService.stopSoundAndVibrate();
                    }
                    stopAlarmManager();
                    stopCountdownTimer();
                    setCountdownText(PreferenceDb.getNapTime(MainActivity.this));
                    clearNotification();
                } else if (status == PreferenceDb.TimerStatus.RINGING) {
                    stopAlarmManager();
                    stopCountdownTimer();
                    setCountdownText(0);
                    stopCountdownTimer();
                } else if (status == PreferenceDb.TimerStatus.COUNTING) {
                    startAlarmManager();
                    startCountdownTimer();
                }
            }
        });
    }


    private void setCountdownText(long millisecond) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        df.setTimeZone(tz);
        String time = df.format(new Date(millisecond));

        tvCountDown.setText(time);
    }

    private void setActionButtonText(PreferenceDb.TimerStatus timerStatus) {
        switch (timerStatus) {
            case COUNTING:
                mBtnAction.setText(getString(R.string.stop));
                mBtnChange.setVisibility(View.GONE);
                break;
            case RINGING:
                mBtnAction.setText(getString(R.string.dismiss));
                mBtnChange.setVisibility(View.GONE);
                break;
            case STOPPED:
                mBtnAction.setText(getString(R.string.start));
                mBtnChange.setVisibility(View.VISIBLE);
                setCountdownText(selectedNapTime.getDuration());
                break;
        }
    }

    private void showListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Nap Type")
                .setItems(NapTheory.getTitles(), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        selectedNapTime = NapTheory.listNapTime[which];
                        setDescription();
                        setCountdownText(selectedNapTime.getDuration());
                    }
                });
        builder.create().show();
    }

    private void setDescription() {
        tvDescription.setText(selectedNapTime.getDescription());
    }

    public void startAlarmManager() {
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        mAlarmMgr.cancel(alarmIntent);

        mAlarmMgr.set(AlarmManager.RTC_WAKEUP,
                PreferenceDb.getStartTime(this) +
                        selectedNapTime.getDuration(), alarmIntent);

    }

    private void stopAlarmManager() {
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        mAlarmMgr.cancel(alarmIntent);
    }


    public void showOngoingNotification() {
        NapTheory.NapTime napTime = NapTheory.getNapByTime(PreferenceDb.getNapTime(this));

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, "1")
                        .setSmallIcon(R.drawable.icon_notif)
                        .setContentTitle("Power Nap is running")
                        .setContentText(napTime != null ? napTime.getLabel() : "Nap time!")
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

    public void clearNotification() {
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (mNotifyMgr != null) {
            mNotifyMgr.cancelAll();
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAlarmEvent(AlarmEvent event) {
        timerStatus.setValue(event.getStatus());
    }
}
