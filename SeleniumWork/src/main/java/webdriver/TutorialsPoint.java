package webdriver;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.Select;

import java.io.File;

public class TutorialsPoint {

    public static void main(String[] args) {

        // Create an instance of ChromeDriver (launch the Chrome browser)
        WebDriver driver = new ChromeDriver();

        try {
            // Navigate to the desired website (GeeksforGeeks in this example)
            driver.get("https://www.tutorialspoint.com/selenium/practice/selenium_automation_practice.php");

            // Get and print the page title
            String pageTitle = driver.getTitle();
            System.out.println("Page Title: " + pageTitle);

            WebElement nameField = driver.findElement(By.id("name"));
            WebElement emailField = driver.findElement(By.id("email"));
            WebElement mobileField = driver.findElement(By.id("mobile"));
            WebElement dobField = driver.findElement(By.id("dob"));
            WebElement subjectsField = driver.findElement(By.id("subjects"));
            WebElement addressField = driver.findElement(By.xpath("/html/body/main/div/div/div[2]/form/div[9]/div/textarea"));

            nameField.sendKeys("Sourabh Garg");
            emailField.sendKeys("sourabh.garg@gmail.com");

            // Locate the radio button by its ID (assuming it has an ID)
            WebElement genderRadioOpt = driver.findElement(By.xpath("//*[@id=\"practiceForm\"]/div[3]/div/div/div[2]/input"));
            genderRadioOpt.click();

            mobileField.sendKeys("6789000000");
            dobField.sendKeys("09/12/2000");
            subjectsField.sendKeys("English");

            WebElement checkbox1 = driver.findElement(By.xpath("//*[@id=\"hobbies\"]"));
            WebElement checkbox2 = driver.findElement(By.xpath("//*[@id=\"practiceForm\"]/div[7]/div/div/div[2]/input"));
            WebElement checkbox3 = driver.findElement(By.xpath("//*[@id=\"practiceForm\"]/div[7]/div/div/div[3]/input"));

            checkbox1.click();
            checkbox2.click();
            checkbox3.click();

            WebElement fileInput = driver.findElement(By.id("picture"));

            // Create a File object for the file to upload
            File uploadFile = new File("src/main/resources/input.jpeg"); // Replace with your file path

            // Send the absolute path of the file to the input element
            fileInput.sendKeys(uploadFile.getAbsolutePath());

            addressField.sendKeys("Surrey, BC, Canada");

            // 1. Locate the dropdown element
            WebElement stateDropDown = driver.findElement(By.id("state"));
            // 2. Create a Select object
            Select select = new Select(stateDropDown);
            // 3. Select an option (e.g., by visible text)
            select.selectByVisibleText("NCR");

            WebElement cityDropDown = driver.findElement(By.id("city"));
            select = new Select(cityDropDown);
            select.selectByVisibleText("Agra");

            // Wait for a few seconds (for demonstration purposes only)
            Thread.sleep(3000);

            WebElement submitButton = driver.findElement(By.xpath("//*[@id=\"practiceForm\"]/div[11]/input"));
            submitButton.click();

            // Wait for a few seconds (for demonstration purposes only)
            Thread.sleep(3000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the browser
            driver.quit();
        }
    }
}
