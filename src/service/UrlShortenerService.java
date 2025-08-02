package service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

//Functional class which provides methods to shorten the original URL
public class UrlShortenerService {

    //HashMap to store URL in-memory and a random generator
    private final Map<String, String> urlMap = new HashMap<>();
    private final Random random = new Random();

    // A helper method to extract the base URL from a given URL
    private String getBaseUrl(String fullUrl) {
        try {
            URL url = new URL(fullUrl);
            return url.getProtocol() + "://" + url.getHost() + "/";
        } catch (MalformedURLException e) {
            return "";
        }
    }

    public String shortenUrl(String originalUrl) {
        String code = generateCode();
        String baseUrl = getBaseUrl(originalUrl);

        if (baseUrl.isEmpty()) {
            return "Error: Invalid input";
        }

        String shortUrl = baseUrl + code;
//        urlMap.put(code, new UrlMapping(originalUrl, shortUrl));
        urlMap.put(shortUrl, originalUrl);
        return shortUrl;
    }

    public String expandUrl(String shortUrl) {
        return urlMap.get(shortUrl);
    }

    public Map<String, String> getAllUrls() {
        return urlMap;
    }

    private String generateCode() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        while (urlMap.containsKey(code.toString())) {
            code.setCharAt(random.nextInt(6), chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }
}
