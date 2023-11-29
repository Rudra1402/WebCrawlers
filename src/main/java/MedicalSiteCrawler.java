import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MedicalSiteCrawler {

    // Class to store the diseases information
    public static class DiseaseInfo {
        public String diseaseName;
        public List<String> symptoms;
        public List<String> diagnosis;
        public List<String> treatments;

        public DiseaseInfo(String diseaseName) {
            this.diseaseName = diseaseName;
            this.symptoms = new ArrayList<>();
            this.diagnosis = new ArrayList<>();
            this.treatments = new ArrayList<>();
        }

        public String getDiseaseName() {
            return diseaseName;
        }

        public List<String> getSymptoms() {
            return symptoms;
        }

        public List<String> getDiagnosis() {
            return diagnosis;
        }

        public List<String> getTreatments() {
            return treatments;
        }
    }

    public static int totalSites = 1;

    public static void crawl(String url) {

        List<DiseaseInfo> diseaseInfoList = new ArrayList<>();

        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--headless");
        WebDriver driver = new ChromeDriver(opts);
        driver.get(url);

        WebElement listContainer = driver.findElement(By.cssSelector("[id='site-hits']"));

        List<WebElement> links = listContainer.findElements(By.tagName("a"));
        int ct = 0;
        boolean flag = false;
        for (WebElement link: links) {
            String href = link.getAttribute("href");
            if (href!=null && !href.isEmpty()) { // Checking if href is not null and is not empty.
                // Using this counter variable to make sure that maximum 5 sites are crawled for a specific alphabet.
                // When the value of this variable will be 5, the flag will set to "true" and the loop will break.
                ct++;
                ChromeDriver innerDriver = new ChromeDriver(opts);
                innerDriver.get(href);

                System.out.println("Crawling Site No: "+totalSites); // Printing the current site number.
                totalSites++;

                WebDriverWait wait = new WebDriverWait(innerDriver, Duration.ofSeconds(45));

                // Waiting for the required element to get loaded and then search by cssSelector.
                WebElement rootContentElement = wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[data-identity='main-article-content']")));

                // Title of the disease or condition that is being crawled.
                WebElement h1Tag = innerDriver.findElement(By.tagName("h1"));
                String diseaseName = h1Tag.getText();

                DiseaseInfo diseaseInfo = new DiseaseInfo(diseaseName); // Creating an object of the class DiseaseInfo

                // Fetching Symptoms and Causes of the disease
                try {
                    WebElement symptomsElement = rootContentElement.findElement(By.cssSelector("[id='symptoms-and-causes']")).findElement(By.tagName("ul"));
                    String text = symptomsElement.getText();
                    String[] sList = text.split("\n");
                    diseaseInfo.symptoms.addAll(Arrays.asList(sList));
                } catch (Exception e) {
                    // If a <ul> list is not found then get the first <p> tag to get basic information
                    try {
                        WebElement symptomsElement = rootContentElement.findElement(By.cssSelector("[id='symptoms-and-causes']")).findElement(By.cssSelector("[data-identity='rich-text']"));
                        WebElement pTag = symptomsElement.findElement(By.tagName("p"));
                        diseaseInfo.symptoms.add(pTag.getText());
                    } catch (Exception tempE) {
                        diseaseInfo.symptoms.add("None");
                    }
                }

                // Fetching Diagnosis and Tests done to detect the disease
                try {
                    WebElement diagnosisElement = rootContentElement.findElement(By.cssSelector("[id='diagnosis-and-tests']")).findElement(By.tagName("ul"));
                    String text = diagnosisElement.getText();
                    String[] dList = text.split("\n");
                    diseaseInfo.diagnosis.addAll(Arrays.asList(dList));
                } catch (Exception e) {
                    // If a <ul> list is not found then get the first <p> tag to get basic information
                    try {
                        WebElement diagnosisElement = rootContentElement.findElement(By.cssSelector("[id='diagnosis-and-tests']")).findElement(By.cssSelector("[data-identity='rich-text']"));
                        WebElement pTag = diagnosisElement.findElement(By.tagName("p"));
                        diseaseInfo.diagnosis.add(pTag.getText());
                    } catch (Exception tempE) {
                        diseaseInfo.diagnosis.add("None");
                    }
                }

                // Fetching Management techniques and Treatments given to cure the disease.
                try {
                    WebElement treatmentElement = rootContentElement.findElement(By.cssSelector("[id='management-and-treatment']")).findElement(By.tagName("ul"));
                    String text = treatmentElement.getText();
                    String[] tList = text.split("\n");
                    diseaseInfo.treatments.addAll(Arrays.asList(tList));
                } catch (Exception e) {
                    // If a <ul> list is not found then get the first <p> tag to get basic information
                    try {
                        WebElement treatmentElement = rootContentElement.findElement(By.cssSelector("[id='management-and-treatment']")).findElement(By.cssSelector("[data-identity='rich-text']"));
                        WebElement pTag = treatmentElement.findElement(By.tagName("p"));
                        diseaseInfo.treatments.add(pTag.getText());
                    } catch (Exception tempE) {
                        diseaseInfo.treatments.add("None");
                    }
                }
                int index = 1;
                diseaseInfoList.add(diseaseInfo);
                System.out.println("Disease Name: " + diseaseInfo.getDiseaseName());
                System.out.println("Symptoms:");
                for (String s: diseaseInfo.getSymptoms()) {
                    System.out.println(index + ". " + s);
                    index++;
                }
                index = 1;
                System.out.println("Diagnosis:");
                for (String d: diseaseInfo.getDiagnosis()) {
                    System.out.println(index + ". " + d);
                    index++;
                }
                index = 1;
                System.out.println("Treatments:");
                for (String t: diseaseInfo.getTreatments()) {
                    System.out.println(index + ". " + t);
                    index++;
                }
                System.out.println("----------");
                innerDriver.quit();

                // If counter variable value is 5 then flag will be set as "true" and the loop will break.
                if (ct == 5) {
                    flag = true;
                    break;
                }
            }
        }
        if (flag) { // When flag is true, the data will get appended in the .csv file with the help of writeToCSV() function.
            writeToCSV(diseaseInfoList);
        }

        driver.quit();
    }

    public static boolean isHeadingAppended = false;

    private static void writeToCSV(List<DiseaseInfo> diseaseInfoList) {
        // Second parameter in the FileWriter class is set as "true" which ensures that the data will append to the .csv file.
        // If the second parameter is not provided or given as "false", the data will get overwritten.
        // Hence, setting the second parameter as "true" based on the requirements.
        try (FileWriter csvWriter = new FileWriter("MedicalInfo.csv", true)) {
            // Write CSV header
            if (!isHeadingAppended) { // If headings are not present, then add the headings
                csvWriter.append("Disease Name,Symptoms,Diagnosis,Treatments\n");
                isHeadingAppended = true;
            }

            // Append data to the .csv file for each disease
            for (DiseaseInfo diseaseInfo : diseaseInfoList) {
                csvWriter.append(String.format("\"%s\",\"%s\",\"%s\",\"%s\"\n",
                        diseaseInfo.diseaseName,
                        String.join("; ", diseaseInfo.symptoms),
                        String.join("; ", diseaseInfo.diagnosis),
                        String.join("; ", diseaseInfo.treatments)));
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Diseases based on character matching in the query parameter
        String diseaseByChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ", url;
        Character queryChar; // This character will be stored as the query parameter in the URL.
        for (int i = 0; i < diseaseByChars.length(); i++) { // for each character the loop will iterate and call crawl() function.
            queryChar = diseaseByChars.charAt(i);
            url="https://my.clevelandclinic.org/health/diseases?q="+queryChar+"+&dFR[type][0]=Diseases";
            crawl(url); // Calling the crawl() function with url as an argument.
        }
    }
}
