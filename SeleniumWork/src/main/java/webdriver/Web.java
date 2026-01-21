package webdriver;

import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class Web {
    public static void main(String[] args) {
        // Set the system property for ChromeDriver (path to chromedriver executable)
//        System.setProperty("webdriver.chrome.driver", "src/main/resources/driver/chromedriver");

        // Create an instance of ChromeDriver (launch the Chrome browser)
        WebDriver driver = new ChromeDriver();

        try {
            // Navigate to the desired website (GeeksforGeeks in this example)
            driver.get("https://www.geeksforgeeks.org/");

            // Get and print the page title
            String pageTitle = driver.getTitle();
            System.out.println("Page Title: " + pageTitle);

            // Wait for a few seconds (for demonstration purposes only)
            Thread.sleep(3000);


            // Switch to alert
            Alert alert = driver.switchTo().alert();
            alert.accept();   // Click OK
            alert.dismiss();  // Click Cancel
            alert.getText();  // Get alert text

            driver.switchTo().frame("frameName"); // By name/id
            driver.switchTo().frame(0);           // By index
            driver.switchTo().defaultContent();   // Back to main page

            String parentWindow = driver.getWindowHandle();
            for (String handle : driver.getWindowHandles()) {
                driver.switchTo().window(handle);
            }
            // Switch back to parent window
            driver.switchTo().window(parentWindow);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Close the browser
            driver.quit();
        }
    }
}