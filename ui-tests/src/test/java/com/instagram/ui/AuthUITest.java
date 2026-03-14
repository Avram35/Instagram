package com.instagram.ui;

import java.util.List;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import static com.instagram.ui.UITestConfig.apiRegisterAndLogin;
import static com.instagram.ui.UITestConfig.clearSession;
import static com.instagram.ui.UITestConfig.click;
import static com.instagram.ui.UITestConfig.createDriver;
import static com.instagram.ui.UITestConfig.loginViaUI;
import static com.instagram.ui.UITestConfig.quit;
import static com.instagram.ui.UITestConfig.type;
import static com.instagram.ui.UITestConfig.unique;
import static com.instagram.ui.UITestConfig.waitFor;
import static com.instagram.ui.UITestConfig.waitForClickable;
import static com.instagram.ui.UITestConfig.waitForPath;

/**
 * UI integracionih testovi -- Autentikacija
 *
 * Struktura Login.jsx (bez cirilicnih string literala u XPath-u):
 *   Login mod (2 inputa):    input[1]=username/email  input[2]=lozinka
 *   Register mod (6 inputa): input[1]=ime  input[2]=prezime  input[3]=username
 *                             input[4]=email  input[5]=lozinka  input[6]=potvrda
 *
 * CSS klase:
 *   .login-form     - forma
 *   .login_button   - submit dugme
 *   .register-span  - toggle registracija/login
 *   .error          - poruka greske
 *   .show_button    - toggle prikaz lozinke
 *   id="more"       - navbar element koji otvara MorePanel
 *   .button_logout  - dugme za odjavu unutar MorePanel
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("UI -- Autentikacija")
public class AuthUITest {

    private static WebDriver driver;
    private static String testUsername;
    private static String testEmail;
    private static final String PASSWORD = "Test1234!";

    private static By nthInput(int n) {
        return By.xpath(
            "(//form[contains(@class,'login-form')]//input)[" + n + "]");
    }

    @BeforeAll
    static void setup() {
        testUsername = unique("ui_auth");
        testEmail    = unique("uiauth") + "@test.com";
        apiRegisterAndLogin(testUsername, testEmail, PASSWORD, "UI", "Auth");
        driver = createDriver();
    }

    @AfterAll
    static void teardown() {
        quit(driver);
    }

    /**
     * Osigurava cisto stanje: brise sesiju pa navigira na /login.
     * clearSession() prvo navigira na FRONTEND_URL ako je driver jos
     * na data: URL-u, pa tek onda poziva localStorage.clear() --
     * Chrome baca gresku na data: URL-u.
     */
    private void goToLogin() {
        clearSession(driver);
        driver.get(FRONTEND_URL + "/login");
        waitFor(driver, By.className("login-form"));
    }

    // -------------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("Login stranica prikazuje formu sa 2 inputa i submit dugmetom")
    void loginPage_shouldDisplayForm() {
        goToLogin();

        List<WebElement> inputs = driver.findElements(
            By.xpath("//form[contains(@class,'login-form')]//input"));
        assertEquals(2, inputs.size(), "Login forma mora imati tacno 2 input polja");
        assertTrue(waitFor(driver, By.className("login_button")).isDisplayed());
        assertTrue(waitFor(driver, By.className("register-span")).isDisplayed());
    }

    @Test
    @Order(2)
    @DisplayName("Prikaz/sakrivanje lozinke funkcionise")
    void passwordToggle_shouldWork() {
        goToLogin();

        type(driver, nthInput(2), "nekitest");

        WebElement showBtn = waitFor(driver, By.className("show_button"));
        showBtn.click();
        assertEquals("text", driver.findElement(nthInput(2)).getAttribute("type"));

        driver.findElement(By.className("show_button")).click();
        assertEquals("password", driver.findElement(nthInput(2)).getAttribute("type"));
    }

    @Test
    @Order(3)
    @DisplayName("Prijava sa korisnickim imenom preusmerava na Feed")
    void login_withUsername_shouldRedirectToFeed() {
        goToLogin();
        type(driver, nthInput(1), testUsername);
        type(driver, nthInput(2), PASSWORD);
        click(driver, By.className("login_button"));

        new WebDriverWait(driver, DEFAULT_WAIT)
            .until(d -> !d.getCurrentUrl().contains("/login"));
        assertFalse(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    @Order(4)
    @DisplayName("Prijava sa e-adresom preusmerava na Feed")
    void login_withEmail_shouldRedirectToFeed() {
        goToLogin();
        type(driver, nthInput(1), testEmail);
        type(driver, nthInput(2), PASSWORD);
        click(driver, By.className("login_button"));

        new WebDriverWait(driver, DEFAULT_WAIT)
            .until(d -> !d.getCurrentUrl().contains("/login"));
        assertFalse(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    @Order(5)
    @DisplayName("Pogresna lozinka prikazuje gresku i ostaje na /login")
    void login_wrongPassword_shouldShowError() {
        goToLogin();
        type(driver, nthInput(1), testUsername);
        type(driver, nthInput(2), "pogresna_lozinka_xyz");
        click(driver, By.className("login_button"));

        assertFalse(waitFor(driver, By.className("error")).getText().isBlank());
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    @Order(6)
    @DisplayName("Prazna polja prikazuju gresku validacije")
    void login_emptyFields_shouldShowValidationError() {
        goToLogin();
        click(driver, By.className("login_button"));

        assertFalse(waitFor(driver, By.className("error")).getText().isBlank());
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    @Order(7)
    @DisplayName("Nepostojeci korisnik prikazuje gresku")
    void login_nonExistentUser_shouldShowError() {
        goToLogin();
        type(driver, nthInput(1), "nepostoji_xyz_99999");
        type(driver, nthInput(2), PASSWORD);
        click(driver, By.className("login_button"));

        waitFor(driver, By.className("error"));
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    @Order(8)
    @DisplayName("Zasticena ruta bez sesije preusmerava na /login")
    void protectedRoute_noToken_shouldRedirectToLogin() {
        goToLogin();                          // sesija obrisana
        driver.get(FRONTEND_URL + "/");       // pokusaj pristupa feed-u
        waitForPath(driver, "/login");
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    // -------------------------------------------------------------------------
    // Registracija
    // -------------------------------------------------------------------------

    @Test
    @Order(9)
    @DisplayName("Klik na register-span prikazuje formular sa 6 inputa")
    void clickRegister_shouldShowRegistrationForm() {
        goToLogin();
        click(driver, By.className("register-span"));

        new WebDriverWait(driver, DEFAULT_WAIT).until(
            ExpectedConditions.numberOfElementsToBeMoreThan(
                By.xpath("//form[contains(@class,'login-form')]//input"), 2));

        assertEquals(6,
            driver.findElements(
                By.xpath("//form[contains(@class,'login-form')]//input")).size());
    }

    @Test
    @Order(10)
    @DisplayName("Registracija sa razlicitim lozinkama prikazuje gresku")
    void register_passwordMismatch_shouldShowError() {
        goToLogin();
        click(driver, By.className("register-span"));
        new WebDriverWait(driver, DEFAULT_WAIT).until(
            ExpectedConditions.numberOfElementsToBeMoreThan(
                By.xpath("//form[contains(@class,'login-form')]//input"), 2));

        type(driver, nthInput(1), "Test");
        type(driver, nthInput(2), "User");
        type(driver, nthInput(3), unique("mm"));
        type(driver, nthInput(4), unique("mm") + "@t.com");
        type(driver, nthInput(5), "Test1234!");
        type(driver, nthInput(6), "Razlicita5!");
        click(driver, By.className("login_button"));

        assertFalse(waitFor(driver, By.className("error")).getText().isBlank());
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    @Order(11)
    @DisplayName("Registracija sa kratkom lozinkom prikazuje gresku")
    void register_shortPassword_shouldShowError() {
        goToLogin();
        click(driver, By.className("register-span"));
        new WebDriverWait(driver, DEFAULT_WAIT).until(
            ExpectedConditions.numberOfElementsToBeMoreThan(
                By.xpath("//form[contains(@class,'login-form')]//input"), 2));

        type(driver, nthInput(1), "Test");
        type(driver, nthInput(2), "User");
        type(driver, nthInput(3), unique("sp"));
        type(driver, nthInput(4), unique("sp") + "@t.com");
        type(driver, nthInput(5), "abc");
        type(driver, nthInput(6), "abc");
        click(driver, By.className("login_button"));

        assertFalse(waitFor(driver, By.className("error")).getText().isBlank());
    }

    @Test
    @Order(12)
    @DisplayName("Uspesna registracija preusmerava na Feed")
    void register_valid_shouldRedirectToFeed() {
        goToLogin();
        click(driver, By.className("register-span"));
        new WebDriverWait(driver, DEFAULT_WAIT).until(
            ExpectedConditions.numberOfElementsToBeMoreThan(
                By.xpath("//form[contains(@class,'login-form')]//input"), 2));

        type(driver, nthInput(1), "UI");
        type(driver, nthInput(2), "Reg");
        type(driver, nthInput(3), unique("ui_reg"));
        type(driver, nthInput(4), unique("uireg") + "@test.com");
        type(driver, nthInput(5), PASSWORD);
        type(driver, nthInput(6), PASSWORD);
        click(driver, By.className("login_button"));

        new WebDriverWait(driver, DEFAULT_WAIT)
            .until(d -> !d.getCurrentUrl().contains("/login"));
        assertFalse(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    @Order(13)
    @DisplayName("Dupli username pri registraciji prikazuje gresku")
    void register_duplicateUsername_shouldShowError() {
        goToLogin();
        click(driver, By.className("register-span"));
        new WebDriverWait(driver, DEFAULT_WAIT).until(
            ExpectedConditions.numberOfElementsToBeMoreThan(
                By.xpath("//form[contains(@class,'login-form')]//input"), 2));

        type(driver, nthInput(1), "Test");
        type(driver, nthInput(2), "User");
        type(driver, nthInput(3), testUsername);  // vec postoji
        type(driver, nthInput(4), unique("dup") + "@t.com");
        type(driver, nthInput(5), PASSWORD);
        type(driver, nthInput(6), PASSWORD);
        click(driver, By.className("login_button"));

        waitFor(driver, By.className("error"));
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }

    @Test
    @Order(14)
    @DisplayName("Drugi klik na register-span vraca na login formu sa 2 inputa")
    void clickBackToLogin_shouldShowLoginForm() {
        goToLogin();
        click(driver, By.className("register-span"));
        new WebDriverWait(driver, DEFAULT_WAIT).until(
            ExpectedConditions.numberOfElementsToBeMoreThan(
                By.xpath("//form[contains(@class,'login-form')]//input"), 2));

        click(driver, By.className("register-span"));
        new WebDriverWait(driver, DEFAULT_WAIT).until(d ->
            d.findElements(
                By.xpath("//form[contains(@class,'login-form')]//input"))
             .size() == 2);

        assertEquals(2,
            driver.findElements(
                By.xpath("//form[contains(@class,'login-form')]//input")).size());
    }

    @Test
    @Order(15)
    @DisplayName("Odjava korisnika vraca na /login")
    void logout_shouldRedirectToLogin() {
        loginViaUI(driver, testUsername, PASSWORD);

        // Navbar.jsx: <div id="more"> otvara MorePanel
        click(driver, By.id("more"));

        // MorePanel.jsx: <button className="button_logout">
        waitForClickable(driver, By.className("button_logout")).click();

        waitForPath(driver, "/login");
        assertTrue(driver.getCurrentUrl().contains("/login"));
    }
}