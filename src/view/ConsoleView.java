package view;

import controller.UrlShortenerController;

import java.util.Map;
import java.util.Scanner;

public class ConsoleView {
    private final UrlShortenerController controller;
    private final Scanner scanner;

    public ConsoleView(UrlShortenerController controller) {
        this.controller = controller;
        this.scanner = new Scanner(System.in);
    }

    public void start() {

        while (true) {
            System.out.println("\n Choose what you want to perform:");
            System.out.println("1. Shorten your URL");
            System.out.println("2. Expanding already existing URL");
            System.out.println("3. Shut down the service");
            System.out.println("0. Get all URls");

            String userChoice = scanner.nextLine();

            switch (userChoice) {
                case "1":
                    System.out.print("Enter the original URL: ");
                    String originalUrl = scanner.nextLine();
                    String shortUrl = controller.createShortUrl(originalUrl);
                    System.out.println("Short URL: " + shortUrl);
                    break;
                case "2":
                    System.out.print("Enter the existing short URL: ");
                    String inputShortUrl = scanner.nextLine();
                    String originalExpandedUrl = controller.getOriginalUrl(inputShortUrl);
                    if (originalExpandedUrl != null) {
                        System.out.println("Original URL: " + originalExpandedUrl);
                    } else {
                        System.out.println("Short URL not found.");
                    }
                    break;
                case "3":
                    System.out.println("The service haы been shut down!");
                    return;
                case "0":
                    System.out.println("Here is a list of all available URLs in DB");
                    Map<String, String> allUrls = controller.getAllData();
                    for (Map.Entry<String, String> entry : allUrls.entrySet()) {
                        System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
                    }
                default:
                    System.out.println("Wrong input");
            }
        }
    }
}
