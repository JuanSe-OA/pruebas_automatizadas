package co.edu.uniquindio.ingesis.pruebas.support;

public class Endpoints {
    public static String AUTH() {
        return System.getProperty("AUTH_URL",
                System.getenv().getOrDefault("AUTH_URL", "http://auth-app:8080"));
    }
    public static String MON() {
        // si no te pasan MON_URL, usa BASE_URL y si no, un default
        String base = System.getProperty("MON_URL", System.getenv("MON_URL"));
        if (base == null || base.isBlank()) {
            base = System.getProperty("BASE_URL",
                    System.getenv().getOrDefault("BASE_URL", "http://monitoring-app:8084"));
        }
        return base;
    }
}
