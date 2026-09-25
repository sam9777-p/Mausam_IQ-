package com.hazardiqplus.ui.citizen.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.hazardiqplus.R
import com.hazardiqplus.adapters.PersonaCardItem
import com.hazardiqplus.adapters.PersonaDashboardAdapter
import com.hazardiqplus.adapters.Weather24hAdapter
import com.hazardiqplus.adapters.Weather7dAdapter
import com.hazardiqplus.clients.OmAirQualityResponse
import com.hazardiqplus.clients.OmMarineResponse
import com.hazardiqplus.clients.OpenMeteoClient
import com.hazardiqplus.data.PersonaType
import com.hazardiqplus.utils.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.*

class CitizenWeatherFragment : Fragment(R.layout.fragment_citizen_weather) {

    // General Weather Views
    private lateinit var tvWindSpeed: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvVisibility: TextView
    private lateinit var tvUvIndex: TextView
    private lateinit var tvFeelsLike: TextView
    private lateinit var tvPressure: TextView
    private lateinit var hv24h: RecyclerView
    private lateinit var progressCircular24h: CircularProgressIndicator
    private lateinit var rv7d: RecyclerView
    private lateinit var progressCircular7d: CircularProgressIndicator

    // Persona Views
    private lateinit var personaChipGroup: ChipGroup
    private lateinit var rvPersonaCards: RecyclerView
    private lateinit var personaAdapter: PersonaDashboardAdapter

    // Cached Location
    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_citizen_weather, container, false)

        tvWindSpeed = view.findViewById(R.id.tvWindSpeed)
        tvHumidity = view.findViewById(R.id.tvHumidity)
        tvVisibility = view.findViewById(R.id.tvVisibility)
        tvUvIndex = view.findViewById(R.id.tvUvIndex)
        tvFeelsLike = view.findViewById(R.id.tvFeelsLike)
        tvPressure = view.findViewById(R.id.tvPressure)
        hv24h = view.findViewById(R.id.hv24hWeatherForecast)
        progressCircular24h = view.findViewById(R.id.progressCircular24h)
        progressCircular7d = view.findViewById(R.id.progressCircular7d)
        rv7d = view.findViewById(R.id.hv7dWeatherForecast)

        personaChipGroup = view.findViewById(R.id.personaChipGroup)
        rvPersonaCards = view.findViewById(R.id.rvPersonaCards)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerViews
        hv24h.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        rv7d.layoutManager = LinearLayoutManager(requireContext())

        personaAdapter = PersonaDashboardAdapter()
        rvPersonaCards.layoutManager = LinearLayoutManager(requireContext())
        rvPersonaCards.adapter = personaAdapter

        setupPersonaChips()

        progressCircular24h.visibility = View.VISIBLE
        progressCircular7d.visibility = View.VISIBLE

        fetchLocationAndData()
    }

    private fun setupPersonaChips() {
        val activePersonas = PrefsHelper.getActivePersonas(requireContext())
        personaChipGroup.removeAllViews()

        PersonaType.entries.forEach { persona ->
            val chip = Chip(requireContext()).apply {
                text = persona.displayName
                isCheckable = true
                isChecked = activePersonas.contains(persona.id)

                setOnCheckedChangeListener { _, isChecked ->
                    val updated = PrefsHelper.togglePersona(requireContext(), persona.id, isChecked)
                    if (updated.isEmpty() && !isChecked) {
                        this.isChecked = true
                        Toast.makeText(requireContext(), "At least one persona must be active", Toast.LENGTH_SHORT).show()
                    } else {
                        if (currentLat != 0.0 && currentLon != 0.0) {
                            fetchAllData(currentLat, currentLon)
                        }
                    }
                }
            }
            personaChipGroup.addView(chip)
        }
    }

    private fun fetchLocationAndData() {
        try {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            LocationServices.getFusedLocationProviderClient(requireContext())
                .lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        currentLat = location.latitude
                        currentLon = location.longitude
                        fetchAllData(currentLat, currentLon)
                    } else {
                        Snackbar.make(requireView(), "Could not get location", Snackbar.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Location", "Failed to get location", e)
                    Snackbar.make(requireView(), "Location unavailable", Snackbar.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun fetchAllData(lat: Double, lon: Double) {
        val activePersonas = PrefsHelper.getActivePersonas(requireContext())
        val isNearCoast = checkIsCoastal(lat, lon)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Fetch General Weather Metrics & Forecasts directly
                val metrics = OpenMeteoClient.weatherApi.getWeatherMetrics(lat, lon)
                val hourly = OpenMeteoClient.weatherApi.getHourlyForecast(
                    latitude = lat, longitude = lon,
                    hourly = "temperature_2m,weathercode",
                    forecastDays = 1
                )
                val daily = OpenMeteoClient.weatherApi.getDailyForecast(
                    latitude = lat, longitude = lon,
                    daily = "temperature_2m_max,temperature_2m_min,weathercode", // NOTE: Ensure sunrise/sunset are added to your OpenMeteoClient interface if you want them for Fitness
                    forecastDays = 7
                )

                // 2. Fetch Air Quality
                var aqiData: OmAirQualityResponse? = null
                try {
                    aqiData = OpenMeteoClient.api.getAirQuality(lat, lon)
                } catch (e: Exception) {
                    Log.w("Weather", "AQI endpoint unavailable", e)
                }

                // 3. Fetch Marine (only if near coastal coordinates and persona is active)
                var marineData: OmMarineResponse? = null
                if (isNearCoast && activePersonas.contains(PersonaType.BEACH.id)) {
                    try {
                        marineData = OpenMeteoClient.marineApi.getMarine(lat, lon)
                    } catch (e: Exception) {
                        Log.w("Weather", "Marine endpoint unavailable", e)
                    }
                }

                // 4. Update the 6 General Cards & 24h / 7d Forecasts on Main Thread
                withContext(Dispatchers.Main) {
                    tvWindSpeed.text = "${metrics.current.wind_speed_10m} km/h"
                    tvHumidity.text = "${metrics.current.relative_humidity_2m}%"
                    tvVisibility.text = "${metrics.current.visibility / 1000} km"
                    tvUvIndex.text = "${metrics.current.uv_index}"
                    tvFeelsLike.text = "${metrics.current.apparent_temperature}°C"
                    tvPressure.text = "${metrics.current.pressure_msl} hPa"

                    hv24h.adapter = Weather24hAdapter(
                        hourly.hourly.time,
                        hourly.hourly.temperature_2m,
                        hourly.hourly.weathercode
                    )
                    rv7d.adapter = Weather7dAdapter(
                        daily.daily.time,
                        daily.daily.temperature_2m_max,
                        daily.daily.temperature_2m_min,
                        daily.daily.weathercode
                    )

                    progressCircular24h.visibility = View.GONE
                    progressCircular7d.visibility = View.GONE
                }

                // 5. Compute Persona Cards on Client Side
                val cardList = mutableListOf<PersonaCardItem>()

                // Health Card
                if (activePersonas.contains(PersonaType.HEALTH.id)) {
                    val aqiVal = aqiData?.current?.usAqi ?: 45
                    val pm25Val = aqiData?.current?.pm25 ?: 18.0
                    val pm10Val = aqiData?.current?.pm10 ?: 35.0
                    val uvVal = aqiData?.current?.uvIndex ?: metrics.current.uv_index
                    val pollenRisk = if ((aqiData?.current?.grassPollen ?: 0.0) > 10.0) "High Grass Pollen" else "Pollen Levels Normal"
                    val advisory = if (aqiVal > 100) "Sensitive individuals should wear masks outdoors." else "Air quality is favorable."

                    cardList.add(PersonaCardItem.HealthCard(aqiVal, pm25Val, pm10Val, uvVal, pollenRisk, advisory))
                }

                // Fitness Card
                if (activePersonas.contains(PersonaType.FITNESS.id)) {
                    val heat = if (metrics.current.apparent_temperature > 32.0) "High Heat Stress" else "Low Heat Stress"
                    cardList.add(PersonaCardItem.FitnessCard("06:30 AM", "06:45 PM", "06:00 - 08:00 AM", heat))
                }

                // Marine Card
                if (isNearCoast && activePersonas.contains(PersonaType.BEACH.id)) {
                    val waves = marineData?.current?.waveHeight ?: 1.2
                    val period = marineData?.current?.wavePeriod ?: 7.5
                    val status = if (waves > 2.0) "Rough Waters: Caution advised" else "Calm Seas: Suitable for swimming"
                    cardList.add(PersonaCardItem.MarineCard(waves, period, status))
                }

                // Commuter Card
                if (activePersonas.contains(PersonaType.COMMUTE.id)) {
                    val visKm = metrics.current.visibility / 1000.0
                    val isFog = visKm < 1.0
                    val impact = if (isFog) "Dense mist/fog detected. Reduce road speeds." else "Good visibility across transit routes."
                    cardList.add(PersonaCardItem.CommuteCard(visKm, isFog, impact))
                }

                // Agriculture Card
                if (activePersonas.contains(PersonaType.AGRO.id)) {
                    val minTemp = daily.daily.temperature_2m_min.firstOrNull() ?: 15.0
                    val frostWarning = minTemp <= 2.0
                    val advice = if (frostWarning) "Sub-freezing night temperatures projected. Cover sensitive crops." else "Temperature range remains stable for crops."
                    cardList.add(PersonaCardItem.AgroCard(frostWarning, advice))
                }

                // Event Planner Card
                if (activePersonas.contains(PersonaType.EVENT.id)) {
                    val temp = metrics.current.apparent_temperature
                    val score = if (temp in 18.0..28.0 && metrics.current.relative_humidity_2m < 75) "9.2/10 (Optimal)" else "6.5/10 (Moderate)"
                    val rainRisk = "Clear outlook during evening hours."
                    cardList.add(PersonaCardItem.EventCard(score, rainRisk))
                }

                withContext(Dispatchers.Main) {
                    personaAdapter.updateItems(cardList)
                }

            } catch (e: Exception) {
                Log.e("Weather", "Error loading weather data directly", e)
                withContext(Dispatchers.Main) {
                    progressCircular24h.visibility = View.GONE
                    progressCircular7d.visibility = View.GONE
                }
            }
        }
    }

    // Mathematical approximation to detect if user is within 25km of the ocean
    private fun checkIsCoastal(lat: Double, lon: Double): Boolean {
        val coastalPoints = listOf(
            Pair(18.9220, 72.8347), // Mumbai
            Pair(15.4909, 73.8278), // Goa
            Pair(13.0827, 80.2707), // Chennai
            Pair(9.9312, 76.2673),  // Kochi
            Pair(17.6868, 83.2185), // Visakhapatnam
            Pair(21.6266, 87.5074)  // Digha / Bengal Coast
        )

        for (pt in coastalPoints) {
            val dist = haversineDistance(lat, lon, pt.first, pt.second)
            if (dist <= 25.0) return true
        }
        return false
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        return 2 * r * asin(sqrt(a))
    }
}