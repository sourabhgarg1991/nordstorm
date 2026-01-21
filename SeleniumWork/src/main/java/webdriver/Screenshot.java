package webdriver;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.apache.commons.io.FileUtils;
import java.io.File;

public class Screenshot {
    public static void captureScreenshot(WebDriver driver, String screenshotName) {
        try {
            // 1. Convert WebDriver object to TakesScreenshot
            TakesScreenshot ts = (TakesScreenshot) driver;

            // 2. Capture the screenshot as a file
            File source = ts.getScreenshotAs(OutputType.FILE);

            // Define the destination path, typically in a 'screenshots' folder
            String destination = System.getProperty("user.dir") +
                    File.separator + "screenshots" +
                    File.separator + screenshotName + ".png";

            File finalDestination = new File(destination);

            // Ensure the directory exists
            finalDestination.getParentFile().mkdirs();

            // 3. Copy the file from source to destination
            FileUtils.copyFile(source, finalDestination);

            System.out.println("Screenshot saved successfully to: " + destination);

        } catch (Exception e) {
            System.out.println("Exception while taking screenshot: " + e.getMessage());
        }
    }
}