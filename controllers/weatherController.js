// controllers/weatherController.js
const { checkIsCoastal } = require('../utils/geoUtils');
const { fetchForecast, fetchAirQuality, fetchMarine } = require('../services/personaWeatherService');

// Steadman's Apparent Temperature (Simplified Comfort Index 1-10)
function calculateComfortIndex(temp, humidity, wind) {
    const e = (humidity / 100) * 6.105 * Math.exp((17.27 * temp) / (237.7 + temp));
    const apparentTemp = temp + 0.33 * e - 0.70 * (wind / 3.6) - 4.00;

    // Map AT to a 1-10 comfort scale (ideal is around 20-25 C)
    let score = 10 - Math.abs(apparentTemp - 22) / 3;
    return Math.max(1, Math.min(10, Math.round(score * 10) / 10));
}

// Find best running hours today
function getBestRunningHours(hourlyForecast, currentAqi) {
    const bestHours = [];
    // Check next 12 hours
    for (let i = 0; i < 12; i++) {
        const temp = hourlyForecast.temperature_2m[i];
        const wind = hourlyForecast.wind_speed_10m[i];
        const rainProb = hourlyForecast.precipitation_probability[i];
        const time = new Date(hourlyForecast.time[i]).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});

        if (temp >= 10 && temp <= 25 && wind < 20 && rainProb < 15 && currentAqi <= 100) {
            bestHours.push(time);
        }
    }
    return bestHours.length > 0 ? [`${bestHours[0]} - ${bestHours[bestHours.length - 1]}`] : ["No ideal slots today"];
}

async function getPersonaInsights(req, res) {
    try {
        const lat = parseFloat(req.query.lat);
        const lon = parseFloat(req.query.lon);
        const personasQuery = req.query.personas || "health";
        const requestedPersonas = personasQuery.split(',').map(p => p.trim());

        if (isNaN(lat) || isNaN(lon)) {
            return res.status(400).json({ error: "Invalid coordinates" });
        }

        const isCoastal = checkIsCoastal(lat, lon);

        // Fetch APIs concurrently
        const apiCalls = [
            fetchForecast(lat, lon),
            fetchAirQuality(lat, lon)
        ];

        if (isCoastal && requestedPersonas.includes('beach')) {
            apiCalls.push(fetchMarine(lat, lon));
        }

        const [forecast, aqiData, marineData] = await Promise.all(apiCalls);

        // --- NEW: General Weather Metrics ---
        const generalWeather = {
            windSpeed: forecast.current.wind_speed_10m,
            humidity: forecast.current.relative_humidity_2m,
            visibility: forecast.current.visibility,
            uvIndex: aqiData.current.uv_index,
            feelsLike: forecast.current.apparent_temperature,
            pressure: forecast.current.pressure_msl,
            hourly: forecast.hourly,
            daily: forecast.daily
        };

        // Base Location Meta
        const response = {
            location: { lat, lon, isCoastal },
            activePersonas: requestedPersonas,
            generalWeather: generalWeather
        };

        // 1. HEALTH PERSONA
        if (requestedPersonas.includes('health')) {
            const currentAqi = aqiData.current.us_aqi || 50;
            response.health = {
                aqi: currentAqi,
                pm25: aqiData.current.pm2_5,
                pm10: aqiData.current.pm10,
                uvIndex: aqiData.current.uv_index,
                humidity: forecast.current.relative_humidity_2m,
                dominantPollen: aqiData.current.grass_pollen > 10 ? "Grass" : "Low",
                healthRiskAdvisory: currentAqi > 100 ? "Sensitive individuals should mask up." : "Air quality is favorable."
            };
        }

        // 2. FITNESS PERSONA
        if (requestedPersonas.includes('fitness')) {
            response.fitness = {
                sunrise: new Date(forecast.daily.sunrise[0]).toLocaleTimeString(),
                sunset: new Date(forecast.daily.sunset[0]).toLocaleTimeString(),
                bestRunningHours: getBestRunningHours(forecast.hourly, aqiData.current.us_aqi),
                currentWindSpeedKmH: forecast.current.wind_speed_10m,
                heatStressLevel: forecast.current.temperature_2m > 32 ? "High" : "Low"
            };
        }

        // 3. COMMUTE PERSONA
        if (requestedPersonas.includes('commute') || requestedPersonas.includes('parent')) {
            // Check morning visibility (approx index 7-9 depending on timezone)
            const morningVis = forecast.hourly.visibility[8] || 10000;
            const isFog = morningVis < 1000;

            const commuteData = {
                visibilityMeters: morningVis,
                isFogAlert: isFog,
                commuteSlotImpact: isFog ? "Delays expected due to morning fog." : "Clear routes expected."
            };

            if (requestedPersonas.includes('commute')) response.commute = commuteData;
            if (requestedPersonas.includes('parent')) {
                response.parent = {
                    morningSchoolWindowRisk: commuteData.commuteSlotImpact,
                    afternoonPickUpRisk: forecast.hourly.precipitation_probability[15] > 40 ? "Rain likely, carry umbrellas." : "Clear",
                    extremeColdHeatAlert: null
                };
            }
        }

        // 4. AGRO PERSONA
        if (requestedPersonas.includes('agro')) {
            // Check for frost in next 24h
            const minTemp = Math.min(...forecast.hourly.temperature_2m.slice(0, 24));
            response.agriculture = {
                soilMoistureM3: forecast.hourly.soil_moisture_3_to_9cm[0] || 0,
                soilTemperatureC: forecast.hourly.soil_temperature_6cm[0] || 0,
                frostWarning: minTemp <= 2,
                evapotranspirationMm: forecast.daily.et0_fao_evapotranspiration[0] || 0,
                irrigationAdvice: forecast.hourly.precipitation_probability[0] > 50 ? "Rain expected, pause irrigation." : "Standard irrigation schedule applies."
            };
        }

        // 5. EVENT PLANNER PERSONA
        if (requestedPersonas.includes('event')) {
            response.eventPlanner = {
                comfortIndex: calculateComfortIndex(forecast.current.temperature_2m, forecast.current.relative_humidity_2m, forecast.current.wind_speed_10m),
                rainProbabilityDay: Math.max(...forecast.hourly.precipitation_probability.slice(0, 24)),
                outdoorViability: forecast.current.temperature_2m > 15 && forecast.current.temperature_2m < 30 ? "High" : "Moderate",
            };
        }

        // 6. MARINE/BEACH PERSONA
        if (requestedPersonas.includes('beach') && isCoastal && marineData) {
            response.marine = {
                waveHeightMeters: marineData.current.wave_height,
                wavePeriodSec: marineData.current.wave_period,
                waveDirection: marineData.current.wave_direction,
                tideStatus: marineData.current.wave_height > 2.0 ? "High/Rough Surf" : "Calm"
            };
        }

        res.json(response);

    } catch (error) {
        console.error("Error generating insights:", error.message);
        res.status(500).json({ error: "Failed to generate persona insights." });
    }
}

module.exports = { getPersonaInsights };