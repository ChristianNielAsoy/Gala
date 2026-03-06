/**
 * Fetch weather data for a given location using Open-Meteo (free, no API key).
 * @param {string} location - The location (city name) to fetch weather for.
 * @returns {Promise<{forecast: string; temperature: number; icon: string}>} - The weather data.
 */
export async function fetchWeather(
  location: string,
): Promise<{ forecast: string; temperature: number; icon: string }> {
  try {
    // First, get coordinates from location (Open-Meteo needs lat/lon)
    const geoResponse = await fetch(
      `https://geocoding-api.open-meteo.com/v1/search?name=${encodeURIComponent(
        location,
      )}&count=1&language=en&format=json`,
    );
    const geoData = await geoResponse.json();
    if (!geoData.results || geoData.results.length === 0) {
      throw new Error('Location not found');
    }
    const { latitude, longitude } = geoData.results[0];

    // Fetch weather data
    const weatherResponse = await fetch(
      `https://api.open-meteo.com/v1/forecast?latitude=${latitude}&longitude=${longitude}&current=temperature_2m,weather_code&timezone=auto`,
    );
    const weatherData = await weatherResponse.json();

    // Map weather code to description
    const weatherCodes: Record<number, string> = {
      0: 'Clear sky',
      1: 'Mainly clear',
      2: 'Partly cloudy',
      3: 'Overcast',
      45: 'Fog',
      48: 'Depositing rime fog',
      51: 'Light drizzle',
      53: 'Moderate drizzle',
      55: 'Dense drizzle',
      61: 'Slight rain',
      63: 'Moderate rain',
      65: 'Heavy rain',
      66: 'Light freezing rain',
      67: 'Heavy freezing rain',
      71: 'Slight snow fall',
      73: 'Moderate snow fall',
      75: 'Heavy snow fall',
      77: 'Snow grains',
      80: 'Slight rain showers',
      81: 'Moderate rain showers',
      82: 'Violent rain showers',
      85: 'Slight snow showers',
      86: 'Heavy snow showers',
      95: 'Thunderstorm',
      96: 'Thunderstorm with slight hail',
      99: 'Thunderstorm with heavy hail',
    };
    const forecast = weatherCodes[weatherData.current.weather_code] || 'Unknown';

    return {
      forecast,
      temperature: weatherData.current.temperature_2m,
      icon: `https://openweathermap.org/img/wn/${getIconFromCode(
        weatherData.current.weather_code,
      )}@2x.png`, // Reuse OpenWeather icons for simplicity
    };
  } catch (error) {
    console.error('Error fetching weather:', error);
    throw new Error('Unable to fetch weather data.');
  }
}

// Helper to map weather codes to OpenWeather icon codes (approximate)
function getIconFromCode(code: number): string {
  if (code === 0) return '01d'; // Clear
  if (code <= 3) return '02d'; // Cloudy
  if (code >= 45 && code <= 48) return '50d'; // Fog
  if (code >= 51 && code <= 55) return '09d'; // Drizzle
  if (code >= 61 && code <= 67) return '10d'; // Rain
  if (code >= 71 && code <= 77) return '13d'; // Snow
  if (code >= 80 && code <= 86) return '09d'; // Showers
  if (code >= 95) return '11d'; // Thunderstorm
  return '01d'; // Default
}
