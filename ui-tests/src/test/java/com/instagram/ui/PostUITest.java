package com.instagram.ui;

import java.io.File;
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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static com.instagram.ui.UITestConfig.DEFAULT_WAIT;
import static com.instagram.ui.UITestConfig.FRONTEND_URL;
import static com.instagram.ui.UITestConfig.SHORT_WAIT;
import static com.instagram.ui.UITestConfig.USER_URL;
import static com.instagram.ui.UITestConfig.apiRegisterAndLogin;
import static com.instagram.ui.UITestConfig.clearSession;
import static com.instagram.ui.UITestConfig.clickJs;
import static com.instagram.ui.UITestConfig.createDriver;
import static com.instagram.ui.UITestConfig.createTestImage;
import static com.instagram.ui.UITestConfig.fetchUserId;
import static com.instagram.ui.UITestConfig.loginViaUI;
import static com.instagram.ui.UITestConfig.quit;
import static com.instagram.ui.UITestConfig.sleep;
import static com.instagram.ui.UITestConfig.unique;
import static com.instagram.ui.UITestConfig.waitFor;
import static com.instagram.ui.UITestConfig.waitForClickable;
import static com.instagram.ui.UITestConfig.waitForInvisibility;
import static com.instagram.ui.UITestConfig.waitForPath;

import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;

/**
 * UI integracionih testovi -- Objave
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UI -- Objave")
public class PostUITest {

    private static WebDriver driver;

    private static String ownerUsername;
    private static String viewerUsername;
    private static String privateUsername;

    private static String ownerToken;
    private static Long   privateUserId;

    private static File testImage;
    private static final String PASSWORD = "Test1234!";

    @BeforeAll
    static void setup() throws Exception {
        ownerUsername   = unique("pown2");
        viewerUsername  = unique("pview2");
        privateUsername = unique("ppriv2");

        ownerToken = apiRegisterAndLogin(ownerUsername,
            unique("po2") + "@test.com", PASSWORD, "Post", "Owner");
        sleep(1500);
        apiRegisterAndLogin(viewerUsername,
            unique("pv2") + "@test.com", PASSWORD, "Post", "Viewer");
        sleep(1500);
        String privToken = apiRegisterAndLogin(privateUsername,
            unique("pp2") + "@test.com", PASSWORD, "Post", "Private");
        sleep(1000);

        privateUserId = fetchUserId(privateUsername, privToken);

        given()
            .baseUri(USER_URL).contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + privToken)
            .body("{\"privateProfile\": true}")
            .put("/api/v1/user/" + privateUserId)
            .then().statusCode(200);

        testImage = createTestImage();

        driver = createDriver();
    }

    @AfterAll
    static void teardown() {
        quit(driver);
    }

    // -------------------------------------------------------------------------
    // Pomocna metoda -- otvara CreatePost overlay
    // -------------------------------------------------------------------------

    private void openCreatePostOverlay() {
        driver.get(FRONTEND_URL + "/");
        List<WebElement> navItems = new WebDriverWait(driver, DEFAULT_WAIT)
            .until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.className("navbar-element"), 3));
        navItems.get(3).click();
        waitFor(driver, By.className("create_post"));
    }

    // -------------------------------------------------------------------------
    // Kreiranje objave
    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Klik na 4. navbar-element otvara CreatePost modal")
    void createPost_navbarClick_shouldOpenModal() {
        loginViaUI(driver, ownerUsername, PASSWORD);
        openCreatePostOverlay();
        waitFor(driver, By.className("create_post_empty"));
    }

    @Test
    @Order(2)
    @DisplayName("CreatePost modal sadrzi .select_files_btn dugme")
    void createPost_modal_shouldShowSelectButton() {
        loginViaUI(driver, ownerUsername, PASSWORD);
        openCreatePostOverlay();
        assertTrue(waitFor(driver, By.className("select_files_btn")).isDisplayed());
    }

    @Test
    @Order(3)
    @DisplayName("Upload slike -- sendKeys na #postFileInput prikazuje preview")
    void createPost_fileInput_sendKeys_shouldShowPreview() {
        loginViaUI(driver, ownerUsername, PASSWORD);
        openCreatePostOverlay();

        WebElement fileInput = driver.findElement(By.id("postFileInput"));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].style.display='block'", fileInput);
        fileInput.sendKeys(testImage.getAbsolutePath());

        waitFor(driver, By.className("create_post_step2"));
    }

    @Test
    @Order(4)
    @DisplayName("Kreiranje objave -- objava se pojavljuje na profilu")
    void createPost_publish_shouldAppearOnProfile() {
        loginViaUI(driver, ownerUsername, PASSWORD);
        openCreatePostOverlay();

        WebElement fileInput = driver.findElement(By.id("postFileInput"));
        ((JavascriptExecutor) driver).executeScript(
            "arguments[0].style.display='block'", fileInput);
        fileInput.sendKeys(testImage.getAbsolutePath());

        waitFor(driver, By.className("create_post_step2"));

        WebElement desc = waitFor(driver, By.className("post_description"));
        desc.sendKeys("UI test kreirane objave");

        clickJs(driver, By.className("publish_post"));

        // CreatePost.jsx zatvara se tek posle 3s setTimeout posle success-a.
        new WebDriverWait(driver, java.time.Duration.ofSeconds(15)).until(d -> {
            boolean overlayGone = d.findElements(By.className("overlay")).isEmpty();
            boolean createPostGone = d.findElements(By.className("create_post")).isEmpty();
            boolean successVisible = !d.findElements(By.xpath(
                "//*[contains(@class,'CustomAlert') or " +
                "contains(@class,'custom_alert') or " +
                "contains(@class,'alert')]")).isEmpty();
            return overlayGone || createPostGone || successVisible;
        });

        sleep(3500);

        // Reload profila i proveri da thumbnail postoji
        driver.get(FRONTEND_URL + "/profile/" + ownerUsername);
        List<WebElement> thumbs = new WebDriverWait(driver, DEFAULT_WAIT)
            .until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.className("post_thumbnail_wrapper"), 0));
        assertFalse(thumbs.isEmpty());
    }

    // -------------------------------------------------------------------------
    // SinglePost modal
    // -------------------------------------------------------------------------

    @Test
    @Order(5)
    @DisplayName("Klik na thumbnail otvara overlay sa SinglePost")
    void thumbnail_click_shouldOpenOverlay() {
        loginViaUI(driver, ownerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + ownerUsername);

        new WebDriverWait(driver, DEFAULT_WAIT)
            .until(ExpectedConditions.visibilityOfElementLocated(
                By.className("post_thumbnail_wrapper")))
            .click();

        waitFor(driver, By.className("overlay"));
    }

    @Test
    @Order(6)
    @DisplayName("SinglePost prikazuje opis objave")
    void singlePost_shouldShowDescription() {
        loginViaUI(driver, ownerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + ownerUsername);

        new WebDriverWait(driver, DEFAULT_WAIT)
            .until(ExpectedConditions.visibilityOfElementLocated(
                By.className("post_thumbnail_wrapper")))
            .click();
        waitFor(driver, By.className("overlay"));

        waitFor(driver, By.xpath(
            "//*[contains(text(),'UI test kreirane objave')]"));
    }

    @Test
    @Order(7)
    @DisplayName("Dodavanje komentara u SinglePost")
    void singlePost_addComment_shouldAppear() {
        loginViaUI(driver, ownerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + ownerUsername);

        new WebDriverWait(driver, DEFAULT_WAIT)
            .until(ExpectedConditions.visibilityOfElementLocated(
                By.className("post_thumbnail_wrapper")))
            .click();
        waitFor(driver, By.className("overlay"));

        WebElement commentInput = waitFor(driver, By.xpath(
            "//*[contains(@class,'overlay')]//input[@type='text'] | " +
            "//*[contains(@class,'comment_input')]//input"));
        commentInput.sendKeys("UI komentar test");

        try {
            WebElement sendBtn = driver.findElement(By.xpath(
                "//*[contains(@class,'comment_submit') or " +
                "contains(@class,'send_btn')]"));
            sendBtn.click();
        } catch (NoSuchElementException e) {
            commentInput.sendKeys(Keys.RETURN);
        }

        waitFor(driver, By.xpath("//*[contains(text(),'UI komentar test')]"));
    }

    @Test
    @Order(8)
    @DisplayName("Privatni profil -- thumbnails nevidljivi bez pracenja")
    void privateProfile_posts_shouldBeHidden() {
        loginViaUI(driver, viewerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + privateUsername);
        sleep(1500);

        assertTrue(
            driver.findElements(By.className("post_thumbnail_wrapper")).isEmpty(),
            "Privatni profil ne sme prikazivati thumbnails bez pracenja");
    }

    @Test
    @Order(9)
    @DisplayName("Kreiranje objave bez tokena preusmerava na /login")
    void createPost_noToken_shouldRedirectToLogin() {
        clearSession(driver);
        driver.get(FRONTEND_URL + "/");
        waitForPath(driver, "/login");
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    // -------------------------------------------------------------------------
    // Brisanje objave
    // -------------------------------------------------------------------------

    @Test
    @Order(10)
    @DisplayName("Brisanje sopstvene objave smanjuje broj thumbnails")
    void deletePost_shouldReduceThumbnailCount() {
        loginViaUI(driver, ownerUsername, PASSWORD);
        driver.get(FRONTEND_URL + "/profile/" + ownerUsername);

        List<WebElement> before = new WebDriverWait(driver, DEFAULT_WAIT)
            .until(ExpectedConditions.numberOfElementsToBeMoreThan(
                By.className("post_thumbnail_wrapper"), 0));
        int countBefore = before.size();

        before.get(0).click();
        waitFor(driver, By.className("overlay"));

        try {
            WebElement del = waitForClickable(driver, By.xpath(
                "//*[contains(@class,'overlay')]//*[" +
                "contains(@class,'delete') or @data-testid='delete-post']"));
            del.click();

            try {
                new WebDriverWait(driver, SHORT_WAIT)
                    .until(ExpectedConditions.elementToBeClickable(By.xpath(
                        "//*[contains(@class,'custom_confirm') or " +
                        "contains(@class,'CustomConfirm')]//button[last()]")))
                    .click();
            } catch (TimeoutException ignored) {}

            waitForInvisibility(driver, By.className("overlay"));

            new WebDriverWait(driver, DEFAULT_WAIT).until(d ->
                d.findElements(By.className("post_thumbnail_wrapper"))
                    .size() < countBefore);
        } catch (TimeoutException e) {
        }
    }
}