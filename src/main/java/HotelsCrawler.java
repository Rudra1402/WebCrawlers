import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HotelsCrawler {

    public static class Parameters {
        String enteredCity, enteredState, enteredCountry, startDate, endDate;
        int noOfRooms, totalNoOfAdults;
        int[] adultsArr;

        public Parameters(String enteredCity, String enteredState, String enteredCountry, String startDate, String endDate, int noOfRooms, int[] adultsArr, int totalAdults) {
            this.enteredCity = enteredCity;
            this.enteredState = enteredState;
            this.enteredCountry = enteredCountry;
            this.startDate = startDate;
            this.endDate = endDate;
            this.noOfRooms = noOfRooms;
            this.adultsArr = adultsArr;
            this.totalNoOfAdults = totalAdults;
        }
    }

    public static class Room {
        String roomDesc, pricePerRoomPerNight, priceWithTax;
        private List<String> roomFacilities;

        public Room(String roomDesc, List<String> roomFacilities, String pricePerRoomPerNight, String priceWithTax) {
            this.roomDesc = roomDesc;
            this.roomFacilities = roomFacilities;
            this.pricePerRoomPerNight = pricePerRoomPerNight;
            this.priceWithTax = priceWithTax;
        }
    }

    public static class HotelData {
        String name, stars5Rating, location, reviewScore, reviewMessage, numberOfReviews;
        List<Room> rooms;
        List<String> amenities;

        public HotelData(String name, String stars5Rating, String location, String reviewScore, String reviewMessage, String numberOfReviews, List<Room> rooms, List<String> amenities) {
            this.name = name;
            this.stars5Rating = stars5Rating;
            this.location = location;
            this.reviewScore = reviewScore;
            this.reviewMessage = reviewMessage;
            this.numberOfReviews = numberOfReviews;
            this.rooms = rooms;
            this.amenities = amenities;
        }
    }

    public static void crawlSubUrl(String url, List<HotelData> hotels, String city, String filePath, String site) {
        FirefoxOptions opts = new FirefoxOptions();
        opts.addArguments("--headless");
        WebDriver driver = new FirefoxDriver(opts);
        driver.get(url);
        System.out.println("Connected URL: "+url);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45));

        String name = "", stars5Rating = "", location = "";
        String reviewScore = "", reviewMessage = "", numberOfReviews = "";
        List<String> amenities = new ArrayList<>();
        String h3Tag = "", price = "", totalPrice = "";
        List<String> singleRoomAmenities = new ArrayList<>();
        List<Room> rooms = new ArrayList<>();

        // Title Element
        System.out.println("Crawling Title Element...");
        try {
            WebElement titleElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-stid='content-hotel-title']")));
            try {
                WebElement nameElem = titleElement.findElement(By.cssSelector("h1"));
                name = nameElem.getText();
            } catch (Exception e) {
                name = "Not found!";
            }
            try {
                List<WebElement> stars5RatingElem;
                stars5RatingElem = titleElement.findElements(By.tagName("span"));
                stars5Rating = stars5RatingElem.get(0).getText();
                if (stars5Rating.equals("VIP Access")) {
                    stars5Rating = stars5RatingElem.get(2).getText();
                }
            } catch (Exception e) {
                stars5Rating = "Not found!";
            }
            try {
                WebElement locationElem = titleElement.findElements(By.tagName("div")).get(0).findElements(By.tagName("div")).get(2);
                location = locationElem.getText();
            } catch (Exception e) {
                location = "Not found!";
            }
        } catch(Exception e) {
            System.out.println("Main Title Element not found!");
        }

        // Review Element
        System.out.println("Crawling Review Element...");
        try {
            WebElement reviewElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-stid='content-hotel-reviewsummary']")));
            try {
                WebElement reviewScoreElem = reviewElement.findElements(By.tagName("span")).get(1);
                reviewScore = reviewScoreElem.getText();
            } catch (Exception e) {
                reviewScore = "Not found!";
            }
            try {
                WebElement reviewMessageElem = reviewElement.findElements(By.tagName("h3")).get(0);
                reviewMessage = reviewMessageElem.getText();
            } catch (Exception e) {
                reviewMessage = "Not found!";
            }
            try {
                WebElement numberOfReviewsElem = reviewElement.findElements(By.cssSelector("[data-stid='reviews-link']")).get(0);
                numberOfReviews = numberOfReviewsElem.getText();
                String[] reviews = numberOfReviews.split(" ");
                numberOfReviews = reviews[2] + " " + reviews[3];
            } catch (Exception e) {
                numberOfReviews = "Not found!";
            }
        } catch (Exception e) {
            System.out.println("Main Review Element not found!");
        }

        // Amenities Element
        System.out.println("Crawling Amenities Element...");
        try {
            WebElement amenitiesElement;
            amenitiesElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-stid='hotel-amenities-list']")));
            List<WebElement> aList = amenitiesElement.findElements(By.tagName("li"));
            for (WebElement li : aList) {
                WebElement liSpanTag = li.findElement(By.tagName("span"));
                String liSpan = liSpanTag.getText();
                amenities.add(liSpan);
            }
        } catch (Exception e) {
            System.out.println("Main Amenities Element not found!");
        }

        // Rooms List Element
        System.out.println("Crawling Rooms List Element...");
        try {
            WebElement roomsListContainer;
            roomsListContainer = new WebDriverWait(driver, Duration.ofSeconds(45)).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-stid='section-room-list']"))).findElement(By.tagName("div"));
            List<WebElement> roomsList = roomsListContainer.findElements(By.cssSelector("[data-stid^='property-offer-']"));
            for (WebElement room : roomsList) {
                singleRoomAmenities = new ArrayList<>();
                try {
                    List<WebElement> roomDescH3Tag = room.findElements(By.tagName("h3"));
                    WebElement h3TagElem = roomDescH3Tag.get(0);
                    h3Tag = h3TagElem.getText();
                    if (h3Tag.equals("Popular among travellers")) {
                        h3TagElem = roomDescH3Tag.get(1);
                        h3Tag = h3TagElem.getText();
                    }
                } catch (Exception e) {
                    h3Tag = "Not found!";
                }
                try {
                    List<WebElement> liTags = room.findElements(By.tagName("li"));
                    for (WebElement li : liTags) {
                        singleRoomAmenities.add(li.getText());
                    }
                } catch (Exception e) {
                    System.out.println("Individual Room Amenities not found!");
                }
                try {
                    List<WebElement> prices = room.findElements(By.cssSelector("[data-test-id='price-summary-message-line']"));
                    try {
                        WebElement priceElem = prices.get(0).findElement(By.tagName("span"));
                        price = priceElem.getText();
                    } catch (Exception e) {
                        price = "Not found!";
                    }
                    try {
                        WebElement totalPriceElem = prices.get(1);
                        totalPrice = totalPriceElem.getText();
                    } catch (Exception e) {
                        totalPrice = "Not found!";
                    }
                } catch (Exception e) {
                    price = "Not found!";
                    totalPrice = "Not found!";
                }
                rooms.add(new Room(h3Tag, singleRoomAmenities, price, totalPrice));
            }
        } catch (Exception e) {
            System.out.println("Main Rooms List Element not found!");
        }
        HotelData obj = new HotelData(name, stars5Rating, city, reviewScore, reviewMessage, numberOfReviews, rooms, amenities);
        hotels.add(obj);
        appendJsonToFile(filePath, obj, city);
        System.out.println("-----Data added to json-----");

        driver.quit();
    }

    public static void crawlBookingSubUrl(String url, List<HotelData> hotels, String city, String filePath, String site) {
        FirefoxOptions opts = new FirefoxOptions();
        opts.addArguments("--headless");
        WebDriver driver = new FirefoxDriver(opts);
        driver.get(url);
        System.out.println("Connected URL: "+url);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45));

        String name = "", stars5Rating = "", location = "";
        String reviewScore = "", reviewMessage = "", numberOfReviews = "";
        List<String> amenities = new ArrayList<>();
        String h3Tag = "", price = "", totalPrice = "";
        List<String> singleRoomAmenities = new ArrayList<>();
        List<Room> rooms = new ArrayList<>();

        // Title Element
        System.out.println("Crawling Title Element...");
        try {
            WebElement titleElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("hp_hotel_name")));
            try {
                WebElement nameElem = titleElement.findElement(By.cssSelector("h2"));
                name = nameElem.getText();
            } catch (Exception e) {
                name = "Not found!";
            }
            try {
                WebElement ratingElem = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='quality-rating']")));
                List<WebElement> stars5RatingElem = ratingElem.findElements(By.tagName("svg"));
                stars5Rating = Integer.toString(stars5RatingElem.size());
            } catch (Exception e) {
                stars5Rating = "Not found!";
            }
            try {
                WebElement locationElem;
                locationElem = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("showMap2"))).findElements(By.tagName("span")).get(1);
                location = locationElem.getText();
            } catch (Exception e) {
                location = "";
            }
        } catch(Exception e) {
            System.out.println("Main Title Element not found!");
        }

        // Review Element
        System.out.println("Crawling Review Element...");
        try {
            WebElement reviewElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='review-score-right-component']")));
            List<WebElement> divTags = reviewElement.findElements(By.tagName("div"));
            try {
                reviewScore = divTags.get(0).getText();
            } catch(Exception e) {
                reviewScore = "Not found!";
            }
            try {
                reviewMessage = divTags.get(2).getText();
            } catch(Exception e) {
                reviewMessage = "Not found!";
            }
            try {
                numberOfReviews = divTags.get(3).getText();
            } catch(Exception e) {
                numberOfReviews = "Not found!";
            }
        } catch (Exception e) {
            System.out.println("Main Review Element not found!");
        }

        // Amenities Element
        System.out.println("Crawling Amenities Element...");
        try {
            WebElement amenitiesElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-testid='property-most-popular-facilities-wrapper']")));
            List<WebElement> liList = amenitiesElement.findElements(By.tagName("li"));
            for (WebElement li: liList) {
                String liText = li.getText();
                amenities.add(liText);
            }
        } catch (Exception e) {
            System.out.println("Main Amenities Element not found!");
        }

        // Rooms List Element
        System.out.println("Crawling Rooms List Element...");
        try {
//            WebElement roomsListContainer = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("rooms_table")));
            WebElement table = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("hprt-table")));
            WebElement tbody = table.findElement(By.tagName("tbody"));
            List<WebElement> trTags = tbody.findElements(By.tagName("tr"));
            for (WebElement tr: trTags) {
                singleRoomAmenities = new ArrayList<>();
                List<WebElement> facilities = tr.findElements(By.cssSelector("[data-facility-id^='']"));
                if (facilities.isEmpty()) {
                    facilities = tr.findElements(By.className("hprt-facilities-facility"));
                }
                if (!facilities.isEmpty()) {
                    for (WebElement facility: facilities) {
                        String facilityText = facility.getText();
                        if (facilityText.length() != 0) {
                            singleRoomAmenities.add(facilityText);
                        }
                    }
                    try {
                        WebElement roomType = tr.findElement(By.className("hprt-roomtype-icon-link"));
                        h3Tag = roomType.getText();
                    } catch (Exception e) {
                        h3Tag = "Not found!";
                    }
                    try {
                        WebElement tdTag = tr.findElements(By.tagName("td")).get(2);
                        WebElement priceElement = tdTag;
                        WebElement dealsContainer = priceElement.findElement(By.cssSelector("[data-component='deals-container']"));
                        String[] pricesArr = priceElement.getText().split("\n");
                        try {
                            Pattern pattern = Pattern.compile("\\d+");
                            String mainPrice = dealsContainer == null ? pricesArr[0] : pricesArr[1];
                            Matcher matcher = pattern.matcher(mainPrice);
                            while (matcher.find()) {
                                price = matcher.group();
                            }
                            try {
                                String taxStr = dealsContainer == null ? pricesArr[2] : pricesArr[3];
                                matcher = pattern.matcher(taxStr);
                                while (matcher.find()) {
                                    totalPrice = matcher.group();
                                }
                                int temp = Integer.parseInt(price) + Integer.parseInt(totalPrice);
                                totalPrice = "CAD "+Integer.toString(temp);
                            } catch (Exception e) {
                                totalPrice = "Not found!";
                            }
                        } catch (Exception e) {
                            price = "Not found!";
                        }
                    } catch (Exception e) {
                        price = "Not found!";
                        totalPrice = "Not found!";
                    }
                }
            }
            rooms.add(new Room(h3Tag, singleRoomAmenities, price, totalPrice));
        } catch (Exception e) {
            System.out.println("Rooms table not found!");
        }
        HotelData obj = new HotelData(name, stars5Rating, city, reviewScore, reviewMessage, numberOfReviews, rooms, amenities);
        hotels.add(obj);
        appendJsonToFile(filePath, obj, city);
        System.out.println("-----Data added to json-----");

        driver.quit();
    }

    public static void crawl(String url, String cityInJSON, String filePath, String site) {
        FirefoxOptions opts = new FirefoxOptions();
        opts.addArguments("--headless");
        WebDriver driver = new FirefoxDriver(opts);
        driver.get(url);
        System.out.println("Connected URL: "+url);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(45));

        if(site.equals("hotels")) {
            wait.until(tempDriver -> {
                String currentUrl = tempDriver.getCurrentUrl();
                return currentUrl != null && !currentUrl.equals(url);
            });

            String currURL = driver.getCurrentUrl();
            driver.get(currURL);
            System.out.println("Connected URL: " + currURL);
        }


        wait = new WebDriverWait(driver, Duration.ofSeconds(45));

        if (site.equals("hotels")) {
            try {
                WebElement searchResultSection = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-stid='section-results']")));
                WebElement listingSection = searchResultSection.findElement(By.cssSelector("[data-stid='property-listing-results']"));
                List<WebElement> hotelList = listingSection.findElements(By.cssSelector("[data-stid='lodging-card-responsive']"));

                Map<String, List<HotelData>> hotelData = new HashMap<>();
                List<HotelData> cityHotels = new ArrayList<>();

                for (WebElement hotel : hotelList) {
                    WebElement h3Tag = hotel.findElement(By.tagName("h3"));
                    String hotelName = h3Tag.getText();
                    if (hotelName.contains("Photo gallery")) {
                        continue;
                    }
                    WebElement aTag = hotel.findElement(By.cssSelector("[data-stid='open-hotel-information']"));
                    if (aTag != null) {
                        String link = aTag.getAttribute("href");
                        if (!link.isEmpty()) {
                            crawlSubUrl(link, cityHotels, cityInJSON, filePath, site);
                        }
                    }
                }
                hotelData.put(cityInJSON, cityHotels);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(site.equals("booking")){
            try {
                List<WebElement> cards = driver.findElements(By.cssSelector("[data-testid='property-card-container']"));

                Map<String, List<HotelData>> hotelData = new HashMap<>();
                List<HotelData> cityHotels = new ArrayList<>();

                for (WebElement card: cards) {
                    WebElement titleLink = card.findElement(By.cssSelector("[data-testid='title-link']"));
                    if (titleLink != null) {
                        String link = titleLink.getAttribute("href");
                        if (!link.isEmpty()) {
                            crawlBookingSubUrl(link, cityHotels, cityInJSON, filePath, site);
                        }
                    }
                }
                hotelData.put(cityInJSON, cityHotels);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        driver.quit();
    }

    private static void appendJsonToFile(String filePath, HotelData hotelData, String city) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Map<String, List<HotelData>> existingMap;

            String existingContent = Files.exists(Paths.get(filePath)) ? new String(Files.readAllBytes(Paths.get(filePath))) : "{}";
            existingMap = gson.fromJson(existingContent, new TypeToken<Map<String, List<HotelData>>>(){}.getType());

            if (existingMap == null) {
                existingMap = new HashMap<>();
            }

            List<HotelData> cityHotels = existingMap.getOrDefault(city, new ArrayList<>());
            cityHotels.add(hotelData);
            existingMap.put(city, cityHotels);

            Files.write(Paths.get(filePath), gson.toJson(existingMap).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeParametersToJson(Parameters parameters, String filePath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(filePath);

        try (FileWriter writer = new FileWriter(file)) {
            if (!file.exists()) {
                file.createNewFile();
            }
            gson.toJson(parameters, writer);
            System.out.println("JSON written to: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        LocalDate currentDate = LocalDate.now();

        System.out.print("Enter city: ");
        String cityName = s.nextLine();
        System.out.print("Enter province/state: ");
        String provinceState = s.nextLine();
        System.out.print("Enter country: ");
        String country = s.nextLine();
        System.out.print("Enter start date (YYYY-MM-DD): ");
        String startDate = s.next();
        LocalDate startEnteredDate = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE);
        while (startEnteredDate.isBefore(currentDate)) {
            System.out.print("Entered Date is in the past. Enter again : ");
            startDate = s.next();
            startEnteredDate = LocalDate.parse(startDate, DateTimeFormatter.ISO_DATE);
        }

        System.out.print("Enter end date (YYYY-MM-DD): ");
        String endDate = s.next();
        LocalDate endEnteredDate = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE);
        while (endEnteredDate.isBefore(startEnteredDate)) {
            System.out.print("Entered Date is before start date. Enter again : ");
            endDate = s.next();
            endEnteredDate = LocalDate.parse(endDate, DateTimeFormatter.ISO_DATE);
        }

        System.out.print("Enter number of rooms: ");
        int numberOfRooms = s.nextInt();
        while (numberOfRooms < 1) {
            System.out.print("Number of rooms can not be less than 1. Enter again: ");
            numberOfRooms = s.nextInt();
        }
        int[] adults = new int[numberOfRooms];
        int totalAdults = 0;
        String roomAdultsQuery = "adults=";
        for (int i = 0; i < numberOfRooms; i++) {
            System.out.print("Adults for room number "+(i+1)+": ");
            int noOfAdults = s.nextInt();
            while (noOfAdults < 1) {
                System.out.print("Number of adults can not be less than 1. Enter again (Room "+(i+1)+"): ");
                noOfAdults = s.nextInt();
            }
            adults[i] = noOfAdults;
            totalAdults += noOfAdults;
            if (i == 0) {
                roomAdultsQuery += noOfAdults;
            } else {
                roomAdultsQuery += "%2C"+noOfAdults;
            }
        }

        Parameters p = new Parameters(cityName, provinceState, country, startDate, endDate, numberOfRooms, adults, totalAdults);
        String parameterJsonFile = "parameters.json";
        writeParametersToJson(p, parameterJsonFile);

        String cityInJSON = cityName + ", " + provinceState + ", " + country;

        String filePath = "Site_Hotels_Data.json";
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    System.out.println("File created: " + filePath);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String baseURL = "https://ca.hotels.com/Hotel-Search?"+roomAdultsQuery+"&destination="+cityName+"%2C"+provinceState+"%2C"+country+"&endDate="+endDate+"&sort=RECOMMENDED&startDate="+startDate;
        crawl(baseURL, cityInJSON, filePath, "hotels");

        filePath = "Site_Booking_Data.json";
        file = new File(filePath);
        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    System.out.println("File created: " + filePath);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        baseURL = "https://www.booking.com/searchresults.html?ss="+cityName+"%2C"+provinceState+"%2C"+country+"&checkin="+startDate+"&checkout="+endDate+"&group_adults="+totalAdults+"&no_rooms="+numberOfRooms+"&group_children=0";
        crawl(baseURL, cityInJSON, filePath, "booking");
    }
}
