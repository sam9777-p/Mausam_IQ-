package com.hazardiqplus.utils

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WeatherReportScheduler {

    fun scheduleWeatherReport(context: Context) {
        // Schedule both morning and evening sweeps
        scheduleSweep(context, "MorningPersonaSweep", 6, 30)  // 06:30 AM
        scheduleSweep(context, "EveningPersonaSweep", 20, 0)  // 08:00 PM
    }

    private fun scheduleSweep(context: Context, workName: String, hour: Int, minute: Int) {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time has already passed today, schedule it for tomorrow
            if (before(currentTime)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val delayMillis = targetTime.timeInMillis - currentTime.timeInMillis
        val delayMinutes = TimeUnit.MILLISECONDS.toMinutes(delayMillis)

        val workRequest = PeriodicWorkRequestBuilder<WeatherReportGenerator>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.UPDATE, // Updates the existing schedule if it changes
            workRequest
        )

        Log.d("WeatherReportScheduler", "$workName scheduled to run in $delayMinutes minutes.")
    }
}