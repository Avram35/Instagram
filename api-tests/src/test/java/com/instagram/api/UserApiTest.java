package com.instagram.api;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserApiTest extends ApiTestConfig {

    @BeforeAll
    static void setup() { initUsersIfNeeded(); }

    @Test @Order(1)
    @DisplayName("Profil — po username-u")
    void getByUsername() {
        given().baseUri(USER_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/user/" + usernameA)
        .then().statusCode(200)
            .body("username", equalTo(usernameA))
            .body("fname", notNullValue());
    }

    @Test @Order(2)
    @DisplayName("Profil — po ID-u")
    void getById() {
        given().baseUri(USER_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/user/id/" + userIdA)
        .then().statusCode(200).body("id", equalTo(userIdA.intValue()));
    }

    @Test @Order(3)
    @DisplayName("Profil — nepostojeci username vraca 404")
    void getByUsername_notFound() {
        given().baseUri(USER_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/user/nepostojeci_xyz_999")
        .then().statusCode(404);
    }

    @Test @Order(4)
    @DisplayName("Profil — nepostojeci ID vraca 404")
    void getById_notFound() {
        given().baseUri(USER_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/user/id/999999")
        .then().statusCode(404);
    }

    @Test @Order(5)
    @DisplayName("Pretraga — vraca listu")
    void search_returnsList() {
        given().baseUri(USER_URL).header("Authorization", "Bearer " + tokenA)
            .queryParam("query", "javatest")
        .when().get("/api/v1/user/search")
        .then().statusCode(200).body("$", instanceOf(java.util.List.class));
    }

    @Test @Order(6)
    @DisplayName("Pretraga — prazan query vraca 400")
    void search_emptyQuery() {
        given().baseUri(USER_URL).header("Authorization", "Bearer " + tokenA)
            .queryParam("query", "")
        .when().get("/api/v1/user/search")
        .then().statusCode(400);
    }

    @Test @Order(7)
    @DisplayName("Pretraga — ne vraca sebe")
    void search_excludesSelf() {
        var ids = given().baseUri(USER_URL).header("Authorization", "Bearer " + tokenA)
            .queryParam("query", usernameA)
        .when().get("/api/v1/user/search")
        .then().statusCode(200)
            .extract().jsonPath().getList("id", Long.class);

        Assertions.assertFalse(ids.contains(userIdA));
    }

    @Test @Order(8)
    @DisplayName("Update — promena bio")
    void update_bio() {
        Map<String, Object> body = new HashMap<>();
        body.put("bio", "Java test bio");

        given().baseUri(USER_URL).header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON).body(body)
        .when().put("/api/v1/user/" + userIdA)
        .then().statusCode(200);

        // Verifikacija
        given().baseUri(USER_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/user/" + usernameA)
        .then().body("bio", equalTo("Java test bio"));
    }

    @Test @Order(9)
    @DisplayName("Update — ime i prezime")
    void update_fnameAndLname() {
        Map<String, Object> body = new HashMap<>();
        body.put("fname", "NovoIme");
        body.put("lname", "NovoPrezime");

        given().baseUri(USER_URL).header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON).body(body)
        .when().put("/api/v1/user/" + userIdA)
        .then().statusCode(200);
    }

    @Test @Order(10)
    @DisplayName("Update — bio >150 vraca 400")
    void update_bioTooLong() {
        Map<String, Object> body = new HashMap<>();
        body.put("bio", "x".repeat(200));

        given().baseUri(USER_URL).header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON).body(body)
        .when().put("/api/v1/user/" + userIdA)
        .then().statusCode(400);
    }

    @Test @Order(11)
    @DisplayName("Update — tudji profil vraca 403")
    void update_otherUserForbidden() {
        Map<String, Object> body = new HashMap<>();
        body.put("bio", "Hacked");

        given().baseUri(USER_URL).header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON).body(body)
        .when().put("/api/v1/user/" + userIdB)
        .then().statusCode(403);
    }

    @Test @Order(12)
    @DisplayName("Update — toggle privatni profil")
    void update_togglePrivate() {
        Map<String, Object> on = new HashMap<>();
        on.put("privateProfile", true);
        given().baseUri(USER_URL).header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON).body(on)
        .when().put("/api/v1/user/" + userIdA)
        .then().statusCode(200);

        given().baseUri(USER_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/user/" + usernameA)
        .then().body("privateProfile", equalTo(true));

        // Vrati na javni
        Map<String, Object> off = new HashMap<>();
        off.put("privateProfile", false);
        given().baseUri(USER_URL).header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON).body(off)
        .when().put("/api/v1/user/" + userIdA)
        .then().statusCode(200);
    }
}