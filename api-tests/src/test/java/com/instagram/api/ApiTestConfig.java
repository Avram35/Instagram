package com.instagram.api;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

/**
 * Bazna konfiguracija za sve API testove.
 * Sadrzi URL-ove, helper metode i setup test korisnika.
 */
public class ApiTestConfig {

    // ==================== URL-ovi servisa ====================

    protected static final String AUTH_URL        = env("AUTH_URL",        "http://localhost:8081");
    protected static final String USER_URL        = env("USER_URL",        "http://localhost:8082");
    protected static final String FOLLOW_URL      = env("FOLLOW_URL",      "http://localhost:8083");
    protected static final String BLOCK_URL       = env("BLOCK_URL",       "http://localhost:8084");
    protected static final String FEED_URL        = env("FEED_URL",        "http://localhost:8085");
    protected static final String POST_URL        = env("POST_URL",        "http://localhost:8086");
    protected static final String INTERACTIVE_URL = env("INTERACTIVE_URL",  "http://localhost:8087");
    protected static final String INTERNAL_API_KEY = env("INTERNAL_API_KEY", "test-internal-key-12345");

    // ==================== Test korisnici ====================

    protected static String tokenA;
    protected static String tokenB;
    protected static String tokenC;
    protected static Long userIdA;
    protected static Long userIdB;
    protected static Long userIdC;
    protected static String usernameA;
    protected static String usernameB;
    protected static String usernameC;

    protected static final String PASSWORD = "TestPass123!";
    protected static boolean initialized = false;

    /**
     * Inicijalizuje 3 test korisnika (A javni, B javni, C privatni).
     * Idempotentno — poziva se iz @BeforeAll svake klase.
     */
    protected static void initUsersIfNeeded() {
        if (initialized) return;

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        usernameA = unique("javatest_a");
        usernameB = unique("javatest_b");
        usernameC = unique("javatest_c");

        register(usernameA, usernameA + "@test.com", "Test", "UserA");
        register(usernameB, usernameB + "@test.com", "Test", "UserB");
        register(usernameC, usernameC + "@test.com", "Test", "UserC");

        tokenA = login(usernameA);
        tokenB = login(usernameB);
        tokenC = login(usernameC);

        userIdA = getUserId(usernameA, tokenA);
        userIdB = getUserId(usernameB, tokenB);
        userIdC = getUserId(usernameC, tokenC);

        // C = privatni profil
        setPrivate(userIdC, tokenC, true);

        initialized = true;
    }

    // ==================== Helper metode ====================

    protected static String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().substring(0, 6);
    }

    protected static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    protected static void register(String username, String email, String fname, String lname) {
        Map<String, String> body = new HashMap<>();
        body.put("fname", fname);
        body.put("lname", lname);
        body.put("username", username);
        body.put("email", email);
        body.put("password", PASSWORD);

        given()
            .baseUri(AUTH_URL)
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/v1/auth/signup");
        // Ignorisemo odgovor — moze biti 201 ili 409
    }

    protected static String login(String username) {
        Map<String, String> body = new HashMap<>();
        body.put("usernameOrEmail", username);
        body.put("password", PASSWORD);

        return given()
            .baseUri(AUTH_URL)
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .post("/api/v1/auth/signin")
        .then()
            .statusCode(200)
            .extract().jsonPath().getString("token");
    }

    protected static Long getUserId(String username, String token) {
        return given()
            .baseUri(USER_URL)
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/user/" + username)
        .then()
            .statusCode(200)
            .extract().jsonPath().getLong("id");
    }

    protected static void setPrivate(Long userId, String token, boolean isPrivate) {
        Map<String, Object> body = new HashMap<>();
        body.put("privateProfile", isPrivate);

        given()
            .baseUri(USER_URL)
            .header("Authorization", "Bearer " + token)
            .contentType(ContentType.JSON)
            .body(body)
        .when()
            .put("/api/v1/user/" + userId)
        .then()
            .statusCode(200);
    }

    /**
     * Kreira minimalnu test JPEG sliku (100x100 crvena).
     */
    protected static byte[] createMinimalJpeg() {
        try {
            BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 100; x++)
                for (int y = 0; y < 100; y++)
                    img.setRGB(x, y, 0xFF0000);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "jpg", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Kreira objavu i vraca njen ID. Vraca null ako ne uspe.
     */
    protected static Long createPostAndGetId(String token) {
        byte[] img = createMinimalJpeg();
        Response r = given()
            .baseUri(POST_URL)
            .header("Authorization", "Bearer " + token)
            .multiPart("files", "test.jpg", img, "image/jpeg")
            .multiPart("description", "Test post")
        .when()
            .post("/api/v1/post");

        if (r.statusCode() == 200) {
            return r.jsonPath().getLong("id");
        }
        return null;
    }

    /**
     * Pokusava unfollow — ignorise gresku ako ne prati.
     */
    protected static void tryUnfollow(String token, Long targetId) {
        given()
            .baseUri(FOLLOW_URL)
            .header("Authorization", "Bearer " + token)
        .when()
            .delete("/api/v1/follow/" + targetId);
    }

    /**
     * Pokusava unblock — ignorise gresku.
     */
    protected static void tryUnblock(String token, Long targetId) {
        given()
            .baseUri(BLOCK_URL)
            .header("Authorization", "Bearer " + token)
        .when()
            .delete("/api/v1/block/" + targetId);
    }

    /**
     * Pokusava unlike — ignorise gresku.
     */
    protected static void tryUnlike(String token, Long postId) {
        given()
            .baseUri(INTERACTIVE_URL)
            .header("Authorization", "Bearer " + token)
        .when()
            .delete("/api/v1/like/" + postId);
    }

    private static String env(String key, String def) {
        String val = System.getenv(key);
        return (val != null && !val.isEmpty()) ? val : def;
    }
}