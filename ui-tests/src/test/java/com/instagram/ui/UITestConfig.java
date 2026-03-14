package com.instagram.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import io.github.bonigarcia.wdm.WebDriverManager;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;

/**
 * Zajednicki helper za sve UI testove.
 */
public class UITestConfig {

    // --- URL-ovi servisa -----------------------------------------------------
    public static final String FRONTEND_URL    = System.getProperty("frontend.url",
            "http://localhost:5173");
    public static final String AUTH_URL        = System.getProperty("auth.url",
            "http://localhost:8081");
    public static final String USER_URL        = System.getProperty("user.url",
            "http://localhost:8082");
    public static final String FOLLOW_URL      = System.getProperty("follow.url",
            "http://localhost:8083");
    public static final String BLOCK_URL       = System.getProperty("block.url",
            "http://localhost:8084");
    public static final String FEED_URL        = System.getProperty("feed.url",
            "http://localhost:8085");
    public static final String POST_URL        = System.getProperty("post.url",
            "http://localhost:8086");
    public static final String INTERACTIVE_URL = System.getProperty("interactive.url",
            "http://localhost:8087");

    // --- Trajanje cekanja ----------------------------------------------------
    public static final Duration DEFAULT_WAIT = Duration.ofSeconds(10);
    public static final Duration SHORT_WAIT   = Duration.ofSeconds(4);

    // --- Counter za garantovanu jedinstvenost --------------------------------
    private static final AtomicLong COUNTER = new AtomicLong(0);

    // =========================================================================
    // Pravi SOF0 JPEG koji browser moze prikazati (1x1 piksel, sivi)
    // =========================================================================
    private static final byte[] VALID_JPEG_BYTES = new byte[]{
        (byte)0xFF,(byte)0xD8,(byte)0xFF,(byte)0xE0, // SOI + APP0
        0x00,0x10,0x4A,0x46,0x49,0x46,0x00,
        0x01,0x01,0x00,0x00,0x01,0x00,0x01,0x00,0x00,

        (byte)0xFF,(byte)0xDB,0x00,0x43,0x00,        // DQT
        0x08,0x06,0x06,0x07,0x06,0x05,0x08,0x07,
        0x07,0x07,0x09,0x09,0x08,0x0A,0x0C,0x14,
        0x0D,0x0C,0x0B,0x0B,0x0C,0x19,0x12,0x13,
        0x0F,0x14,0x1D,0x1A,0x1F,0x1E,0x1D,0x1A,
        0x1C,0x1C,0x20,0x24,0x2E,0x27,0x20,0x22,
        0x2C,0x23,0x1C,0x1C,0x28,0x37,0x29,0x2C,
        0x30,0x31,0x34,0x34,0x34,0x1F,0x27,0x39,
        0x3D,0x38,0x32,0x3C,0x2E,0x33,0x34,0x32,

        (byte)0xFF,(byte)0xC0,0x00,0x0B,             // SOF0
        0x08,0x00,0x01,0x00,0x01,
        0x01,0x01,0x11,0x00,

        (byte)0xFF,(byte)0xC4,0x00,0x1F,0x00,        // DHT DC
        0x00,0x01,0x05,0x01,0x01,0x01,0x01,0x01,
        0x01,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
        0x00,0x01,0x02,0x03,0x04,0x05,0x06,0x07,
        0x08,0x09,0x0A,0x0B,

        (byte)0xFF,(byte)0xC4,0x00,(byte)0xB5,0x10,  // DHT AC
        0x00,0x02,0x01,0x03,0x03,0x02,0x04,0x03,
        0x05,0x05,0x04,0x04,0x00,0x00,0x01,0x7D,
        0x01,0x02,0x03,0x00,0x04,0x11,0x05,0x12,
        0x21,0x31,0x41,0x06,0x13,0x51,0x61,0x07,
        0x22,0x71,0x14,0x32,(byte)0x81,(byte)0x91,(byte)0xA1,0x08,
        0x23,0x42,(byte)0xB1,(byte)0xC1,0x15,0x52,(byte)0xD1,(byte)0xF0,
        0x24,0x33,0x62,0x72,(byte)0x82,0x09,0x0A,0x16,
        0x17,0x18,0x19,0x1A,0x25,0x26,0x27,0x28,
        0x29,0x2A,0x34,0x35,0x36,0x37,0x38,0x39,
        0x3A,0x43,0x44,0x45,0x46,0x47,0x48,0x49,
        0x4A,0x53,0x54,0x55,0x56,0x57,0x58,0x59,
        0x5A,0x63,0x64,0x65,0x66,0x67,0x68,0x69,
        0x6A,0x73,0x74,0x75,0x76,0x77,0x78,0x79,
        0x7A,(byte)0x83,(byte)0x84,(byte)0x85,(byte)0x86,(byte)0x87,
        (byte)0x88,(byte)0x89,(byte)0x8A,(byte)0x92,(byte)0x93,(byte)0x94,
        (byte)0x95,(byte)0x96,(byte)0x97,(byte)0x98,(byte)0x99,(byte)0x9A,
        (byte)0xA2,(byte)0xA3,(byte)0xA4,(byte)0xA5,(byte)0xA6,(byte)0xA7,
        (byte)0xA8,(byte)0xA9,(byte)0xAA,(byte)0xB2,(byte)0xB3,(byte)0xB4,
        (byte)0xB5,(byte)0xB6,(byte)0xB7,(byte)0xB8,(byte)0xB9,(byte)0xBA,
        (byte)0xC2,(byte)0xC3,(byte)0xC4,(byte)0xC5,(byte)0xC6,(byte)0xC7,
        (byte)0xC8,(byte)0xC9,(byte)0xCA,(byte)0xD2,(byte)0xD3,(byte)0xD4,
        (byte)0xD5,(byte)0xD6,(byte)0xD7,(byte)0xD8,(byte)0xD9,(byte)0xDA,
        (byte)0xE1,(byte)0xE2,(byte)0xE3,(byte)0xE4,(byte)0xE5,(byte)0xE6,
        (byte)0xE7,(byte)0xE8,(byte)0xE9,(byte)0xEA,(byte)0xF1,(byte)0xF2,
        (byte)0xF3,(byte)0xF4,(byte)0xF5,(byte)0xF6,(byte)0xF7,(byte)0xF8,
        (byte)0xF9,(byte)0xFA,

        (byte)0xFF,(byte)0xDA,0x00,0x08,             // SOS
        0x01,0x01,0x00,0x00,0x3F,0x00,
        (byte)0xFB,(byte)0xD3,                       // compressed data

        (byte)0xFF,(byte)0xD9                        // EOI
    };

    // --- WebDriver fabrika ---------------------------------------------------
    public static WebDriver createDriver() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions opts = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("headless", "false"))) {
            opts.addArguments("--headless=new");
        }
        opts.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--window-size=1280,900"
        );

        WebDriver driver = new ChromeDriver(opts);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(0));
        return driver;
    }

    public static void quit(WebDriver driver) {
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
        }
    }

    // --- Test slika ----------------------------------------------------------

    /**
     * Kreira privremeni JPEG fajl koji je validan i koji browser moze prikazati.
     * Svi testovi treba da koriste ovu metodu umesto lokalnih createMinimalJpeg().
     */
    public static File createTestImage() {
        try {
            File img = File.createTempFile("ui_test_img", ".jpg");
            try (FileOutputStream fos = new FileOutputStream(img)) {
                fos.write(VALID_JPEG_BYTES);
            }
            img.deleteOnExit();
            return img;
        } catch (Exception e) {
            throw new RuntimeException(
                "Ne moze kreirati test sliku: " + e.getMessage(), e);
        }
    }

    // --- Sesija / localStorage -----------------------------------------------

    public static void clearSession(WebDriver driver) {
        String current = driver.getCurrentUrl();
        if (current == null || !current.startsWith("http")) {
            driver.get(FRONTEND_URL + "/login");
        }
        ((JavascriptExecutor) driver).executeScript("localStorage.clear()");
    }

    // --- Cekanje na elemente -------------------------------------------------

    public static WebElement waitFor(WebDriver driver, By locator) {
        return new WebDriverWait(driver, DEFAULT_WAIT)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForClickable(WebDriver driver, By locator) {
        return new WebDriverWait(driver, DEFAULT_WAIT)
                .until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static void waitForPath(WebDriver driver, String path) {
        new WebDriverWait(driver, DEFAULT_WAIT)
                .until(d -> d.getCurrentUrl().contains(path));
    }

    public static void waitForInvisibility(WebDriver driver, By locator) {
        new WebDriverWait(driver, DEFAULT_WAIT)
                .until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    // --- Interakcija ---------------------------------------------------------

    public static void click(WebDriver driver, By locator) {
        waitForClickable(driver, locator).click();
    }

    /**
     * Klik sa JS fallback-om za elemente koji nisu "clickable" po Selenium
     * standardu (npr. <span className="publish_post"> u CreatePost.jsx).
     */
    public static void clickJs(WebDriver driver, By locator) {
        WebElement el = waitFor(driver, locator);
        try {
            el.click();
        } catch (Exception e) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click()", el);
        }
    }

    public static void type(WebDriver driver, By locator, String text) {
        WebElement el = waitFor(driver, locator);
        el.clear();
        el.sendKeys(text);
    }

    // --- REST helper ---------------------------------------------------------

    /**
     * Registruje i loguje korisnika putem API-ja sa retry mehanizmom.
     */
    public static String apiRegisterAndLogin(String username, String email,
                                              String password,
                                              String fname, String lname) {
        sleep(800);

        int signupStatus = 0;
        for (int attempt = 0; attempt < 3; attempt++) {
            signupStatus = given()
                .baseUri(AUTH_URL)
                .contentType(ContentType.JSON)
                .body(String.format(
                    "{\"username\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"," +
                    "\"fname\":\"%s\",\"lname\":\"%s\"}",
                    username, email, password, fname, lname))
                .when().post("/api/v1/auth/signup")
                .then().extract().statusCode();

            if (signupStatus == 201) break;
            if (signupStatus == 409) break; // Vec postoji -- ok
            sleep(600 * (attempt + 1));
        }

        if (signupStatus != 201) {
            throw new RuntimeException(
                "Registracija nije uspela za '" + username +
                "', status: " + signupStatus);
        }

        sleep(300);
        return given()
            .baseUri(AUTH_URL)
            .contentType(ContentType.JSON)
            .body(String.format(
                "{\"usernameOrEmail\":\"%s\",\"password\":\"%s\"}",
                username, password))
            .when().post("/api/v1/auth/signin")
            .then().statusCode(200)
            .extract().path("token");
    }

    /**
     * Dohvata ID korisnika sa retry mehanizmom (do 5 pokusaja).
     * User-service ponekad nije propagovao nalog odmah posle registracije.
     */
    public static Long fetchUserId(String username, String token) {
        for (int attempt = 0; attempt < 5; attempt++) {
            int status = given()
                .baseUri(USER_URL).contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .get("/api/v1/user/" + username)
                .then().extract().statusCode();

            if (status == 200) {
                Object raw = given()
                    .baseUri(USER_URL).contentType(ContentType.JSON)
                    .header("Authorization", "Bearer " + token)
                    .get("/api/v1/user/" + username)
                    .then().statusCode(200).extract().path("id");
                return raw instanceof Long ? (Long) raw : ((Integer) raw).longValue();
            }
            sleep(1000 * (attempt + 1));
        }
        throw new RuntimeException(
            "Ne moze da dobi ID za korisnika: " + username);
    }

    /**
     * Prijavljuje korisnika kroz UI.
     */
    public static void loginViaUI(WebDriver driver,
                                   String usernameOrEmail, String password) {
        clearSession(driver);
        driver.get(FRONTEND_URL + "/login");
        waitFor(driver, By.className("login-form"));

        type(driver,
            By.xpath("(//form[contains(@class,'login-form')]//input)[1]"),
            usernameOrEmail);
        type(driver,
            By.xpath("(//form[contains(@class,'login-form')]//input)[2]"),
            password);
        click(driver, By.className("login_button"));

        new WebDriverWait(driver, DEFAULT_WAIT)
            .until(d -> !d.getCurrentUrl().contains("/login"));
    }

    // --- Utility -------------------------------------------------------------

    /**
     * Generise jedinstveni string sa prefiksom.
     * Kombinuje timestamp i atomski brojac da garantuje jedinstvenost.
     */
    public static String unique(String prefix) {
        long ts = System.currentTimeMillis() % 1_000_000_000L;
        long cnt = COUNTER.incrementAndGet() % 1000;
        return prefix + "_" + ts + cnt;
    }

    public static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}