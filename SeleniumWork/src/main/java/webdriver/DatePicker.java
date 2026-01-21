package webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;

public class DatePicker {

    public static void main(String[] args) {

        // Create an instance of ChromeDriver (launch the Chrome browser)
        WebDriver driver = new ChromeDriver();

        driver.get("https://rahulshettyacademy.com/seleniumPractise/#/offers");

        //Use Inputs for month, date and year
        String monthNumber = "6";
        String date = "15";
        String year = "2027";
        String[] expectedDate = {monthNumber, date, year};

        //find the calendar button, click on that
        driver.findElement(By.cssSelector(".react-date-picker__calendar-button")).click();

        //find the Month button, click on that
        driver.findElement(By.cssSelector(".react-calendar__navigation__label")).click();

        //find the Year button, click on that
        driver.findElement(By.cssSelector(".react-calendar__navigation__label")).click();

        //find the year as per input and click
        driver.findElement(By.xpath("//button[text()='"+year+"']")).click();
        //find the month as per input and click
        driver.findElements(By.cssSelector(".react-calendar__year-view__months__month")).get(Integer.parseInt(monthNumber)-1).click();
        //find the date as per input and click
        driver.findElement(By.xpath("//abbr[text()='"+date+"']")).click();

        //Get the selected date to assert
        List<WebElement> actualDate = driver.findElements(By.cssSelector(".react-date-picker__inputGroup__input"));

        for (int i=0;i<actualDate.size();i++){
            System.out.println(actualDate.get(i).getDomAttribute("value"));
            assert actualDate.get(i).getDomAttribute("value").equals(expectedDate[i]);
        }

        driver.close();
    }

}
