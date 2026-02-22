package com.michaelzhou;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class App {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    // Your OpenSky credentials
    private static final String USERNAME = "michaelzhou1008@gmail.com";
    private static final String PASSWORD = "Zzq20051008";

    // Global request interval
    private static final int REFRESH_INTERVAL = 30000;

    public static void main(String[] args) throws Exception {

        while (true) {
            runSnapshot();
            System.out.println("\nRefreshing in " + (REFRESH_INTERVAL / 1000) + " seconds...");
            Thread.sleep(REFRESH_INTERVAL);
        }
    }

    public static void runSnapshot() throws Exception {

        String url = "https://opensky-network.org/api/states/all";

        
        String auth = USERNAME + ":" + PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Authorization", "Basic " + encodedAuth)
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        // Handle rate limiting
        if (response.statusCode() == 429) {
            System.out.println("Rate limit exceeded. Sleeping 60 seconds...");
            Thread.sleep(60000);
            return;
        }

        if (response.statusCode() == 401) {
            System.out.println("Unauthorized: check your username/password and make sure your account is verified.");
            return;
        }

        if (response.statusCode() != 200) {
            System.out.println("API Error: " + response.statusCode());
            System.out.println("Response: " + response.body());
            return;
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode states = root.get("states");

        if (states == null) {
            System.out.println("No aircraft data returned.");
            return;
        }

        int totalAircraft = states.size();
        int airborneCount = 0;
        int canadaCount = 0;
        double totalAltitudeFeet = 0;

        Map<String, Integer> countryCount = new HashMap<>();

        for (JsonNode flight : states) {
            if (flight.get(7).isNull()) continue;

            String country = flight.get(2).asText();
            double altitudeMeters = flight.get(7).asDouble();
            double altitudeFeet = altitudeMeters * 3.28084;

            airborneCount++;
            totalAltitudeFeet += altitudeFeet;

            countryCount.put(country,
                    countryCount.getOrDefault(country, 0) + 1);

            if (country.equalsIgnoreCase("Canada")) {
                canadaCount++;
            }
        }

        double avgAltitude = airborneCount == 0 ? 0 :
                totalAltitudeFeet / airborneCount;

        clearConsole();

        System.out.println("===== GLOBAL AVIATION SNAPSHOT =====");
        System.out.println("Last Updated: " + java.time.LocalTime.now());
        System.out.println("------------------------------------");

        System.out.println("Total Aircraft Detected: " + totalAircraft);
        System.out.println("Airborne Aircraft: " + airborneCount);
        System.out.println("Aircraft Over Canada: " + canadaCount);
        System.out.printf("Average Altitude: %.0f ft%n", avgAltitude);

        System.out.println("\nTop 5 Countries by Aircraft Count:");

        countryCount.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(5)
                .forEach(entry ->
                        System.out.println(entry.getKey() + ": " + entry.getValue()));
    }

    public static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}