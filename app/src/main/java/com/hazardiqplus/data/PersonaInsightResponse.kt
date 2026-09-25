package com.hazardiqplus.data

import com.google.gson.annotations.SerializedName

data class PersonaInsightResponse(
    @SerializedName("location") val location: LocationMeta,
    @SerializedName("activePersonas") val activePersonas: List<String>,
    @SerializedName("generalWeather") val generalWeather: GeneralWeather?,
    @SerializedName("health") val health: HealthInsight?,
    @SerializedName("fitness") val fitness: FitnessInsight?,
    @SerializedName("marine") val marine: MarineInsight?,
    @SerializedName("commute") val commute: CommuteInsight?,
    @SerializedName("agriculture") val agriculture: AgricultureInsight?,
    @SerializedName("eventPlanner") val eventPlanner: EventPlannerInsight?,
    @SerializedName("travel") val travel: TravelInsight?,
    @SerializedName("parent") val parent: ParentInsight?
)

data class GeneralWeather(
    val windSpeed: Double,
    val humidity: Double,
    val visibility: Int,
    val uvIndex: Double,
    val feelsLike: Double,
    val pressure: Double,
    val hourly: Map<String, Any>,
    val daily: Map<String, Any>
)

data class LocationMeta(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lon") val lon: Double,
    @SerializedName("isCoastal") val isCoastal: Boolean
)

data class HealthInsight(
    @SerializedName("aqi") val aqi: Double,
    @SerializedName("pm25") val pm25: Double,
    @SerializedName("pm10") val pm10: Double,
    @SerializedName("uvIndex") val uvIndex: Double,
    @SerializedName("humidity") val humidity: Double,
    @SerializedName("dominantPollen") val dominantPollen: String?,
    @SerializedName("healthRiskAdvisory") val healthRiskAdvisory: String?
)

data class FitnessInsight(
    @SerializedName("sunrise") val sunrise: String,
    @SerializedName("sunset") val sunset: String,
    @SerializedName("bestRunningHours") val bestRunningHours: List<String>,
    @SerializedName("currentWindSpeedKmH") val currentWindSpeedKmH: Double,
    @SerializedName("heatStressLevel") val heatStressLevel: String
)

data class MarineInsight(
    @SerializedName("waveHeightMeters") val waveHeightMeters: Double,
    @SerializedName("wavePeriodSec") val wavePeriodSec: Double,
    @SerializedName("waveDirection") val waveDirection: Double,
    @SerializedName("tideStatus") val tideStatus: String
)

data class CommuteInsight(
    @SerializedName("visibilityMeters") val visibilityMeters: Int,
    @SerializedName("isFogAlert") val isFogAlert: Boolean,
    @SerializedName("commuteSlotImpact") val commuteSlotImpact: String
)

data class AgricultureInsight(
    @SerializedName("soilMoistureM3") val soilMoistureM3: Double,
    @SerializedName("soilTemperatureC") val soilTemperatureC: Double,
    @SerializedName("frostWarning") val frostWarning: Boolean,
    @SerializedName("evapotranspirationMm") val evapotranspirationMm: Double,
    @SerializedName("irrigationAdvice") val irrigationAdvice: String
)

data class EventPlannerInsight(
    @SerializedName("comfortIndex") val comfortIndex: Double,
    @SerializedName("rainProbabilityDay") val rainProbabilityDay: Int,
    @SerializedName("outdoorViability") val outdoorViability: String
)

data class TravelInsight(
    @SerializedName("tempDelta") val tempDelta: Double,
    @SerializedName("packingSuggestions") val packingSuggestions: List<String>
)

data class ParentInsight(
    @SerializedName("morningSchoolWindowRisk") val morningSchoolWindowRisk: String,
    @SerializedName("afternoonPickUpRisk") val afternoonPickUpRisk: String,
    @SerializedName("extremeColdHeatAlert") val extremeColdHeatAlert: String?
)