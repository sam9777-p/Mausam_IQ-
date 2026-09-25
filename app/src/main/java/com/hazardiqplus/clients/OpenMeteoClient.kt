package com.hazardiqplus.clients

import com.google.gson.annotations.SerializedName
import com.hazardiqplus.data.AQIResponse
import com.hazardiqplus.data.OmMetricsResponse
import com.hazardiqplus.data.OmWeatherDailyResponse
import com.hazardiqplus.data.OmWeatherResponse
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- NEW DATA MODELS FOR PERSONAS ---
data class OmAirQualityResponse(
    @SerializedName("current") val current: OmAqiCurrent?
)

data class OmAqiCurrent(
    @SerializedName("us_aqi") val usAqi: Int?,
    @SerializedName("pm10") val pm10: Double?,
    @SerializedName("pm2_5") val pm25: Double?,
    @SerializedName("uv_index") val uvIndex: Double?,
    @SerializedName("grass_pollen") val grassPollen: Double?,
    @SerializedName("birch_pollen") val birchPollen: Double?
)

data class OmMarineResponse(
    @SerializedName("current") val current: OmMarineCurrent?
)

data class OmMarineCurrent(
    @SerializedName("wave_height") val waveHeight: Double?,
    @SerializedName("wave_period") val wavePeriod: Double?,
    @SerializedName("wave_direction") val waveDirection: Double?
)

// --- API CLIENT ---
object OpenMeteoClient {
    private const val AQI_BASE_URL = "https://air-quality-api.open-meteo.com/v1/"
    private const val WEATHER_BASE_URL = "https://api.open-meteo.com/v1/"
    private const val MARINE_BASE_URL = "https://marine-api.open-meteo.com/v1/"

    // 1. AIR QUALITY API (Existing + New)
    interface OpenMeteoAqiApi {
        // Your existing method
        @GET("air-quality")
        suspend fun getAQIHourly(
            @Query("latitude") latitude: Double,
            @Query("longitude") longitude: Double,
            @Query("hourly") hourly: String = "pm10,pm2_5,carbon_monoxide,ozone,nitrogen_dioxide,sulphur_dioxide",
            @Query("timezone") timezone: String = "auto"
        ): AQIResponse

        // NEW: Used for the Health Persona
        @GET("air-quality")
        suspend fun getAirQuality(
            @Query("latitude") latitude: Double,
            @Query("longitude") longitude: Double,
            @Query("current") current: String = "us_aqi,pm10,pm2_5,uv_index,grass_pollen,birch_pollen",
            @Query("timezone") timezone: String = "auto"
        ): OmAirQualityResponse
    }

    val api: OpenMeteoAqiApi by lazy {
        Retrofit.Builder()
            .baseUrl(AQI_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoAqiApi::class.java)
    }

    // 2. WEATHER API (Your exact existing code)
    interface OpenMeteoWeatherApi {
        @GET("forecast")
        suspend fun getHourlyForecast(
            @Query("latitude") latitude: Double,
            @Query("longitude") longitude: Double,
            @Query("hourly") hourly: String = "temperature_2m,weathercode",
            @Query("forecast_days") forecastDays: Int = 1,
            @Query("timezone") timezone: String = "auto"
        ): OmWeatherResponse

        @GET("forecast")
        suspend fun getDailyForecast(
            @Query("latitude") latitude: Double,
            @Query("longitude") longitude: Double,
            @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,weathercode",
            @Query("forecast_days") forecastDays: Int = 7,
            @Query("timezone") timezone: String = "auto"
        ): OmWeatherDailyResponse

        @GET("forecast")
        suspend fun getWeatherMetrics(
            @Query("latitude") latitude: Double,
            @Query("longitude") longitude: Double,
            @Query("current") current: String = "temperature_2m,relative_humidity_2m,apparent_temperature,pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,visibility,uv_index",
            @Query("timezone") timezone: String = "auto"
        ): OmMetricsResponse
    }

    val weatherApi: OpenMeteoWeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl(WEATHER_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoWeatherApi::class.java)
    }

    // 3. NEW: MARINE API (For Beach Persona)
    interface OpenMeteoMarineApi {
        @GET("marine")
        suspend fun getMarine(
            @Query("latitude") latitude: Double,
            @Query("longitude") longitude: Double,
            @Query("current") current: String = "wave_height,wave_period,wave_direction",
            @Query("timezone") timezone: String = "auto"
        ): OmMarineResponse
    }

    val marineApi: OpenMeteoMarineApi by lazy {
        Retrofit.Builder()
            .baseUrl(MARINE_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoMarineApi::class.java)
    }
}