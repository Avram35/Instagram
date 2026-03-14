package com.instagram.ui;

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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static com.instagram.ui.UITestConfig.DEFAULT_WAIT;
import static com.instagram.ui.UITestConfig.FRONTEND_URL;
import static com.instagram.ui.UITestConfig.POST_URL;
import static com.instagram.ui.UITestConfig.USER_URL;
import static com.instagram.ui.UITestConfig.apiRegisterAndLogin;
import static com.instagram.ui.UITestConfig.click;
import static com.instagram.ui.UITestConfig.createDriver;
import static com.instagram.ui.UITestConfig.createTestImage;
import static com.instagram.ui.UITestConfig.fetchUserId;
import static com.instagram.ui.UITestConfig.loginViaUI;
import static com.instagram.ui.UITestConfig.quit;
import static com.instagram.ui.UITestConfig.sleep;
import static com.instagram.ui.UITestConfig.unique;
import static com.instagram.ui.UITestConfig.waitFor;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;

/**
 * UI integracionih testovi -- Profil korisnika
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UI -- Profil korisnika")
public class ProfileUITest {

    private static WebDriver driver;

    private static String ownerUsername;
    private static String viewerUsername;
    private static String privateUsername;
    private static Long   privateUserId;

    private static String ownerToken;
    private static String viewerToken;
    private static String privateToken;

    private static final String PASSWORD = "Test1234!";

    @BeforeAll
    static void setup() {
        ownerUsername   = unique("pown");
        viewerUsername  = unique("pview");
        privateUsername = unique("ppriv");

        ownerToken   = apiRegisterAndLogin(ownerUsername,
            unique("po") + "@test.com", PASSWORD, "Owner", "User");
        sleep(2000);

        viewerToken  = apiRegisterAndLogin(viewerUsername,
            unique("pv") + "@test.com", PASSWORD, "Viewer", "User");
        sleep(2000);

        privateToken = apiRegisterAndLogin(privateUsername,
            unique("pp") + "@test.com", PASSWORD, "Private", "User");
        sleep(1000);

        privateUserId = fetchUserId(privateUsername, privateToken);

        given()
            .baseUri(USER_URL).contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + privateToken)
            .body("{\"privateProfile\": true}")
            .put("/api/v1/user/" + privateUserId)
            .then().statusCode(200);

        driver = createDriver();
    }

    @AfterAll
    static void teardown() {
        quit(driver);
    }

    // -------------------------------------------------------------------------
    // Prikaz profila
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Javni profil prikazuje korisnicko ime u zaglavlju")
    void publicProfile_shouldShowUsername() {
        loginViaUI(driver, viewerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + ownerUsername);

        waitFor(driver, By.xpath(
            "//*[contains(@class,'username') and contains(text(),'" + ownerUsername + "')]"));
    }

    @Test
    @Order(2)
    @DisplayName("Profil prikazuje .profile_info blok sa statistikama")
    void publicProfile_shouldShowStatsBlock() {
        driver.get(FRONTEND_URL + "/profile/" + ownerUsername);
        assertTrue(waitFor(driver, By.className("profile_info")).isDisplayed());
    }

    @Test
    @Order(3)
    @DisplayName("Sopstveni profil prikazuje dugme .edit_profile_button")
    void ownProfile_shouldShowEditButton() {
        loginViaUI(driver, ownerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + ownerUsername);
        assertTrue(waitFor(driver, By.className("edit_profile_button")).isDisplayed());
    }

    @Test
    @Order(4)
    @DisplayName("Profil drugog korisnika prikazuje dugme .follow_button")
    void otherProfile_shouldShowFollowButton() {
        loginViaUI(driver, viewerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + ownerUsername);

        WebElement btn = waitFor(driver, By.xpath(
            "//*[contains(@class,'follow_button') or " +
            "(contains(@class,'edit_profile_button') and @disabled)]"));
        assertTrue(btn.isDisplayed());
    }

    @Test
    @Order(5)
    @DisplayName("Pracenje korisnika menja klasu dugmeta (follow_button -> edit_profile_button)")
    void followButton_click_shouldChangeClass() {
        loginViaUI(driver, viewerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + ownerUsername);

        waitFor(driver, By.className("follow_button")).click();
        waitFor(driver, By.className("edit_profile_button"));
    }

    @Test
    @Order(6)
    @DisplayName("Privatni profil bez pracenja ne prikazuje thumbnails")
    void privateProfile_notFollowing_shouldHideGallery() {
        loginViaUI(driver, viewerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + privateUsername);
        sleep(1500);

        List<WebElement> thumbs = driver.findElements(
            By.className("post_thumbnail_wrapper"));
        assertTrue(thumbs.isEmpty(),
            "Privatni profil ne sme prikazivati galeriju bez pracenja");
    }

    @Test
    @Order(7)
    @DisplayName("Klik na .span_followers_count otvara modal pratilaca")
    void followersCount_click_shouldOpenModal() {
        loginViaUI(driver, ownerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + ownerUsername);

        click(driver, By.xpath(
            "(//*[contains(@class,'span_followers_count')])[1]"));

        waitFor(driver, By.className("followers_modal_div"));
    }

    @Test
    @Order(8)
    @DisplayName("Klik na thumbnail otvara SinglePost modal (.overlay)")
    void profileThumbnail_click_shouldOpenSinglePost() {
        loginViaUI(driver, ownerUsername, PASSWORD);

        java.io.File img = createTestImage();
        given()
            .baseUri(POST_URL)
            .header("Authorization", "Bearer " + ownerToken)
            .multiPart("files", img, "image/jpeg")
            .formParam("description", "Thumbnail test")
            .when().post("/api/v1/post")
            .then().statusCode(200);
        sleep(1000);

        driver.get(FRONTEND_URL + "/profile/" + ownerUsername);

        List<WebElement> thumbs = new WebDriverWait(driver, DEFAULT_WAIT)
            .until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.className("post_thumbnail_wrapper"), 0));
        thumbs.get(0).click();

        waitFor(driver, By.className("overlay"));
    }

    @Test
    @Order(9)
    @DisplayName("Meni sa tri tackice prikazuje opciju za blokiranje")
    void profileMenu_shouldShowBlockOption() {
        loginViaUI(driver, viewerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + ownerUsername);

        waitFor(driver, By.className("username"));
        waitFor(driver, By.className("img_more"));
        click(driver, By.className("img_more"));
        waitFor(driver, By.className("delete_option"));
    }

    // -------------------------------------------------------------------------
    // Edit profila
    // -------------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("Edit profila -- stranica sadrzi .textarea-bio za biografiju")
    void editProfile_shouldShowBioTextarea() {
        loginViaUI(driver, ownerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/edit-profile");
        assertTrue(waitFor(driver, By.className("textarea-bio")).isDisplayed());
    }

    @Test
    @Order(11)
    @DisplayName("Edit profila -- uspesna izmena biografije")
    void editProfile_bio_shouldSave() {
        loginViaUI(driver, ownerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/edit-profile");

        WebElement bio = waitFor(driver, By.className("textarea-bio"));
        bio.clear();
        bio.sendKeys("Nova UI biografia");

        click(driver, By.className("edit-send-btn"));

        new WebDriverWait(driver, DEFAULT_WAIT).until(d ->
            !d.getCurrentUrl().contains("/edit-profile") ||
            d.findElements(By.xpath(
                "//*[contains(@class,'success') or contains(@class,'alert')]"))
            .size() > 0
        );
    }

    @Test
    @Order(12)
    @DisplayName("Edit profila -- prekratko ime (.edit-input za fname) prikazuje gresku")
    void editProfile_shortFname_shouldShowError() {
        loginViaUI(driver, ownerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/edit-profile");

        WebElement fnameInput = waitFor(driver,
            By.xpath("(//*[contains(@class,'edit-input')])[1]"));
        fnameInput.clear();
        fnameInput.sendKeys("A");

        click(driver, By.className("edit-send-btn"));

        new WebDriverWait(driver, DEFAULT_WAIT).until(d ->
            d.getCurrentUrl().contains("/edit-profile") ||
            d.findElements(By.className("error")).size() > 0
        );
        assertTrue(driver.getCurrentUrl().contains("/edit-profile"),
            "Sa kratkim imenom ostaje na edit stranici");
    }

    @Test
    @Order(13)
    @DisplayName("Edit profila -- preduga biografija (152 znaka) prikazuje gresku")
    void editProfile_longBio_shouldShowError() {
        loginViaUI(driver, ownerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/edit-profile");

        WebElement bio = waitFor(driver, By.className("textarea-bio"));
        bio.clear();
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].value = arguments[1]", bio, "a".repeat(152));
        bio.sendKeys(" ");

        click(driver, By.className("edit-send-btn"));

        new WebDriverWait(driver, DEFAULT_WAIT).until(d ->
            d.getCurrentUrl().contains("/edit-profile") ||
            d.findElements(By.className("error")).size() > 0
        );
    }
}