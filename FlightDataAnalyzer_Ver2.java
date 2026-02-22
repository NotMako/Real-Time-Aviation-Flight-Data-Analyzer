package com.michaelzhou;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class App {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final double YVR_LAT = 49.1967;
    private static final double YVR_LON = -123.1815;

    private static final Map<String, Double> trackedAircraft = new HashMap<>();

    public static void main(String[] args) throws Exception {

        while (true) {
            runSnapshot();
            Thread.sleep(5000);
        }
    }

    public static void runSnapshot() throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "https://opensky-network.org/api/states/all" +
                        "?lamin=49.0&lomin=-123.5&lamax=49.5&lomax=-122.5"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        clearConsole();

        if (response.statusCode() != 200) {
            System.out.println("API Error: " + response.statusCode());
            System.out.println(response.body());
            return;
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode states = root.get("states");

        System.out.println("===== YVR AIRSPACE MONITOR =====");
        System.out.println("Last Updated: " + LocalTime.now());
        System.out.println("---------------------------------\n");

        if (states == null || states.size() == 0) {
            System.out.println("No aircraft detected.");
            return;
        }

        Map<String, Double> currentSnapshot = new HashMap<>();

        for (JsonNode flight : states) {

            JsonNode altitudeNode = flight.get(7);
            JsonNode latNode = flight.get(6);
            JsonNode lonNode = flight.get(5);
            JsonNode onGroundNode = flight.get(8);

            if (altitudeNode == null || altitudeNode.isNull()) continue;
            if (latNode == null || latNode.isNull()) continue;
            if (lonNode == null || lonNode.isNull()) continue;
            if (onGroundNode != null && onGroundNode.asBoolean()) continue;

            double altitudeMeters = altitudeNode.asDouble();
            if (altitudeMeters <= 0) continue;

            double altitudeFeet = altitudeMeters * 3.28084;
            double lat = latNode.asDouble();
            double lon = lonNode.asDouble();

            String icao = flight.get(0).asText();
            String callsign = flight.get(1).asText().trim();

            double distance = haversine(lat, lon, YVR_LAT, YVR_LON);

            currentSnapshot.put(icao, altitudeFeet);

            System.out.printf(
                    "Callsign: %-10s | Alt: %6.0f ft | Dist: %5.1f km%n",
                    callsign.isEmpty() ? "N/A" : callsign,
                    altitudeFeet,
                    distance
            );
        }

        trackedAircraft.clear();
        trackedAircraft.putAll(currentSnapshot);

        System.out.println("\nCurrently Tracking: " + trackedAircraft.size() + " aircraft");
    }

    public static double haversine(double lat1, double lon1,
                                   double lat2, double lon2) {

        final int R = 6371;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    public static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}