package com.michaelzhou;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class App {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    // YVR Coordinates
    private static final double YVR_LAT = 49.1967;
    private static final double YVR_LON = -123.1815;

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

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        clearConsole();

        if (response.statusCode() != 200) {
            System.out.println("API Error: " + response.statusCode());
            return;
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode states = root.get("states");

        System.out.println("========= YVR AIRSPACE MONITOR =========");
        System.out.println("Last Updated: " + LocalTime.now().withNano(0));
        System.out.println("========================================\n");

        if (states == null || states.size() == 0) {
            System.out.println("No aircraft detected.");
            return;
        }

        Map<String, String> airborne = new HashMap<>();
        Map<String, String> ground = new HashMap<>();

        for (JsonNode flight : states) {

            if (flight.get(7).isNull() ||
                flight.get(6).isNull() ||
                flight.get(5).isNull() ||
                flight.get(9).isNull())
                continue;

            String callsign = flight.get(1).asText().trim();
            if (callsign.isEmpty()) callsign = "N/A";

            double lat = flight.get(6).asDouble();
            double lon = flight.get(5).asDouble();
            double altitudeFeet = flight.get(7).asDouble() * 3.28084;
            double speedKnots = flight.get(9).asDouble() * 1.94384;

            if (altitudeFeet < -100 || speedKnots < 0)
                continue;

            double distance = haversine(lat, lon, YVR_LAT, YVR_LON);

            if (distance > 50)
                continue;

            String line = String.format(
                    "%-10s | %6.0f ft | %5.0f kt | %5.1f km",
                    callsign,
                    altitudeFeet,
                    speedKnots,
                    distance
            );

            if (altitudeFeet > 500 && speedKnots > 50) {
                airborne.put(callsign, line);
            }
            else if (distance <= 10) {
                ground.put(callsign, line);
            }
        }

        System.out.println("--- AIRBORNE (within 50km) ---");
        airborne.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> System.out.println(entry.getValue()));

        System.out.println();

        System.out.println("--- ON GROUND (within 10km) ---");
        ground.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> System.out.println(entry.getValue()));

        System.out.println();
        System.out.println("Airborne: " + airborne.size());
        System.out.println("On Ground: " + ground.size());
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
