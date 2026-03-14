package com.instagram.ui;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static com.instagram.ui.UITestConfig.FOLLOW_URL;
import static com.instagram.ui.UITestConfig.FRONTEND_URL;
import static com.instagram.ui.UITestConfig.POST_URL;
import static com.instagram.ui.UITestConfig.apiRegisterAndLogin;
import static com.instagram.ui.UITestConfig.clearSession;
import static com.instagram.ui.UITestConfig.click;
import static com.instagram.ui.UITestConfig.createDriver;
import static com.instagram.ui.UITestConfig.createTestImage;
import static com.instagram.ui.UITestConfig.fetchUserId;
import static com.instagram.ui.UITestConfig.loginViaUI;
import static com.instagram.ui.UITestConfig.quit;
import static com.instagram.ui.UITestConfig.sleep;
import static com.instagram.ui.UITestConfig.unique;
import static com.instagram.ui.UITestConfig.waitFor;
import static com.instagram.ui.UITestConfig.waitForPath;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;

/**
 * UI integracionih testovi -- Feed (Vremenska linija)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UI -- Feed")
public class FeedUITest {

    private static WebDriver driver;

    private static String followerUsername;
    private static String followedUsername;
    private static String followerToken;
    private static String followedToken;
    private static Long   followedId;

    private static final String PASSWORD = "Test1234!";

    @BeforeAll
    static void setup() throws Exception {
        followerUsername = unique("ffol");
        followedUsername = unique("ffwd");

        followerToken = apiRegisterAndLogin(followerUsername,
            unique("ffl") + "@test.com", PASSWORD, "Feed", "Follower");
        sleep(2000);

        followedToken = apiRegisterAndLogin(followedUsername,
            unique("ffd") + "@test.com", PASSWORD, "Feed", "Followed");
        sleep(2000);

        followedId = fetchUserId(followedUsername, followedToken);

        sleep(1000);
        given()
            .baseUri(FOLLOW_URL).contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + followerToken)
            .post("/api/v1/follow/" + followedId)
            .then().statusCode(200);

        sleep(1000);
        File img = createTestImage();
        given()
            .baseUri(POST_URL)
            .header("Authorization", "Bearer " + followedToken)
            .multiPart("files", img, "image/jpeg")
            .formParam("description", "Feed UI test objava")
            .when().post("/api/v1/post")
            .then().statusCode(200);

        driver = createDriver();
    }

    @AfterAll
    static void teardown() {
        quit(driver);
    }

    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Feed se ucitava i prikazuje bar jednu objavu")
    void feed_shouldShowPosts() {
        loginViaUI(driver, followerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/");

        waitFor(driver, By.xpath(
            "//*[contains(text(),'" + followedUsername + "')]"));
    }

    @Test
    @Order(2)
    @DisplayName("Objava prikazuje opis koji je dodat pri kreiranju")
    void feed_post_shouldShowDescription() {
        driver.get(FRONTEND_URL + "/");
        waitFor(driver, By.xpath("//*[contains(text(),'Feed UI test objava')]"));
    }

    @Test
    @Order(3)
    @DisplayName("Klik na search u navbar-u otvara .search_div panel")
    void navbar_searchClick_shouldOpenSearchPanel() {
        driver.get(FRONTEND_URL + "/");
        click(driver, By.id("search"));
        waitFor(driver, By.className("search_div"));
    }

    @Test
    @Order(4)
    @DisplayName("Navbar prikazuje .profile_pic sliku (link na profil)")
    void navbar_shouldHaveProfilePic() {
        driver.get(FRONTEND_URL + "/");
        assertTrue(waitFor(driver, By.className("profile_pic")).isDisplayed());
    }

    @Test
    @Order(5)
    @DisplayName("Korisnik bez pracenih ima prazan feed")
    void feed_noFollowing_shouldBeEmpty() {
        String loneUser = unique("lone");
        apiRegisterAndLogin(loneUser,
            unique("ln") + "@test.com", PASSWORD, "Lone", "User");
        sleep(500);

        loginViaUI(driver, loneUser, PASSWORD);
        driver.get(FRONTEND_URL + "/");
        sleep(2000);

        List<WebElement> followed = driver.findElements(
            By.xpath("//*[contains(text(),'" + followedUsername + "')]"));
        assertTrue(followed.isEmpty(),
            "Korisnik bez pracenih ne sme videti tudhe objave");
    }

    @Test
    @Order(6)
    @DisplayName("Feed bez tokena preusmerava na /login")
    void feed_noToken_shouldRedirectToLogin() {
        clearSession(driver);
        driver.get(FRONTEND_URL + "/");
        waitForPath(driver, "/login");
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    @Order(7)
    @DisplayName("Scroll do kraja stranice ne baca gresku")
    void feed_scrollToBottom_shouldNotCrash() {
        loginViaUI(driver, followerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/");

        waitFor(driver, By.xpath(
            "//*[contains(text(),'" + followedUsername + "')]"));

        ((JavascriptExecutor) driver)
            .executeScript("window.scrollTo(0, document.body.scrollHeight)");
        sleep(2000);

        List<WebElement> errors = driver.findElements(
            By.xpath("//*[contains(@class,'error-page')]"));
        assertTrue(errors.isEmpty());
    }

    @Test
    @Order(8)
    @DisplayName("Klik na id='more' u navbar-u otvara MorePanel")
    void navbar_moreClick_shouldOpenMorePanel() {
        loginViaUI(driver, followerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/");

        click(driver, By.id("more"));
        assertTrue(waitFor(driver, By.className("button_logout")).isDisplayed());
    }
}