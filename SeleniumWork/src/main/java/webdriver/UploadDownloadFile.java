package webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class UploadDownloadFile {

    public static void main(String[] args) {
        // Create an instance of ChromeDriver (launch the Chrome browser)
        WebDriver driver = new ChromeDriver();

        uploadFile(driver);

    }

    public static void uploadFile(WebDriver driver) {
        // Navigate to the desired website (GeeksforGeeks in this example)
        driver.get("https://www.tutorialspoint.com/selenium/practice/selenium_automation_practice.php");

        WebElement fileInput = driver.findElement(By.id("picture"));

        // Create a File object for the file to upload
        File uploadFile = new File("src/main/resources/input.jpeg"); // Replace with your file path

        // Send the absolute path of the file to the input element
        fileInput.sendKeys(uploadFile.getAbsolutePath());

    }

    public static void downloadFile(WebDriver driver) throws InterruptedException {
        // Define the directory where you want the file to be downloaded
        String downloadDir = System.getProperty("user.dir") + File.separator + "downloads";
        File downloadFolder = new File(downloadDir);
        if (!downloadFolder.exists()) {
            downloadFolder.mkdir();
        }

        Map<String, Object> chromePrefs = new HashMap<>();
        // Set download directory
        chromePrefs.put("download.default_directory", downloadDir);
        // Disable prompt for download
        chromePrefs.put("download.prompt_for_download", false);
        // Disable PDF viewer for PDF files (important for PDF download testing)
        chromePrefs.put("plugins.always_open_pdf_externally", true);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("prefs", chromePrefs);

        // ... navigate to page and click the download link ...

        // ... After clicking the download link ...

        // Wait for the download to start and complete (use explicit/fluent waits in production code)
        Thread.sleep(5000); // Example, replace with proper wait

        // Verify the file exists in the configured download directory
        File downloadedFile = new File(downloadDir, "expected_file_name.extension"); // e.g., "report.pdf"

        if (downloadedFile.exists()) {
            System.out.println("File downloaded successfully!");
            // Perform file validation here (e.g., check file size, read content)
            downloadedFile.delete(); // Optional: clean up the file after verification
        } else {
            System.out.println("File download failed or took too long.");
        }
    }
}
