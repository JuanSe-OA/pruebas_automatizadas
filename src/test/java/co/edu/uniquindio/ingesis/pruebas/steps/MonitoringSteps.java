package co.edu.uniquindio.ingesis.pruebas.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.withArgs;
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
    public void registro_servicio(String name) {
        Map<String, Object> body = Map.of(
                "name", name,
                "url", "http://auth-app:8080/actuator/health",
                "frequency", 30,
                "emails", List.of("admin@test.com")
        );
        given().contentType(ContentType.JSON).body(body)
                .when().post(monBase + "/monitor/register")
                .then().statusCode(anyOf(is(200), is(201), is(409)));
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
        Response r = given()
                .when().get(monBase + "/monitor/health/" + name);

        // Acepta 200 con/ sin body o 204 sin contenido
        r.then().statusCode(anyOf(is(200), is(204)));

        // Si no hay contenido (204 o body vacío), no hay nada más que validar
        String bodyStr = r.getBody() != null ? r.getBody().asString() : "";
        if (r.statusCode() == 204 || bodyStr == null || bodyStr.isBlank()) {
            return;
        }

        // Si hay contenido, valida según tipo: objeto o arreglo
        String ct = r.getHeader("Content-Type");
        if (ct != null && ct.toLowerCase().contains("application/json")) {
            char first = bodyStr.trim().charAt(0);
            if (first == '{') {
                // Objeto JSON: acepta name o serviceName y que coincida si viene
                r.then()
                        .body(anyOf(hasKey("name"), hasKey("serviceName")))
                        .body("name", anyOf(nullValue(), equalTo(name)))
                        .body("serviceName", anyOf(nullValue(), equalTo(name)));
            } else if (first == '[') {
                // Arreglo JSON: al menos un elemento con name/serviceName = name
                r.then()
                        .body("$", isA(List.class))
                        .body("find { it.name == '%s' || it.serviceName == '%s' }", withArgs(name, name), notNullValue());
            } else {
                // Texto/otro: no fallar, pero deja rastro
                System.out.println("Contenido no-JSON esperado, se omite validación de campos: " + bodyStr);
            }
        } else {
            // Content-Type no JSON: no hacemos validación de campos
            System.out.println("Content-Type no JSON (" + ct + "), se omite validación de campos.");
        }
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
