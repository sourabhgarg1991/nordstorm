package webdriver;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import java.util.List;

public class SpiceJet {

    public static void main(String[] args) {

        // Create an instance of ChromeDriver (launch the Chrome browser)
        WebDriver driver = new ChromeDriver();

        driver.get("https://demo.automationtesting.in/Datepicker.html");

        String monthNumber = "10";
        String date = "15";
        String year = "2025";
        String[] expectedDate = {monthNumber, date, year};
        driver.findElement(By.id("datepicker1")).click();
//        driver.findElement(By.xpath("//td[@data-month='"+monthNumber+"' and @data-year='"+year+"']/a[text()='"+date+"']")).click();
        driver.findElement(By.xpath("//*[@id=\"ui-datepicker-div\"]/table/tbody/tr[3]/td[@data-month='"+monthNumber+"' and @data-year='"+year+"']/a[text()='"+date+"']")).click();

        //        driver.findElement(By.cssSelector(".react-calendar__navigation__label")).click();
//        driver.findElement(By.cssSelector(".react-calendar__navigation__label")).click();
//        driver.findElement(By.xpath("//button[text()='"+year+"']")).click();
//        driver.findElements(By.cssSelector(".react-calendar__year-view__months__month")).get(Integer.parseInt(monthNumber)-1).click();
//        driver.findElement(By.xpath("//abbr[text()='"+date+"']")).click();

        List<WebElement> actualDate = driver.findElements(By.cssSelector(".react-date-picker__inputGroup__input"));

        for (int i=0;i<actualDate.size();i++){
            System.out.println(actualDate.get(i).getDomAttribute("value"));
            assert actualDate.get(i).getDomAttribute("value").equals(expectedDate[i]);
        }

        driver.close();
    }

}
