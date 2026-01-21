package webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Set;

public class SwitchHandles {

    public static void main(String[] args) {

        // Create an instance of ChromeDriver (launch the Chrome browser)
        WebDriver driver = new ChromeDriver();
        driver.get("https://www.example.com"); // Navigate to the desired webpage

        //Get all the window handles using getWindowHandles method
        Set<String> allWindowHandles = driver.getWindowHandles();

        //Get parent window handles using getWindowHandle method
        String parentWindowHandle = driver.getWindowHandle();

        //Iterate all the window handles
        for (String handle : allWindowHandles) {
            //If window handle is not equal to parent handle, switch to the window handle
            if (!handle.equals(parentWindowHandle)) {
                driver.switchTo().window(handle);
                String childTabTitle = driver.getTitle();
                System.out.println("Child Tab Title: " + childTabTitle);
            }
        }
    }
}
