package com.instagram.api;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InteractiveApiTest extends ApiTestConfig {

    private static Long testPostId;
    private static Long testCommentId;

    @BeforeAll
    static void setup() {
        initUsersIfNeeded();
        testPostId = createPostAndGetId(tokenA);
    }

    // ==================== LIKE ====================

    @Test @Order(1)
    @DisplayName("Like — lajkovanje objave (cleanup prvo)")
    void likePost() {
        Assumptions.assumeTrue(testPostId != null);
        // Cleanup: ako vec lajkovano, ukloni
        tryUnlike(tokenA, testPostId);

        given().baseUri(INTERACTIVE_URL).header("Authorization", "Bearer " + tokenA)
        .when().post("/api/v1/like/" + testPostId)
        .then().statusCode(200)
            .body("id", notNullValue())
            .body("postId", equalTo(testPostId.intValue()));
    }

    @Test @Order(2)
    @DisplayName("Like — dupli lajk vraca 409")
    void likePost_duplicate() {
        Assumptions.assumeTrue(testPostId != null);
        // A vec lajkovao iz Order(1)
        given().baseUri(INTERACTIVE_URL).header("Authorization", "Bearer " + tokenA)
        .when().post("/api/v1/like/" + testPostId)
        .then().statusCode(anyOf(is(400), is(409), is(500)));
    }

    @Test @Order(3)
    @DisplayName("Like — check vraca true")
    void checkLiked_true() {
        Assumptions.assumeTrue(testPostId != null);
        given().baseUri(INTERACTIVE_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/like/check/" + testPostId)
        .then().statusCode(200).body("liked", equalTo(true));
    }

    @Test @Order(4)
    @DisplayName("Like — broj lajkova >= 1")
    void likesCount() {
        Assumptions.assumeTrue(testPostId != null);
        given().baseUri(INTERACTIVE_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/like/count/" + testPostId)
        .then().statusCode(200).body("count", greaterThanOrEqualTo(1));
    }

    @Test @Order(5)
    @DisplayName("Like — lista lajkova")
    void likesList() {
        Assumptions.assumeTrue(testPostId != null);
        given().baseUri(INTERACTIVE_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/like/" + testPostId + "/list")
        .then().statusCode(200)
            .body("$", instanceOf(java.util.List.class))
            .body("size()", greaterThanOrEqualTo(1));
    }

    @Test @Order(6)
    @DisplayName("Unlike — uklanjanje lajka")
    void unlike() {
        Assumptions.assumeTrue(testPostId != null);
        given().baseUri(INTERACTIVE_URL).header("Authorization", "Bearer " + tokenA)
        .when().delete("/api/v1/like/" + testPostId)
        .then().statusCode(200);
    }

    @Test @Order(7)
    @DisplayName("Like — posle unlike check vraca false")
    void checkLiked_false() {
        Assumptions.assumeTrue(testPostId != null);
        given().baseUri(INTERACTIVE_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/like/check/" + testPostId)
        .then().statusCode(200).body("liked", equalTo(false));
    }

    @Test @Order(8)
    @DisplayName("Like — bez tokena vraca 401")
    void like_noToken() {
        given().baseUri(INTERACTIVE_URL)
        .when().post("/api/v1/like/1")
        .then().statusCode(401);
    }

    // ==================== COMMENT ====================

    @Test @Order(10)
    @DisplayName("Comment — dodavanje komentara")
    void addComment() {
        Assumptions.assumeTrue(testPostId != null);
        Map<String, String> body = new HashMap<>();
        body.put("content", "Test komentar iz Jave!");

        Response r = given().baseUri(INTERACTIVE_URL).header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON).body(body)
        .when().post("/api/v1/comment/" + testPostId)
        .then().statusCode(200)
            .body("content", equalTo("Test komentar iz Jave!"))
            .body("postId", equalTo(testPostId.intValue()))
            .extract().response();

        testCommentId = r.jsonPath().getLong("id");
    }

    @Test @Order(11)
    @DisplayName("Comment — prazan komentar vraca gresku")
    void addComment_empty() {
        Assumptions.assumeTrue(testPostId != null);
        Map<String, String> body = new HashMap<>();
        body.put("content", "");

        given().baseUri(INTERACTIVE_URL).header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON).body(body)
        .when().post("/api/v1/comment/" + testPostId)
        .then().statusCode(anyOf(is(400), is(500)));
    }

    @Test @Order(12)
    @DisplayName("Comment — lista komentara")
    void commentsList() {
        Assumptions.assumeTrue(testPostId != null);
        given().baseUri(INTERACTIVE_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/comment/" + testPostId + "/list")
        .then().statusCode(200)
            .body("$", instanceOf(java.util.List.class))
            .body("size()", greaterThanOrEqualTo(1));
    }

    @Test @Order(13)
    @DisplayName("Comment — broj komentara >= 1")
    void commentsCount() {
        Assumptions.assumeTrue(testPostId != null);
        given().baseUri(INTERACTIVE_URL).header("Authorization", "Bearer " + tokenA)
        .when().get("/api/v1/comment/count/" + testPostId)
        .then().statusCode(200).body("count", greaterThanOrEqualTo(1));
    }

    @Test @Order(14)
    @DisplayName("Comment — izmena sopstvenog komentara")
    void updateComment() {
        Assumptions.assumeTrue(testCommentId != null);
        Map<String, String> body = new HashMap<>();
        body.put("content", "Izmenjen komentar");

        given().baseUri(INTERACTIVE_URL).header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON).body(body)
        .when().put("/api/v1/comment/" + testCommentId)
        .then().statusCode(200).body("content", equalTo("Izmenjen komentar"));
    }

    @Test @Order(15)
    @DisplayName("Comment — B ne moze da izmeni komentar A")
    void updateComment_forbidden() {
        Assumptions.assumeTrue(testCommentId != null);
        Map<String, String> body = new HashMap<>();
        body.put("content", "Hak");

        given().baseUri(INTERACTIVE_URL).header("Authorization", "Bearer " + tokenB)
            .contentType(ContentType.JSON).body(body)
        .when().put("/api/v1/comment/" + testCommentId)
        .then().statusCode(anyOf(is(400), is(403), is(500)));
    }

    @Test @Order(16)
    @DisplayName("Comment — brisanje sopstvenog komentara")
    void deleteComment() {
        Assumptions.assumeTrue(testPostId != null);
        // Kreiraj novi komentar za brisanje
        Map<String, String> body = new HashMap<>();
        body.put("content", "Za brisanje");

        Response r = given().baseUri(INTERACTIVE_URL).header("Authorization", "Bearer " + tokenA)
            .contentType(ContentType.JSON).body(body)
        .when().post("/api/v1/comment/" + testPostId)
        .then().statusCode(200).extract().response();

        Long id = r.jsonPath().getLong("id");

        given().baseUri(INTERACTIVE_URL).header("Authorization", "Bearer " + tokenA)
        .when().delete("/api/v1/comment/" + id)
        .then().statusCode(200);
    }

    @Test @Order(17)
    @DisplayName("Comment — bez tokena vraca 401")
    void comment_noToken() {
        given().baseUri(INTERACTIVE_URL)
            .contentType(ContentType.JSON).body("{\"content\":\"test\"}")
        .when().post("/api/v1/comment/1")
        .then().statusCode(401);
    }
}