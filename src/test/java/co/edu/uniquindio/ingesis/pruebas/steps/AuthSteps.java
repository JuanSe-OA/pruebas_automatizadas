package co.edu.uniquindio.ingesis.pruebas.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.Assertions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

public class AuthSteps {

    private Response response;
    private static final Map<String, Object> ctx = new HashMap<>();

    @Before
    public void setup() {
        String base = System.getProperty("BASE_URL",
                System.getenv().getOrDefault("BASE_URL", "http://localhost:8081"));
        RestAssured.baseURI = base;
    }

    @Given("la base URL del Auth API")
    public void baseUrl() { }

    @When("hago POST a {string} con JSON:")
    public void postConJson(String path, String body) {
        String resolved = interpolate(body);
        response = given()
                .header("Content-Type", "application/json")
                .body(resolved)
                .when()
                .post(path);
    }

    @When("hago GET a {string} con bearer {string}")
    public void getConBearer(String path, String tokenVar) {
        String token = interpolate(tokenVar);
        response = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(path);
    }

    @When("hago GET a {string} sin autorización")
    public void getSinAuth(String path) {
        response = given().when().get(path);
    }

    @Then("la respuesta debe tener status {int}")
    public void assertStatus(int status) {
        Assertions.assertEquals(status, response.statusCode(),
                "Status esperado " + status + " pero fue " + response.statusCode()
                        + " body: " + response.asString());
    }

    @Then("el header {string} contiene {string}")
    public void headerContiene(String name, String fragment) {
        String hv = response.getHeader(name);
        Assertions.assertNotNull(hv, "Header " + name + " inexistente");
        Assertions.assertTrue(hv.contains(fragment),
                "Header " + name + " no contiene '" + fragment + "': " + hv);
    }

    @Then("el JSON cumple el schema {string}")
    public void validaSchema(String schemaPath) {
        response.then().assertThat().body(matchesJsonSchemaInClasspath(schemaPath));
    }

    @Then("guardo el token JWT retornado como {string}")
    public void guardoToken(String key) {
        String token = response.asString().trim(); // login devuelve texto plano
        Assertions.assertTrue(token.length() > 10, "Token inválido: " + token);
        ctx.put(key, token);
    }

    @Given("tengo un token válido \\(login previo si es necesario)")
    public void obtenerTokenPrevio() {
        if (!ctx.containsKey("AUTH_TOKEN")) {
            String user = System.getProperty("LOGIN_USERNAME",
                    System.getenv().getOrDefault("LOGIN_USERNAME", "admin"));
            String pass = System.getProperty("LOGIN_PASSWORD",
                    System.getenv().getOrDefault("LOGIN_PASSWORD", "admin123"));

            Response r = given()
                    .header("Content-Type", "application/json")
                    .body("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}")
                    .post("/api/auth/login");

            Assertions.assertEquals(200, r.statusCode(), "Login previo falló: " + r.asString());
            String token = r.asString().trim();
            ctx.put("AUTH_TOKEN", token);
        }
    }

    private String interpolate(String text) {
        String out = text;
        for (Map.Entry<String, Object> e : ctx.entrySet()) {
            out = out.replace("${" + e.getKey() + "}", String.valueOf(e.getValue()));
        }
        out = out.replace("${LOGIN_USERNAME}", System.getProperty("LOGIN_USERNAME",
                System.getenv().getOrDefault("LOGIN_USERNAME", "admin")));
        out = out.replace("${LOGIN_PASSWORD}", System.getProperty("LOGIN_PASSWORD",
                System.getenv().getOrDefault("LOGIN_PASSWORD", "admin123")));
        out = out.replace("${REGISTER_USERNAME}", System.getProperty("REGISTER_USERNAME",
                System.getenv().getOrDefault("REGISTER_USERNAME", "testuser")));
        out = out.replace("${REGISTER_EMAIL}", System.getProperty("REGISTER_EMAIL",
                System.getenv().getOrDefault("REGISTER_EMAIL", "test@example.com")));
        out = out.replace("${REGISTER_PASSWORD}", System.getProperty("REGISTER_PASSWORD",
                System.getenv().getOrDefault("REGISTER_PASSWORD", "Secr3t$123")));
        out = out.replace("${REGISTER_PHONE}", System.getProperty("REGISTER_PHONE",
                System.getenv().getOrDefault("REGISTER_PHONE", "3001234567")));
        out = out.replace("${RAND}", UUID.randomUUID().toString().substring(0, 6));
        return out;
    }
}
