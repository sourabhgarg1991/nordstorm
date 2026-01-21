package webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;

public class TableColumnSum {

    public static void main(String[] args) {
        // Set up WebDriver (e.g., ChromeDriver)
//        System.setProperty("webdriver.chrome.driver", "path/to/chromedriver");
        WebDriver driver = new ChromeDriver();
        driver.get("your_html_page_with_table.html"); // Replace with your URL

        //Find table using id
        WebElement table = driver.findElement(By.id("dataTable"));

        //find all column cell within the table
        List<WebElement> columnCells = table.findElements(By.xpath(".//tr/td[4]")); // Summing 4th column

        int sum = 0;

        //iterate all cells
        for (WebElement cell : columnCells) {

            //get text of cells
            String cellText = cell.getText();
            try {
                //Convert string to integer
                int value = Integer.parseInt(cellText);
                //Add to sum
                sum += value;
            } catch (NumberFormatException e) {
                System.out.println("Skipping non-numeric value in column: " + cellText);
            }
        }

        System.out.println("Total sum of the 4th column: " + sum);

        driver.quit();
    }
}