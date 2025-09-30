package co.edu.uniquindio.ingesis.pruebas.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import static org.junit.jupiter.api.Assertions.*;

public class AuthSteps {

    private Response lastResponse;
    private final String baseUrl = System.getProperty("base.url", "http://localhost:8080");

    @Given("el servicio de auth está disponible")
    public void elServicioEstaDisponible() {
        lastResponse = RestAssured.get(baseUrl + "/actuator/health");
        assertEquals(200, lastResponse.statusCode(), "El servicio no está disponible");
    }

    @When("registro un usuario válido")
    public void registroUsuario() {
        String body = """
            { "username":"cuke", "email":"cuke@example.com", "password":"1234", "phoneNumber":"+573001112233" }
            """;
        lastResponse = RestAssured.given()
                .header("Content-Type", "application/json")
                .body(body)
                .post(baseUrl + "/users");
    }

    @Then("la respuesta al registrar debe ser 200")
    public void validarRegistro() {
        assertEquals(200, lastResponse.statusCode(), "El registro falló");
    }

    @When("intento login con el usuario")
    public void intentoLogin() {
        String body = """
            { "username":"cuke", "password":"1234" }
            """;
        lastResponse = RestAssured.given()
                .header("Content-Type", "application/json")
                .body(body)
                .post(baseUrl + "/login");
    }

    @Then("la respuesta al login debe ser 200 y debe contener un token")
    public void validarLogin() {
        assertEquals(200, lastResponse.statusCode(), "El login falló");
        String token = lastResponse.getBody().asString();
        assertNotNull(token);
        assertFalse(token.isBlank());
    }
}