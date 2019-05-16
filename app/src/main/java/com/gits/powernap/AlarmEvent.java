package com.gits.powernap;

import com.gits.powernap.db.PreferenceDb;

/**
 * Created by ibun on 08/01/18.
 */

public class AlarmEvent {
    private PreferenceDb.TimerStatus mStatus;

    public AlarmEvent(PreferenceDb.TimerStatus status) {

        mStatus = status;
    }

    public PreferenceDb.TimerStatus getStatus() {
        return mStatus;
    }
}
