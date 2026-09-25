// services/personaWeatherService.js
const axios = require('axios');

const BASE_FORECAST_URL = 'https://api.open-meteo.com/v1/forecast';
const BASE_AQI_URL = 'https://air-quality-api.open-meteo.com/v1/air-quality';
const BASE_MARINE_URL = 'https://marine-api.open-meteo.com/v1/marine';

async function fetchForecast(lat, lon) {
    const url = `${BASE_FORECAST_URL}?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,wind_speed_10m&hourly=temperature_2m,precipitation_probability,visibility,soil_temperature_6cm,soil_moisture_3_to_9cm,wind_speed_10m&daily=sunrise,sunset,et0_fao_evapotranspiration&timezone=auto`;
    const response = await axios.get(url);
    return response.data;
}

async function fetchAirQuality(lat, lon) {
    const url = `${BASE_AQI_URL}?latitude=${lat}&longitude=${lon}&current=us_aqi,pm10,pm2_5,uv_index,birch_pollen,grass_pollen&timezone=auto`;
    const response = await axios.get(url);
    return response.data;
}

async function fetchMarine(lat, lon) {
    const url = `${BASE_MARINE_URL}?latitude=${lat}&longitude=${lon}&current=wave_height,wave_period,wave_direction&timezone=auto`;
    try {
        const response = await axios.get(url);
        return response.data;
    } catch (error) {
        // Fallback if the coordinate is slightly inland but flagged as coastal
        return null;
    }
}

module.exports = { fetchForecast, fetchAirQuality, fetchMarine };