package com.instagram.api;

import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FeedApiTest extends ApiTestConfig {

    @BeforeAll
    static void setup() { initUsersIfNeeded(); }

    @Test @Order(1)
    @DisplayName("Feed — autentifikovan korisnik")
    void getFeed_authenticated() {
        given().baseUri(FEED_URL).header("Authorization", "Bearer " + tokenA)
            .queryParam("page", 0).queryParam("size", 20)
        .when().get("/api/v1/feed")
        .then().statusCode(200).body("$", instanceOf(java.util.List.class));
    }

    @Test @Order(2)
    @DisplayName("Feed — paginacija (size=3)")
    void getFeed_pagination() {
        given().baseUri(FEED_URL).header("Authorization", "Bearer " + tokenA)
            .queryParam("page", 0).queryParam("size", 3)
        .when().get("/api/v1/feed")
        .then().statusCode(200).body("size()", lessThanOrEqualTo(3));
    }

    @Test @Order(3)
    @DisplayName("Feed — velika stranica vraca prazan niz")
    void getFeed_emptyPage() {
        given().baseUri(FEED_URL).header("Authorization", "Bearer " + tokenA)
            .queryParam("page", 999).queryParam("size", 20)
        .when().get("/api/v1/feed")
        .then().statusCode(200).body("$", hasSize(0));
    }

    @Test @Order(4)
    @DisplayName("Feed — bez tokena vraca 401")
    void getFeed_noToken() {
        given().baseUri(FEED_URL)
            .queryParam("page", 0).queryParam("size", 20)
        .when().get("/api/v1/feed")
        .then().statusCode(401);
    }

    @Test @Order(5)
    @DisplayName("Feed — hronoloski sort (opadajuce)")
    void getFeed_chronological() {
        // Zaprati B
        given().baseUri(FOLLOW_URL).header("Authorization", "Bearer " + tokenA)
        .when().post("/api/v1/follow/" + userIdB);

        Response r = given().baseUri(FEED_URL).header("Authorization", "Bearer " + tokenA)
            .queryParam("page", 0).queryParam("size", 50)
        .when().get("/api/v1/feed")
        .then().statusCode(200).extract().response();

        List<String> dates = r.jsonPath().getList("createdAt", String.class);
        if (dates != null && dates.size() >= 2) {
            for (int i = 0; i < dates.size() - 1; i++) {
                Assertions.assertTrue(
                    dates.get(i).compareTo(dates.get(i + 1)) >= 0,
                    "Feed mora biti sortiran hronoloski opadajuce"
                );
            }
        }
    }

    @Test @Order(6)
    @DisplayName("Feed — default paginacija")
    void getFeed_defaultPagination() {
        given().baseUri(FEED_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/feed")
        .then().statusCode(200).body("$", instanceOf(java.util.List.class));
    }
}