package data.provider;

import org.testng.annotations.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.By;
import org.testng.Assert;

public class LoginTest {

    @Test(dataProvider = "loginData", dataProviderClass = LoginCredDataProviders.class)
    public void testLogin(String username, String password) {
        WebDriver driver = new ChromeDriver();
        driver.get("https://www.example.com/login"); // Replace with your login URL

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("loginButton")).click();

        // Add assertions to verify login success or failure
        Assert.assertTrue(driver.getCurrentUrl().contains("dashboard"));

        driver.quit();
    }

    @Test(dataProvider = "csvData", dataProviderClass = CsvDataProviders.class)
    public void testLoginUsingCsv(String username, String password, String expected) {
        WebDriver driver = new ChromeDriver();
        driver.get("https://practicetestautomation.com/practice-test-login/"); // Replace with your login URL

        driver.findElement(By.id("username")).sendKeys(username);
        driver.findElement(By.id("password")).sendKeys(password);
        driver.findElement(By.id("submit")).click();

        // Add assertions to verify login success or failure
        Assert.assertTrue(driver.getCurrentUrl().contains(expected));

        driver.quit();
    }
}