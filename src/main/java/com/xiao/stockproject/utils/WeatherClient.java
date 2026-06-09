package com.xiao.stockproject.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class WeatherClient {

    private static final int TIMEOUT_SECONDS = 15;

    public static class WeatherInfo {
        private String city;
        private String weather;
        private String temperature;
        private String humidity;
        private String wind;
        private String airQuality;
        private String updateTime;

        public String getCity() { return city; }
        public String getWeather() { return weather; }
        public String getTemperature() { return temperature; }
        public String getHumidity() { return humidity; }
        public String getWind() { return wind; }
        public String getAirQuality() { return airQuality; }
        public String getUpdateTime() { return updateTime; }

        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("【").append(city).append("】实时天气\n");
            sb.append("天气状况: ").append(weather).append("\n");
            sb.append("温度: ").append(temperature).append("\n");
            sb.append("湿度: ").append(humidity).append("\n");
            sb.append("风力风向: ").append(wind).append("\n");
            if (airQuality != null && !airQuality.isEmpty()) {
                sb.append("空气质量: ").append(airQuality).append("\n");
            }
            sb.append("更新时间: ").append(updateTime).append("\n");
            return sb.toString();
        }
    }

    private CloseableHttpClient createHttpClient() {
        return HttpClients.custom()
            .setSSLContext(createInsecureSSLContext())
            .build();
    }

    private javax.net.ssl.SSLContext createInsecureSSLContext() {
        try {
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                }
            };
            javax.net.ssl.SSLContext ssl = javax.net.ssl.SSLContext.getInstance("TLS");
            ssl.init(null, trustAllCerts, new java.security.SecureRandom());
            return ssl;
        } catch (Exception e) {
            log.error("Failed to create SSL context", e);
            throw new RuntimeException(e);
        }
    }

    public WeatherInfo getWeather(String city) {
        try {
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                + java.net.URLEncoder.encode(city, StandardCharsets.UTF_8.name())
                + "&count=1&language=zh&format=json";

            try (CloseableHttpClient client = createHttpClient()) {
                HttpGet geoRequest = new HttpGet(geoUrl);
                geoRequest.setConfig(RequestConfig.custom()
                        .setConnectTimeout(TIMEOUT_SECONDS * 1000)
                        .setSocketTimeout(TIMEOUT_SECONDS * 1000)
                        .build());

                String geoResponse = EntityUtils.toString(client.execute(geoRequest).getEntity(), StandardCharsets.UTF_8);
                log.info("Geo response: {}", geoResponse);

                com.alibaba.fastjson.JSONObject geoJson = com.alibaba.fastjson.JSON.parseObject(geoResponse);
                com.alibaba.fastjson.JSONArray results = geoJson.getJSONArray("results");

                if (results == null || results.isEmpty()) {
                    log.warn("City not found: {}", city);
                    return createEmptyWeatherInfo(city);
                }

                double lat = results.getJSONObject(0).getDouble("latitude");
                double lon = results.getJSONObject(0).getDouble("longitude");
                String geoName = results.getJSONObject(0).getString("name");

                String weatherUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m,wind_direction_10m&timezone=Asia/Shanghai",
                    lat, lon);

                HttpGet weatherRequest = new HttpGet(weatherUrl);
                weatherRequest.setConfig(RequestConfig.custom()
                        .setConnectTimeout(TIMEOUT_SECONDS * 1000)
                        .setSocketTimeout(TIMEOUT_SECONDS * 1000)
                        .build());

                String weatherResponse = EntityUtils.toString(client.execute(weatherRequest).getEntity(), StandardCharsets.UTF_8);
                log.info("Weather response: {}", weatherResponse);

                return parseOpenMeteoResponse(geoName, weatherResponse);
            }
        } catch (Exception e) {
            log.error("获取天气信息失败: city={}, error={}", city, e.getMessage(), e);
            return createEmptyWeatherInfo(city);
        }
    }

    public String getWeatherForecast(String city) {
        try {
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                + java.net.URLEncoder.encode(city, StandardCharsets.UTF_8.name())
                + "&count=1&language=zh&format=json";

            try (CloseableHttpClient client = createHttpClient()) {
                HttpGet geoRequest = new HttpGet(geoUrl);
                geoRequest.setConfig(RequestConfig.custom()
                        .setConnectTimeout(TIMEOUT_SECONDS * 1000)
                        .setSocketTimeout(TIMEOUT_SECONDS * 1000)
                        .build());

                String geoResponse = EntityUtils.toString(client.execute(geoRequest).getEntity(), StandardCharsets.UTF_8);
                com.alibaba.fastjson.JSONObject geoJson = com.alibaba.fastjson.JSON.parseObject(geoResponse);
                com.alibaba.fastjson.JSONArray results = geoJson.getJSONArray("results");

                if (results == null || results.isEmpty()) {
                    return "城市未找到: " + city;
                }

                double lat = results.getJSONObject(0).getDouble("latitude");
                double lon = results.getJSONObject(0).getDouble("longitude");
                String geoName = results.getJSONObject(0).getString("name");

                String weatherUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&daily=weather_code,temperature_2m_max,temperature_2m_min&timezone=Asia/Shanghai",
                    lat, lon);

                HttpGet weatherRequest = new HttpGet(weatherUrl);
                weatherRequest.setConfig(RequestConfig.custom()
                        .setConnectTimeout(TIMEOUT_SECONDS * 1000)
                        .setSocketTimeout(TIMEOUT_SECONDS * 1000)
                        .build());

                String weatherResponse = EntityUtils.toString(client.execute(weatherRequest).getEntity(), StandardCharsets.UTF_8);
                return formatForecastResponse(geoName, weatherResponse);
            }
        } catch (Exception e) {
            log.error("获取天气预报失败: city={}, error={}", city, e.getMessage());
            return "获取天气预报失败: " + e.getMessage();
        }
    }

    private WeatherInfo parseOpenMeteoResponse(String city, String response) {
        WeatherInfo info = new WeatherInfo();
        info.city = city;

        try {
            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(response);
            com.alibaba.fastjson.JSONObject current = json.getJSONObject("current");

            if (current != null) {
                double temp = current.getDoubleValue("temperature_2m");
                info.temperature = temp + "C";
                info.humidity = current.getIntValue("relative_humidity_2m") + "%";
                info.wind = current.getDoubleValue("wind_speed_10m") + " km/h " + getWindDirection(current.getIntValue("wind_direction_10m"));

                int weatherCode = current.getIntValue("weather_code");
                info.weather = getWeatherDescription(weatherCode);
                info.updateTime = java.time.LocalDateTime.now().toString();
                info.airQuality = "";
            }
        } catch (Exception e) {
            log.debug("解析天气响应失败: {}", e.getMessage());
            info.weather = "未知";
            info.temperature = "--";
            info.humidity = "--";
            info.wind = "--";
            info.updateTime = "未知";
            info.airQuality = "";
        }

        return info;
    }

    private String getWeatherDescription(int code) {
        switch (code) {
            case 0: return "晴朗";
            case 1: case 2: case 3: return "多云";
            case 45: case 48: return "雾";
            case 51: case 53: case 55: return "毛毛雨";
            case 56: case 57: return "冻毛毛雨";
            case 61: case 63: case 65: return "小雨";
            case 66: case 67: return "冻雨";
            case 71: case 73: case 75: return "雪";
            case 77: return "雪粒";
            case 80: case 81: case 82: return "阵雨";
            case 85: case 86: return "阵雪";
            case 95: return "雷暴";
            case 96: case 99: return "雷暴加冰雹";
            default: return "未知(" + code + ")";
        }
    }

    private String getWindDirection(int degrees) {
        String[] directions = {"北", "东北", "东", "东南", "南", "西南", "西", "西北"};
        int index = (int) Math.round(degrees / 45.0) % 8;
        return directions[index];
    }

    private String formatForecastResponse(String city, String response) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(city).append("】天气预报\n\n");

        try {
            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(response);
            com.alibaba.fastjson.JSONObject daily = json.getJSONObject("daily");

            if (daily != null) {
                com.alibaba.fastjson.JSONArray dates = daily.getJSONArray("time");
                com.alibaba.fastjson.JSONArray maxTemps = daily.getJSONArray("temperature_2m_max");
                com.alibaba.fastjson.JSONArray minTemps = daily.getJSONArray("temperature_2m_min");
                com.alibaba.fastjson.JSONArray weatherCodes = daily.getJSONArray("weather_code");

                for (int i = 0; i < dates.size() && i < 7; i++) {
                    String date = dates.getString(i);
                    String maxTemp = maxTemps.getString(i);
                    String minTemp = minTemps.getString(i);
                    int code = weatherCodes.getIntValue(i);

                    sb.append(date).append(": ").append(getWeatherDescription(code))
                      .append(", 最高").append(maxTemp).append("C, 最低").append(minTemp).append("C\n");
                }
            }
        } catch (Exception e) {
            sb.append("无法获取详细的天气预报，请稍后重试。\n");
        }

        return sb.toString();
    }

    private WeatherInfo createEmptyWeatherInfo(String city) {
        WeatherInfo info = new WeatherInfo();
        info.city = city;
        info.weather = "获取失败";
        info.temperature = "--";
        info.humidity = "--";
        info.wind = "--";
        info.updateTime = "未知";
        return info;
    }

    public String getWeatherString(String city) {
        WeatherInfo info = getWeather(city);
        return info.toFormattedString();
    }
}