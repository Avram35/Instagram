package com.instagram.api;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthApiTest extends ApiTestConfig {

    @BeforeAll
    static void setup() { initUsersIfNeeded(); }

    @Test @Order(1)
    @DisplayName("Signup — uspesna registracija")
    void signup_success() {
        String u = unique("signup");
        Map<String, String> body = new HashMap<>();
        body.put("fname", "Novo");
        body.put("lname", "Ime");
        body.put("username", u);
        body.put("email", u + "@test.com");
        body.put("password", "StrongPass1!");

        given().baseUri(AUTH_URL).contentType(ContentType.JSON).body(body)
        .when().post("/api/v1/auth/signup")
        .then().statusCode(201).body("message", notNullValue());
    }

    @Test @Order(2)
    @DisplayName("Signup — duplikat username vraca 409")
    void signup_duplicateUsername() {
        Map<String, String> body = new HashMap<>();
        body.put("fname", "Dup");
        body.put("lname", "User");
        body.put("username", usernameA);
        body.put("email", unique("dup") + "@test.com");
        body.put("password", "StrongPass1!");

        given().baseUri(AUTH_URL).contentType(ContentType.JSON).body(body)
        .when().post("/api/v1/auth/signup")
        .then().statusCode(409).body("error", notNullValue());
    }

    @Test @Order(3)
    @DisplayName("Signup — duplikat email vraca 409")
    void signup_duplicateEmail() {
        Map<String, String> body = new HashMap<>();
        body.put("fname", "Dup");
        body.put("lname", "Email");
        body.put("username", unique("dupemail"));
        body.put("email", usernameA + "@test.com");
        body.put("password", "StrongPass1!");

        given().baseUri(AUTH_URL).contentType(ContentType.JSON).body(body)
        .when().post("/api/v1/auth/signup")
        .then().statusCode(409);
    }

    @Test @Order(4)
    @DisplayName("Signup — prazno telo vraca 400")
    void signup_emptyBody() {
        given().baseUri(AUTH_URL).contentType(ContentType.JSON).body("{}")
        .when().post("/api/v1/auth/signup")
        .then().statusCode(400);
    }

    @Test @Order(5)
    @DisplayName("Signin — uspesna prijava sa username")
    void signin_withUsername() {
        Map<String, String> body = new HashMap<>();
        body.put("usernameOrEmail", usernameA);
        body.put("password", PASSWORD);

        given().baseUri(AUTH_URL).contentType(ContentType.JSON).body(body)
        .when().post("/api/v1/auth/signin")
        .then().statusCode(200)
            .body("token", notNullValue())
            .body("username", equalTo(usernameA));
    }

    @Test @Order(6)
    @DisplayName("Signin — uspesna prijava sa email")
    void signin_withEmail() {
        Map<String, String> body = new HashMap<>();
        body.put("usernameOrEmail", usernameA + "@test.com");
        body.put("password", PASSWORD);

        given().baseUri(AUTH_URL).contentType(ContentType.JSON).body(body)
        .when().post("/api/v1/auth/signin")
        .then().statusCode(200).body("token", notNullValue());
    }

    @Test @Order(7)
    @DisplayName("Signin — pogresna lozinka vraca 401")
    void signin_wrongPassword() {
        Map<String, String> body = new HashMap<>();
        body.put("usernameOrEmail", usernameA);
        body.put("password", "WrongPass999!");

        given().baseUri(AUTH_URL).contentType(ContentType.JSON).body(body)
        .when().post("/api/v1/auth/signin")
        .then().statusCode(401);
    }

    @Test @Order(8)
    @DisplayName("Signin — nepostojeci korisnik vraca 401")
    void signin_userNotFound() {
        Map<String, String> body = new HashMap<>();
        body.put("usernameOrEmail", "nepostojeci_xyz_123");
        body.put("password", "anything");

        given().baseUri(AUTH_URL).contentType(ContentType.JSON).body(body)
        .when().post("/api/v1/auth/signin")
        .then().statusCode(401);
    }

    @Test @Order(9)
    @DisplayName("Delete — bez tokena vraca 401")
    void delete_noToken() {
        given().baseUri(AUTH_URL)
        .when().delete("/api/v1/auth/delete")
        .then().statusCode(401);
    }
}