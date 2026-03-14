package com.instagram.api;

import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BlockApiTest extends ApiTestConfig {

    @BeforeAll
    static void setup() { initUsersIfNeeded(); }

    @Test @Order(1)
    @DisplayName("Block — A blokira B (cleanup prvo)")
    void block() {
        tryUnblock(tokenA, userIdB);

        given().baseUri(BLOCK_URL).header("Authorization", "Bearer " + tokenA)
        .when().post("/api/v1/block/" + userIdB)
        .then().statusCode(200).body("message", notNullValue());
    }

    @Test @Order(2)
    @DisplayName("Block — check vraca true")
    void checkBlocked() {
        given().baseUri(BLOCK_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/block/check/" + userIdB)
        .then().statusCode(200).body("blocked", equalTo(true));
    }

    @Test @Order(3)
    @DisplayName("Block — check-by (B proverava A)")
    void checkBlockedBy() {
        given().baseUri(BLOCK_URL).header("Authorization", "Bearer " + tokenB)
        .when().get("/api/v1/block/check-by/" + userIdA)
        .then().statusCode(200).body("blocked", notNullValue());
    }

    @Test @Order(4)
    @DisplayName("Block — lista blokiranih")
    void blockedList() {
        given().baseUri(BLOCK_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/block/list")
        .then().statusCode(200)
            .body("$", instanceOf(java.util.List.class))
            .body("size()", greaterThanOrEqualTo(1));
    }

    @Test @Order(5)
    @DisplayName("Block — blokiranje sebe vraca gresku")
    void block_self() {
        given().baseUri(BLOCK_URL).header("Authorization", "Bearer " + tokenA)
        .when().post("/api/v1/block/" + userIdA)
        .then().statusCode(anyOf(is(400), is(409), is(500)));
    }

    @Test @Order(6)
    @DisplayName("Block internal — check-either")
    void checkEitherWay() {
        given().baseUri(BLOCK_URL).header("X-Internal-Api-Key", INTERNAL_API_KEY)
        .when().get("/api/v1/block/internal/check-either/" + userIdA + "/" + userIdB)
        .then().statusCode(200).body("blocked", notNullValue());
    }

    @Test @Order(7)
    @DisplayName("Unblock — A odblokira B")
    void unblock() {
        given().baseUri(BLOCK_URL).header("Authorization", "Bearer " + tokenA)
        .when().delete("/api/v1/block/" + userIdB)
        .then().statusCode(200).body("message", notNullValue());
    }

    @Test @Order(8)
    @DisplayName("Unblock — posle odblokiranja check vraca false")
    void checkAfterUnblock() {
        given().baseUri(BLOCK_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/block/check/" + userIdB)
        .then().statusCode(200).body("blocked", equalTo(false));
    }

    @Test @Order(9)
    @DisplayName("Block — bez tokena vraca 401")
    void block_noToken() {
        given().baseUri(BLOCK_URL)
        .when().post("/api/v1/block/" + userIdB)
        .then().statusCode(401);
    }

    @Test @Order(10)
    @DisplayName("Block list — bez tokena vraca 401")
    void list_noToken() {
        given().baseUri(BLOCK_URL)
        .when().get("/api/v1/block/list")
        .then().statusCode(401);
    }
}