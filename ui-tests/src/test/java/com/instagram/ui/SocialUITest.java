package com.instagram.ui;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static com.instagram.ui.UITestConfig.DEFAULT_WAIT;
import static com.instagram.ui.UITestConfig.FRONTEND_URL;
import static com.instagram.ui.UITestConfig.USER_URL;
import static com.instagram.ui.UITestConfig.apiRegisterAndLogin;
import static com.instagram.ui.UITestConfig.click;
import static com.instagram.ui.UITestConfig.createDriver;
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
 * UI integracionih testovi -- Pracenje, blokiranje i pretraga
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UI -- Pracenje, blokiranje i pretraga")
public class SocialUITest {

    private static WebDriver driver;

    private static String userAUsername;
    private static String userBUsername;
    private static String userCUsername;

    private static String tokenA;
    private static String tokenC;
    private static Long   userCId;

    private static final String PASSWORD = "Test1234!";

    @BeforeAll
    static void setup() {
        userAUsername = unique("sa");
        userBUsername = unique("sb");
        userCUsername = unique("sc");

        tokenA = apiRegisterAndLogin(userAUsername,
            unique("ea") + "@test.com", PASSWORD, "Social", "Aa");
        sleep(2000);

        apiRegisterAndLogin(userBUsername,
            unique("eb") + "@test.com", PASSWORD, "Social", "Bb");
        sleep(2000);

        tokenC = apiRegisterAndLogin(userCUsername,
            unique("ec") + "@test.com", PASSWORD, "Social", "Cc");
        sleep(1000);

        userCId = fetchUserId(userCUsername, tokenC);

        given()
            .baseUri(USER_URL).contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + tokenC)
            .body("{\"privateProfile\": true}")
            .put("/api/v1/user/" + userCId)
            .then().statusCode(200);

        driver = createDriver();
    }

    @AfterAll
    static void teardown() {
        quit(driver);
    }

    // -------------------------------------------------------------------------
    // Pretraga
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Klik na id='search' otvara .search_div panel")
    void navbar_searchClick_shouldOpenSearchPanel() {
        loginViaUI(driver, userAUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/");

        click(driver, By.id("search"));
        waitFor(driver, By.className("search_div"));
    }

    @Test
    @Order(2)
    @DisplayName("Unos teksta u .search_input vraca .search_profile rezultate")
    void search_input_shouldReturnResults() {
        loginViaUI(driver, userAUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/");

        click(driver, By.id("search"));
        waitFor(driver, By.className("search_div"));

        WebElement input = waitFor(driver,
            By.xpath("//*[contains(@class,'search_input')]//input"));
        input.sendKeys(userBUsername.substring(0, 5));

        List<WebElement> results = new WebDriverWait(driver, DEFAULT_WAIT)
            .until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.className("search_profile"), 0));
        assertFalse(results.isEmpty());
    }

    @Test
    @Order(3)
    @DisplayName("Rezultat pretrage prikazuje .search_username")
    void search_result_shouldShowUsername() {
        loginViaUI(driver, userAUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/");

        click(driver, By.id("search"));
        WebElement input = waitFor(driver,
            By.xpath("//*[contains(@class,'search_input')]//input"));
        input.sendKeys(userBUsername.substring(0, 5));

        waitFor(driver, By.xpath(
            "//*[contains(@class,'search_username') and " +
            "contains(text(),'" + userBUsername.substring(0, 5) + "')]"));
    }

    @Test
    @Order(4)
    @DisplayName("Klik na .search_profile otvara profil")
    void search_resultClick_shouldOpenProfile() {
        loginViaUI(driver, userAUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/");

        click(driver, By.id("search"));
        WebElement input = waitFor(driver,
            By.xpath("//*[contains(@class,'search_input')]//input"));
        input.sendKeys(userBUsername.substring(0, 5));

        waitFor(driver, By.className("search_profile")).click();

        waitForPath(driver, "/profile/");
        assertTrue(driver.getCurrentUrl().contains("/profile/"));
    }

    @Test
    @Order(5)
    @DisplayName("Brisanje pretrage uklanja .search_profile rezultate")
    void search_clear_shouldRemoveResults() {
        loginViaUI(driver, userAUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/");

        click(driver, By.id("search"));
        WebElement input = waitFor(driver,
            By.xpath("//*[contains(@class,'search_input')]//input"));
        input.sendKeys(userBUsername.substring(0, 5));

        new WebDriverWait(driver, DEFAULT_WAIT)
            .until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.className("search_profile"), 0));

        input.sendKeys(org.openqa.selenium.Keys.CONTROL + "a");
        input.sendKeys(org.openqa.selenium.Keys.DELETE);

        String currentVal = input.getAttribute("value");
        if (currentVal != null && !currentVal.isEmpty()) {
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "var el = arguments[0];" +
                "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(" +
                "  window.HTMLInputElement.prototype, 'value').set;" +
                "nativeInputValueSetter.call(el, '');" +
                "el.dispatchEvent(new Event('input', { bubbles: true }));",
                input);
        }

        sleep(1200);

        List<WebElement> results = driver.findElements(By.className("search_profile"));
        assertTrue(results.isEmpty() ||
            results.stream().noneMatch(WebElement::isDisplayed));
    }

    // -------------------------------------------------------------------------
    // Pracenje
    // -------------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("Pracenje javnog korisnika -- .follow_button postaje .edit_profile_button")
    void follow_publicUser_shouldChangeButtonClass() {
        loginViaUI(driver, userAUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + userBUsername);

        waitFor(driver, By.className("follow_button")).click();
        waitFor(driver, By.className("edit_profile_button"));
    }

    @Test
    @Order(7)
    @DisplayName("Modal pratilaca -- .span_followers_count klik otvara .followers_modal_div")
    void followersModal_shouldOpen() {
        loginViaUI(driver, userAUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + userBUsername);

        click(driver, By.xpath("(//*[contains(@class,'span_followers_count')])[1]"));
        waitFor(driver, By.className("followers_modal_div"));
    }

    @Test
    @Order(8)
    @DisplayName("Modal pratilaca sadrzi .follower_row elemente")
    void followersModal_shouldContainRows() {
        loginViaUI(driver, userAUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + userBUsername);

        click(driver, By.xpath("(//*[contains(@class,'span_followers_count')])[1]"));
        waitFor(driver, By.className("followers_modal_div"));

        List<WebElement> rows = new WebDriverWait(driver, DEFAULT_WAIT)
            .until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.className("follower_row"), 0));
        assertFalse(rows.isEmpty());
    }

    @Test
    @Order(9)
    @DisplayName("Otpracivanje korisnika -- .edit_profile_button postaje .follow_button")
    void unfollow_shouldRevertToFollowButton() {
        loginViaUI(driver, userAUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + userBUsername);

        waitFor(driver, By.className("edit_profile_button")).click();
        waitFor(driver, By.className("follow_button"));
    }

    @Test
    @Order(10)
    @DisplayName("Pracenje privatnog profila -- .follow_button postaje .edit_profile_button")
    void follow_privateUser_shouldChangeToPending() {
        loginViaUI(driver, userAUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + userCUsername);

        waitFor(driver, By.className("follow_button")).click();
        waitFor(driver, By.className("edit_profile_button"));
    }

    // -------------------------------------------------------------------------
    // Blokiranje
    // -------------------------------------------------------------------------

    @Test
    @Order(11)
    @DisplayName("Klik na .img_more prikazuje .post_menu sa .delete_option")
    void profileMenu_shouldShowBlockOption() {
        loginViaUI(driver, userAUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + userBUsername);

        waitFor(driver, By.className("img_more"));
        click(driver, By.className("img_more"));
        waitFor(driver, By.className("delete_option"));
    }

    // -------------------------------------------------------------------------
    // Notifikacije i MorePanel
    // -------------------------------------------------------------------------

    @Test
    @Order(12)
    @DisplayName("Klik na id='notification' otvara panel notifikacija")
    void navbar_notificationClick_shouldOpenPanel() {
        loginViaUI(driver, userAUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/");

        click(driver, By.id("notification"));
        waitFor(driver, By.className("notification_div"));
    }

    @Test
    @Order(13)
    @DisplayName("Klik na id='more' prikazuje .button_logout u MorePanel")
    void navbar_moreClick_shouldShowLogoutButton() {
        loginViaUI(driver, userAUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/");

        click(driver, By.id("more"));
        assertTrue(waitFor(driver, By.className("button_logout")).isDisplayed());
    }
}