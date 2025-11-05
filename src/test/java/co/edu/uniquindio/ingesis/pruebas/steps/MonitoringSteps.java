package co.edu.uniquindio.ingesis.pruebas.steps;

import co.edu.uniquindio.ingesis.pruebas.support.Endpoints;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import io.restassured.http.ContentType;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class MonitoringSteps {

    private String monBase;

    @Before("@monitoring")
    public void setupMonitoringBase() {
        monBase = Endpoints.MON(); // http://monitoring-app:8083 o lo que inyectes
    }

    @Given("el servicio de monitoreo está disponible")
    public void monitoring_up() {
        given().when().get(monBase + "/health").then().statusCode(200);
    }

    @When("registro para monitoreo el servicio {string}")
    public void registro_servicio(String name) {
        Map<String, Object> body = Map.of(
                "serviceName", name,
                "host", "auth-app",
                "port", 8080,
                "healthEndpoint", "/actuator/health",
                "emails", List.of("admin@test.com")
        );

        given().contentType(ContentType.JSON).body(body)
                .when().post(monBase + "/monitor/register")
                .then().statusCode(anyOf(is(200), is(201)));
    }

    @Then("listar la salud global retorna arreglo")
    public void listar_salud_global() {
        given().when().get(monBase + "/monitor/health")
                .then().statusCode(200).body("$", isA(List.class));
    }

    @When("consulto la salud por nombre {string}")
    public void consulto_salud_nombre(String name) {
        given().when().get(monBase + "/monitor/health/" + name)
                .then().statusCode(200).body("name", equalTo(name));
    }
}
