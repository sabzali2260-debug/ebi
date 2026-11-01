package com.separ.alert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StopAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        context.stopService(Intent(context, AlertService::class.java))
    }
}
