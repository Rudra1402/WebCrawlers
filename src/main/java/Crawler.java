import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Crawler {

    public static class HotelData{
        public String name, distance, rating, price, alternateDeal;

        public HotelData(String name, String distance, String rating, String price, String alternateDeal) {
            this.name = name;
            this.distance = distance;
            this.rating = rating;
            this.price = price;
            this.alternateDeal = alternateDeal;
        }

        public String getName() {
            return name;
        }

        public String getDistance() {
            return distance;
        }

        public String getRating() {
            return rating;
        }

        public String getPrice() {
            return price;
        }

        public String getAlternateDeal() { return alternateDeal; }
    }

    public static int siteNo = 1;

    public static void crawl(String url,String filePath) {
        FirefoxOptions opts = new FirefoxOptions();
        opts.addArguments("--headless");
        WebDriver pageDriver = new FirefoxDriver(opts);
        pageDriver.get(url);

        Map<String, List<HotelData>> hotelData = new HashMap<>();
        List<HotelData> cityHotels = new ArrayList<>();

        WebDriverWait wait = new WebDriverWait(pageDriver, Duration.ofSeconds(45));
        WebElement cityName = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='search-form-destination']")));
        String city = cityName.getAttribute("value");
        System.out.println(city);
        List<WebElement> elements = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("[data-testid=accommodation-list-element]")));

        for (WebElement element : elements) {
            String hotelName="", hotelDist="",hotelRating="", hotelPrice="", hotelAlternateDeal="";
            try {
                WebElement hotelNameElement = element.findElement(By.cssSelector("[data-testid=item-name]"));
                hotelName = hotelNameElement.getText();
            } catch (Exception e) {
                hotelName = "Not available";
            }

            try {
                WebElement hotelDistElement = element.findElement(By.cssSelector("[data-testid=distance-label-section]"));
                hotelDist = hotelDistElement.getText();
            } catch (Exception e) {
                hotelDist = "Not available";
            }

            try {
                WebElement hotelRatingElement = element.findElement(By.cssSelector("[data-testid=aggregate-rating]"));
                hotelRating = hotelRatingElement.getText();
            } catch (Exception e) {
                hotelRating = "Not available";
            }

            try {
                WebElement hotelPriceElement = element.findElement(By.cssSelector("[data-testid=recommended-price]"));
                hotelPrice = hotelPriceElement.getText();
            } catch (Exception e) {
                hotelPrice = "Not available";
            }

            try {
                WebElement hotelAlternateDealElement = element.findElement(By.cssSelector("[data-testid=price-label]"));
                String[] altDeal = hotelAlternateDealElement.getText().split("\n");
                if (altDeal.length > 1) {
                    hotelAlternateDeal = altDeal[0] + " (" + altDeal[1] + ")";
                } else {
                    hotelAlternateDeal = altDeal[0];
                }
            } catch (Exception e) {
                hotelAlternateDeal = "Not available";
            }

            HotelData hotelDataObj = new HotelData(hotelName, hotelDist, hotelRating, hotelPrice, hotelAlternateDeal);
            cityHotels.add(hotelDataObj);
            System.out.println(siteNo+". ----------");
//            System.out.println(siteNo+" Hotel Name: "+hotelName + "\n" + "Location: " + hotelDist + "\n" + "Ratings: " + hotelRating + "\n" + "Price: " + hotelPrice + "\n" + "Alternate Deal: " + hotelAlternateDeal);
            siteNo++;
        }
        hotelData.put(city, cityHotels);
//        String json = convertToJson(hotelData);
        appendJsonToFile(filePath, hotelData, city);
        pageDriver.quit();
    }

    private static void appendJsonToFile(String filePath, Map<String, List<HotelData>> hotelData, String city) {
        try {
//            fileWriter.write(json + System.lineSeparator());
            String existingContent = Files.exists(Paths.get(filePath)) ? new String(Files.readAllBytes(Paths.get(filePath))) : "[]";

//            Gson gson = new Gson();
//            List<Map<String, List<HotelData>>> existingArray = gson.fromJson(existingContent, List.class);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            List<Map<String, List<HotelData>>> existingArray = gson.fromJson(existingContent, new TypeToken<List<Map<String, List<HotelData>>>>(){}.getType());

            if (existingArray == null) {
                existingArray = new ArrayList<>();
            }

            existingArray.add(Map.of(city, hotelData.get(city)));
            Files.write(Paths.get(filePath), gson.toJson(existingArray).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String convertToJson(Map<String, List<HotelData>> hotelData) {
        Gson gson = new Gson();
        return gson.toJson(hotelData);
    }

    public static void main(String[] args) throws IOException {

        FirefoxOptions opts = new FirefoxOptions();
        opts.addArguments("--headless");

        WebDriver driver = new FirefoxDriver(opts);
        driver.get("https://www.trivago.ca");
        System.out.println("Root URL connected!");

        List<WebElement> links = driver.findElements(By.tagName("a"));
        if (!links.isEmpty()) {
            System.out.println(links.size()+" links found!");
        }

        String jsonFilePath = "hotels.json";
        File file = new File(jsonFilePath);

        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    System.out.println("File created: " + jsonFilePath);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        List<String> hrefLinks = new ArrayList<>();
        for (WebElement link : links) {
            String href = link.getAttribute("href");
            if (href != null && href.startsWith("https://www.trivago.ca/en-CA/odr")) {
                hrefLinks.add(href);
            }
        }

        for (String link : hrefLinks) {
            crawl(link, jsonFilePath);
        }

        driver.quit();

    }
}
