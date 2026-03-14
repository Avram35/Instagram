package com.instagram.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostApiTest extends ApiTestConfig {

    private static Long postId;

    @BeforeAll
    static void setup() { initUsersIfNeeded(); }

    @Test @Order(1)
    @DisplayName("Post — kreiranje sa jednom slikom")
    void createPost_single() {
        byte[] img = createMinimalJpeg();

        Response r = given().baseUri(POST_URL).header("Authorization", "Bearer " + tokenA)
            .multiPart("files", "photo.jpg", img, "image/jpeg")
            .multiPart("description", "Test objava")
        .when().post("/api/v1/post")
        .then().statusCode(200)
            .body("id", notNullValue())
            .body("description", equalTo("Test objava"))
            .body("media.size()", greaterThanOrEqualTo(1))
            .extract().response();

        postId = r.jsonPath().getLong("id");
    }

    @Test @Order(2)
    @DisplayName("Post — kreiranje sa vise slika")
    void createPost_multiple() {
        byte[] img1 = createMinimalJpeg();
        byte[] img2 = createMinimalJpeg();

        given().baseUri(POST_URL).header("Authorization", "Bearer " + tokenA)
            .multiPart("files", "p1.jpg", img1, "image/jpeg")
            .multiPart("files", "p2.jpg", img2, "image/jpeg")
        .when().post("/api/v1/post")
        .then().statusCode(200).body("media.size()", equalTo(2));
    }

    @Test @Order(3)
    @DisplayName("Post — bez fajlova vraca gresku")
    void createPost_noFiles() {
        given().baseUri(POST_URL).header("Authorization", "Bearer " + tokenA)
            .multiPart("description", "Bez slike")
        .when().post("/api/v1/post")
        .then().statusCode(anyOf(is(400), is(500)));
    }

    @Test @Order(4)
    @DisplayName("Post — bez tokena vraca 401")
    void createPost_noToken() {
        byte[] img = createMinimalJpeg();
        given().baseUri(POST_URL)
            .multiPart("files", "p.jpg", img, "image/jpeg")
        .when().post("/api/v1/post")
        .then().statusCode(401);
    }

    @Test @Order(5)
    @DisplayName("Post — preuzimanje po ID-u")
    void getById() {
        Assumptions.assumeTrue(postId != null);
        given().baseUri(POST_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/post/" + postId)
        .then().statusCode(200).body("id", equalTo(postId.intValue()));
    }

    @Test @Order(6)
    @DisplayName("Post — objave korisnika")
    void getByUser() {
        given().baseUri(POST_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/post/user/" + userIdA)
        .then().statusCode(200)
            .body("$", instanceOf(java.util.List.class))
            .body("size()", greaterThanOrEqualTo(1));
    }

    @Test @Order(7)
    @DisplayName("Post — broj objava")
    void getCount() {
        given().baseUri(POST_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/post/count/" + userIdA)
        .then().statusCode(200).body("count", greaterThanOrEqualTo(1));
    }

    @Test @Order(8)
    @DisplayName("Post — nepostojeca objava")
    void getById_notFound() {
        given().baseUri(POST_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/post/999999")
        .then().statusCode(anyOf(is(404), is(500)));
    }

    @Test @Order(9)
    @DisplayName("Post — azuriranje opisa")
    void updateDescription() {
        Assumptions.assumeTrue(postId != null);
        given().baseUri(POST_URL).header("Authorization", "Bearer " + tokenA)
            .contentType("application/json")
            .body("{\"description\": \"Azuriran opis\"}")
        .when().put("/api/v1/post/" + postId)
        .then().statusCode(200).body("description", equalTo("Azuriran opis"));
    }

    @Test @Order(10)
    @DisplayName("Post — B ne moze da izmeni post A")
    void updateDescription_forbidden() {
        Assumptions.assumeTrue(postId != null);
        given().baseUri(POST_URL).header("Authorization", "Bearer " + tokenB)
            .contentType("application/json")
            .body("{\"description\": \"Hak\"}")
        .when().put("/api/v1/post/" + postId)
        .then().statusCode(anyOf(is(400), is(403), is(500)));
    }

    @Test @Order(11)
    @DisplayName("Post — privatni profil C — B ne moze da vidi objave")
    void privateProfile_forbidden() {
        given().baseUri(POST_URL).header("Authorization", "Bearer " + tokenB)
        .when().get("/api/v1/post/user/" + userIdC)
        .then().statusCode(403);
    }
}