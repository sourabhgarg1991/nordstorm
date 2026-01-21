package webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EcommerceCart {

    public static void main(String[] args) {

        // Create an instance of ChromeDriver (launch the Chrome browser)
        WebDriver driver = new ChromeDriver();

        String[] itemList = {"Blue Top", "Men Tshirt"};
        try {
            // Navigate to the desired website (GeeksforGeeks in this example)
            driver.get("https://automationexercise.com/");

            // Get and print the page title
            String pageTitle = driver.getTitle();
            System.out.println("Page Title: " + pageTitle);
            addItems(driver, itemList);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close the browser
            driver.quit();
        }
    }

    public static void addItems(WebDriver driver, String[] itemsNeeded) throws InterruptedException {
        int count = 0;
        Actions actions = new Actions(driver);

        //Create list of items needed to add
        List<String> items = Arrays.asList(itemsNeeded);

        //find all the products web element using cssSelector
        List<WebElement> products = driver.findElements(By.cssSelector("p"));

        //Iterate all the products web element
        for (int i = 0; i < products.size(); i++) {

            for (String item : items) {
                //check if product.text exist in input list
                if (products.get(i).getText().contains(item)) {
                    count++;

                    //if product.text exist in input list, add to cart by clicking add to cart button using xpath
                    WebElement addToCartLink = driver.findElements(By.xpath("//a[@class='btn btn-default add-to-cart']")).get(i);
                    actions.moveToElement(addToCartLink).perform();
                    WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
                    WebElement addToCart = wait.until(ExpectedConditions.elementToBeClickable(addToCartLink));
                    addToCart.click();

                    //Remove item from input list to avoid adding duplicate items in cart
                    items.remove(item);
                    driver.findElement(By.xpath("//*[@id=\"cartModal\"]/div/div/div[3]/button")).click();
                    break;
                }
            }
        }
    }
}
