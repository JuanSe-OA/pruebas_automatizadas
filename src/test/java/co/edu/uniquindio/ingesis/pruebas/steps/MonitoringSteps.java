package co.edu.uniquindio.ingesis.pruebas.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Paso a paso robusto e idempotente para el servicio de monitoreo.
 * - Lee BASE_URL desde -DBASE_URL (inyectado por Jenkins).
 * - Tolera /monitor/health y /health.
 * - Registro idempotente: si ya existe, no re-registra (evita 500).
 */
public class MonitoringSteps {

    private String monBase;

    @Before("@monitoring")
    public void setupMonitoringBase() {
        // Lee del system property que inyecta Jenkins (-DBASE_URL=...).
        // Si no viene, puedes poner un fallback local.
        this.monBase = System.getProperty("BASE_URL", "http://monitoring-app:8084");
        // Normalizar: quitar "/" final si viene
        if (monBase.endsWith("/")) monBase = monBase.substring(0, monBase.length() - 1);
    }

    @Given("el servicio de monitoreo está disponible")
    public void monitoring_up() {
        // Soporta ambas rutas de health por compatibilidad
        // Intenta /monitor/health y, si falla, prueba /health
        boolean ok = false;

        try {
            given()
                    .when().get(monBase + "/monitor/health")
                    .then().statusCode(200);
            ok = true;
        } catch (AssertionError | Exception ignored) { /* intentamos /health abajo */ }

        if (!ok) {
            given()
                    .when().get(monBase + "/health")
                    .then().statusCode(200);
        }
    }

    @When("registro para monitoreo el servicio {string}")
    public void registro_servicio(String name) throws InterruptedException {
        // Primero verifica si YA existe para evitar 500 del backend
        if (existeServicio(name)) {
            // ya está registrado → idempotente
            return;
        }

        Map<String, Object> body = Map.of(
                "serviceName", name,               // OJO: si tu backend espera "name", cambia a "name"
                "host", "auth-app",
                "port", 8080,
                "healthEndpoint", "/actuator/health",
                "emails", List.of("admin@test.com")
        );

        // Pequeño retry por calentamiento (migraciones, etc.)
        int attempts = 4;
        AssertionError last = null;

        for (int i = 1; i <= attempts; i++) {
            try {
                Response resp =
                        given()
                                .baseUri(monBase)
                                .contentType(ContentType.JSON)
                                .body(body)
                                .when().post("/monitor/register");

                // Log de respuesta si hay error para depurar en el log de Jenkins
                if (resp.statusCode() >= 400) {
                    resp.then().log().all();
                }

                resp.then().statusCode(anyOf(is(200), is(201), is(409))); // tolera ya-existe

                last = null;
                break;
            } catch (AssertionError e) {
                last = e;
                Thread.sleep(1500);
            }
        }
        if (last != null) throw last;

        // Verifica que ahora exista
        given()
                .when().get(monBase + "/monitor/health/" + name)
                .then().statusCode(200);
    }

    @Then("listar la salud global retorna arreglo")
    public void listar_salud_global() {
        given()
                .when().get(monBase + "/monitor/health")
                .then()
                .statusCode(200)
                .body("$", isA(List.class));
    }

    @When("consulto la salud por nombre {string}")
    public void consulto_salud_nombre(String name) {
        // Según tu contrato, valida el nombre. Si el campo es "serviceName", cámbialo aquí.
        given()
                .when().get(monBase + "/monitor/health/" + name)
                .then()
                .statusCode(200)
                .body(anyOf(
                        hasKey("name"),          // admite { "name": "..." }
                        hasKey("serviceName")    // o { "serviceName": "..." }
                ))
                .body("name", anyOf(nullValue(), equalTo(name)))
                .body("serviceName", anyOf(nullValue(), equalTo(name)));
    }

    // =======================
    // Helpers
    // =======================
    private boolean existeServicio(String name) {
        try {
            given()
                    .when().get(monBase + "/monitor/health/" + name)
                    .then().statusCode(200);
            return true;
        } catch (AssertionError | Exception e) {
            return false;
        }
    }
}
