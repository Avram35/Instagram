package com.instagram.api;

import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FollowApiTest extends ApiTestConfig {

    @BeforeAll
    static void setup() { initUsersIfNeeded(); }

    // ==================== PRACENJE JAVNOG PROFILA ====================

    @Test @Order(1)
    @DisplayName("Follow — A zaprati javnog B (cleanup pa follow)")
    void follow_shouldReturn200_forPublicProfile() {
        // Cleanup: ako A vec prati B, prvo otprati
        tryUnfollow(tokenA, userIdB);

        given().baseUri(FOLLOW_URL).header("Authorization", "Bearer " + tokenA)
        .when().post("/api/v1/follow/" + userIdB)
        .then().statusCode(200).body("message", notNullValue());
    }

    @Test @Order(2)
    @DisplayName("Follow — duplikat vraca 409")
    void follow_duplicate() {
        // A vec prati B iz Order(1)
        given().baseUri(FOLLOW_URL).header("Authorization", "Bearer " + tokenA)
        .when().post("/api/v1/follow/" + userIdB)
        .then().statusCode(anyOf(is(400), is(409), is(500)));
    }

    @Test @Order(3)
    @DisplayName("Follow — pracenje samog sebe vraca gresku")
    void follow_self() {
        given().baseUri(FOLLOW_URL).header("Authorization", "Bearer " + tokenA)
        .when().post("/api/v1/follow/" + userIdA)
        .then().statusCode(anyOf(is(400), is(409), is(500)));
    }

    // ==================== PRIVATNI PROFIL ====================

    @Test @Order(4)
    @DisplayName("Follow — zahtev za pracenje privatnog C")
    void follow_privateProfile() {
        // Cleanup
        tryUnfollow(tokenB, userIdC);

        given().baseUri(FOLLOW_URL).header("Authorization", "Bearer " + tokenB)
        .when().post("/api/v1/follow/" + userIdC)
        .then().statusCode(200).body("message", notNullValue());
    }

    @Test @Order(5)
    @DisplayName("Follow — pending zahtev check")
    void hasPendingRequest() {
        given().baseUri(FOLLOW_URL).header("Authorization", "Bearer " + tokenB)
        .when().get("/api/v1/follow/requests/check/" + userIdC)
        .then().statusCode(200).body("pending", notNullValue());
    }

    @Test @Order(6)
    @DisplayName("Follow — C vidi pending zahteve")
    void getPendingRequests() {
        given().baseUri(FOLLOW_URL).header("Authorization", "Bearer " + tokenC)
        .when().get("/api/v1/follow/requests/pending")
        .then().statusCode(200).body("$", instanceOf(java.util.List.class));
    }

    // ==================== LISTE I STATUS ====================

    @Test @Order(7)
    @DisplayName("Follow — lista pratilaca B")
    void getFollowers() {
        given().baseUri(FOLLOW_URL).header("Authorization", "Bearer " + tokenB)
        .when().get("/api/v1/follow/" + userIdB + "/followers")
        .then().statusCode(200).body("$", instanceOf(java.util.List.class));
    }

    @Test @Order(8)
    @DisplayName("Follow — lista pracenja A")
    void getFollowing() {
        given().baseUri(FOLLOW_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/follow/" + userIdA + "/following")
        .then().statusCode(200).body("$", instanceOf(java.util.List.class));
    }

    @Test @Order(9)
    @DisplayName("Follow — broj pratilaca/pracenja")
    void getFollowCount() {
        given().baseUri(FOLLOW_URL).header("Authorization", "Bearer " + tokenB)
        .when().get("/api/v1/follow/" + userIdB + "/count")
        .then().statusCode(200)
            .body("followersCount", notNullValue())
            .body("followingCount", notNullValue());
    }

    @Test @Order(10)
    @DisplayName("Follow — status (following + pending)")
    void getFollowStatus() {
        given().baseUri(FOLLOW_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/follow/status/" + userIdB)
        .then().statusCode(200)
            .body("following", notNullValue())
            .body("pending", notNullValue());
    }

    @Test @Order(11)
    @DisplayName("Follow — check da li A prati B")
    void isFollowing() {
        given().baseUri(FOLLOW_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/follow/check/" + userIdB)
        .then().statusCode(200).body("following", notNullValue());
    }

    // ==================== NOTIFIKACIJE ====================

    @Test @Order(12)
    @DisplayName("Notifikacije — lista")
    void getNotifications() {
        given().baseUri(FOLLOW_URL).header("Authorization", "Bearer " + tokenB)
        .when().get("/api/v1/follow/notifications")
        .then().statusCode(200).body("$", instanceOf(java.util.List.class));
    }

    @Test @Order(13)
    @DisplayName("Notifikacije — oznaci sve kao procitane")
    void markAllAsRead() {
        given().baseUri(FOLLOW_URL).header("Authorization", "Bearer " + tokenB)
        .when().put("/api/v1/follow/notifications/read-all")
        .then().statusCode(200);
    }

    // ==================== UNFOLLOW ====================

    @Test @Order(14)
    @DisplayName("Unfollow — A otprati B")
    void unfollow() {
        given().baseUri(FOLLOW_URL).header("Authorization", "Bearer " + tokenA)
        .when().delete("/api/v1/follow/" + userIdB)
        .then().statusCode(200);
    }

    // ==================== BEZ AUTH ====================

    @Test @Order(15)
    @DisplayName("Follow — bez tokena vraca 401")
    void follow_noToken() {
        given().baseUri(FOLLOW_URL)
        .when().post("/api/v1/follow/" + userIdB)
        .then().statusCode(401);
    }

    @Test @Order(16)
    @DisplayName("Notifikacije — bez tokena vraca 401")
    void notifications_noToken() {
        given().baseUri(FOLLOW_URL)
        .when().get("/api/v1/follow/notifications")
        .then().statusCode(401);
    }
}