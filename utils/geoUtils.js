// utils/geoUtils.js

// Sample coastal points (Latitude, Longitude) for India's major beaches/coasts
const COASTAL_POINTS = [
    { lat: 19.0760, lon: 72.8777 }, // Mumbai
    { lat: 15.2993, lon: 74.1240 }, // Goa
    { lat: 13.0827, lon: 80.2707 }, // Chennai
    { lat: 9.9312,  lon: 76.2673 }, // Kochi
    { lat: 17.6868, lon: 83.2185 }  // Visakhapatnam
];

function haversineDistance(lat1, lon1, lat2, lon2) {
    const toRad = (value) => (value * Math.PI) / 180;
    const R = 6371; // Earth radius in kilometers

    const dLat = toRad(lat2 - lat1);
    const dLon = toRad(lon2 - lon1);

    const a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
              Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
              Math.sin(dLon / 2) * Math.sin(dLon / 2);

    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

function checkIsCoastal(lat, lon, thresholdKm = 25) {
    for (const point of COASTAL_POINTS) {
        if (haversineDistance(lat, lon, point.lat, point.lon) <= thresholdKm) {
            return true;
        }
    }
    return false;
}

module.exports = { checkIsCoastal };