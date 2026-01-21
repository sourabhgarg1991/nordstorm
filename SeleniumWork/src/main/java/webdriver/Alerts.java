package webdriver;

import org.openqa.selenium.Alert;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class Alerts {

    public static void main(String args[]){
        WebDriver driver = new ChromeDriver();

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
    }
}
