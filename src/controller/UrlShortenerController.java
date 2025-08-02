package controller;

import service.UrlShortenerService;

import java.util.Map;

public class UrlShortenerController {
    private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    public String createShortUrl(String originalUrl) {
        return service.shortenUrl(originalUrl);
    }

    public String getOriginalUrl(String shortUrl) {
        return service.expandUrl(shortUrl);
    }

    public Map<String, String> getAllData() {
        return service.getAllUrls();
    }
}
