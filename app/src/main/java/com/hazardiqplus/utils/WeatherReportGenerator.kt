package com.hazardiqplus.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.hazardiqplus.R
import com.hazardiqplus.clients.OpenMeteoClient
import com.hazardiqplus.data.PersonaType
import com.hazardiqplus.ui.LoginActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class WeatherReportGenerator(
    context: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.d("WeatherReportGenerator", "No user logged in")
            return Result.failure()
        }

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return Result.failure()
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        val location = getLastLocation(fusedClient) ?: return Result.retry()
        val lat = location.latitude
        val lon = location.longitude

        return try {
            // 1. Fetch Data
            val metrics = OpenMeteoClient.weatherApi.getWeatherMetrics(lat, lon)
            val aqiData = OpenMeteoClient.api.getAirQuality(lat, lon)
            val daily = OpenMeteoClient.weatherApi.getDailyForecast(lat, lon, forecastDays = 2)

            // 2. Get Active Personas
            val activePersonas = PrefsHelper.getActivePersonas(applicationContext)

            // 3. Evaluate Rules & Fire Alerts

            // --- HEALTH PERSONA ---
            if (activePersonas.contains(PersonaType.HEALTH.id)) {
                val aqi = aqiData.current?.usAqi ?: 0
                if (aqi > 100) {
                    sendNotification(2001, "Poor Air Quality", "AQI is $aqi. Consider wearing a mask outdoors today.")
                }

                val uvIndex = aqiData.current?.uvIndex ?: metrics.current.uv_index
                if (uvIndex > 7.0) {
                    sendNotification(2002, "High UV Alert", "UV Index is extremely high ($uvIndex). Wear sunscreen and protect your skin.")
                }
            }

            // --- COMMUTER & PARENT PERSONA ---
            if (activePersonas.contains(PersonaType.COMMUTE.id) || activePersonas.contains(PersonaType.PARENT.id)) {
                val visibility = metrics.current.visibility
                if (visibility < 1000) { // Less than 1km visibility
                    sendNotification(2003, "Low Visibility Warning", "Dense fog detected. Road visibility is down to ${visibility}m. Drive carefully.")
                }
            }

            // --- AGRICULTURE PERSONA ---
            if (activePersonas.contains(PersonaType.AGRO.id)) {
                val minTemp = daily.daily.temperature_2m_min.firstOrNull() ?: 10.0
                if (minTemp <= 2.0) {
                    sendNotification(2004, "Frost Alert", "Temperatures dropping to $minTemp°C tonight. Cover sensitive crops immediately.")
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("WeatherReportGenerator", "Error evaluating background rules", e)
            Result.retry()
        }
    }

    private fun sendNotification(notificationId: Int, title: String, message: String) {
        val channelId = "persona_alerts_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Environmental Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Expands text on pull-down
            .setSmallIcon(R.drawable.warning_24px)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun getLastLocation(client: FusedLocationProviderClient): Location? =
        suspendCancellableCoroutine { cont ->
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                client.lastLocation
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            } else {
                cont.resume(null)
            }
        }
}