package webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class AutoSuggest {

    public static void main(String[] args) {

        // Create an instance of ChromeDriver (launch the Chrome browser)
        WebDriver driver = new ChromeDriver();

        // Item to be searched
        String item = "Blue Top";
        try {
            // Navigate to the desired website
            driver.get("https://automationexercise.com/");
            autoSuggest(driver, item);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the browser
            driver.quit();
        }
    }

    public static void autoSuggest(WebDriver driver, String input) throws InterruptedException {

        //find element field which has auto complete and enter few letters using sendKeys
        driver.findElement(By.id("autosuggest")).sendKeys("blu");

        //Find suggestion list using css selector
        List<WebElement> items = driver.findElements(By.cssSelector("suggestions"));

        //Iterate the item list
        for (WebElement item : items) {

            //If item matches the input, click on the match
            if (item.getText().contains(input)) {
                item.click();
                break;
            }
        }
    }
}
