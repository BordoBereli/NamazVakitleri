package com.kutluoglu.prayer_notifications.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STOP_COUNTDOWN = "STOP_COUNTDOWN"
        const val EXTRA_PRAYER_KEY = "extra_prayer_key"
    }

    override fun onReceive(context: Context, intent: Intent) {
    }
}
