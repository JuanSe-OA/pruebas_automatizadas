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

import com.github.javafaker.Faker;
import java.util.Locale;


public class AuthSteps {

    private Response response;
    private static final Map<String, Object> ctx = new HashMap<>();
    private static final Faker faker = new Faker(new Locale("es","CO"));


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
            String user = propOr("LOGIN_USERNAME", "admin");
            String pass = propOr("LOGIN_PASSWORD", "admin123");

            Response r = given().header("Content-Type", "application/json")
                    .body("{\"username\":\"" + user + "\",\"password\":\"" + pass + "\"}")
                    .post("/api/auth/login");

            if (r.statusCode() != 200) {
                // Auto-registro de un usuario si el login del admin falla
                String newUser = faker.name().username().replaceAll("[^a-zA-Z0-9]", "") + "_" +
                        UUID.randomUUID().toString().substring(0,5);
                String newEmail = faker.internet().emailAddress();
                String newPass  = "Secr3t$123";

                given().header("Content-Type","application/json")
                        .body("{\"username\":\""+newUser+"\",\"email\":\""+newEmail+"\",\"password\":\""+newPass+"\",\"phoneNumber\":\"3"+faker.number().digits(9)+"\"}")
                        .post("/api/users")
                        .then().statusCode(200);

                // Login con el usuario recién creado
                r = given().header("Content-Type","application/json")
                        .body("{\"username\":\""+newUser+"\",\"password\":\""+newPass+"\"}")
                        .post("/api/auth/login")
                        .then().statusCode(200).extract().response();

                // Guarda esos valores por si quieres usarlos después
                ctx.put("LOGIN_USERNAME", newUser);
                ctx.put("LOGIN_PASSWORD", newPass);
            }

            String token = r.asString().trim();
            Assertions.assertTrue(token.length() > 10, "Token inválido: " + token);
            ctx.put("AUTH_TOKEN", token);
        }
    }
    private String propOr(String key, String fallback) {
        // 1) System property
        String v = System.getProperty(key);
        if (v != null && !v.trim().isEmpty()) return v.trim();
        // 2) Env var
        v = System.getenv(key);
        if (v != null && !v.trim().isEmpty()) return v.trim();
        // 3) Fallback
        return fallback;
    }

    private String interpolate(String text) {
        String out = text;

        // Reemplazos desde el contexto (tokens guardados, etc.)
        for (Map.Entry<String, Object> e : ctx.entrySet()) {
            out = out.replace("${" + e.getKey() + "}", String.valueOf(e.getValue()));
        }

        // Faker para defaults aleatorios
        String randomUser  = faker.name().username().replaceAll("[^a-zA-Z0-9]", "");
        String randomMail  = faker.internet().emailAddress();
        String randomPhone = "3" + faker.number().digits(9);

        // Login (usa defaults si están vacíos)
        out = out.replace("${LOGIN_USERNAME}", propOr("LOGIN_USERNAME", "admin"));
        out = out.replace("${LOGIN_PASSWORD}", propOr("LOGIN_PASSWORD", "admin123"));

        // Registro (si están vacíos, usa Faker)
        out = out.replace("${REGISTER_USERNAME}", propOr("REGISTER_USERNAME", randomUser));
        out = out.replace("${REGISTER_EMAIL}",   propOr("REGISTER_EMAIL",   randomMail));
        out = out.replace("${REGISTER_PASSWORD}",propOr("REGISTER_PASSWORD","Secr3t$123"));
        out = out.replace("${REGISTER_PHONE}",   propOr("REGISTER_PHONE",   randomPhone));

        out = out.replace("${RAND}", UUID.randomUUID().toString().substring(0, 6));
        return out;
    }


}
