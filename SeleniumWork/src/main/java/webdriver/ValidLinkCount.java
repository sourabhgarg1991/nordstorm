package webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class ValidLinkCount {

    public static void main(String[] args) {

        // Create an instance of ChromeDriver (launch the Chrome browser)
        WebDriver driver = new ChromeDriver();
        driver.get("https://www.example.com"); // Navigate to the desired webpage

        //Get all links using tagname a
        List<WebElement> links = driver.findElements(By.tagName("a"));
        int validLinkCount = 0;


        //Iterate all the links
        for (WebElement link : links) {
            //Get url from the link
            String url = link.getAttribute("href");

            if (url != null && !url.isEmpty()) {
                try {

                    //Connect to URL using httpConnection
                    HttpURLConnection huc = (HttpURLConnection) new URL(url).openConnection();
                    huc.setRequestMethod("HEAD"); // Use HEAD request for efficiency
                    huc.connect();

                    //Get response code
                    int respCode = huc.getResponseCode();

                    //Verify that response code is valid
                    if (respCode >= 200 && respCode < 300) {
                        //Increment the valid link counter
                        validLinkCount++;
                    }
                } catch (Exception e) {
                    // Handle exceptions (e.g., malformed URL, network issues)
                    System.out.println("Error checking link: " + url + " - " + e.getMessage());
                }
            }
            System.out.println(validLinkCount);
        }
    }
}
