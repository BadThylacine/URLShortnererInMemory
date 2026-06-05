package controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import service.UrlShortenerService;

@RestController
@RequestMapping("/api")
public class UrlShortenerController {
    private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    public String createShortUrl(String originalUrl) {
        return service.shortenUrl(originalUrl);
    }

    @PostMapping("/shorten")
    public ResponseEntity<?> shortenUrl(@RequestBody ShortenRequest request) {
        String url = request.url();
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("URL is required"));
        }

        String shortUrl = createShortUrl(url.trim());
        if (shortUrl.startsWith("Error:")) {
            return ResponseEntity.badRequest().body(new ErrorResponse(shortUrl.substring(7).trim()));
        }
        return ResponseEntity.ok(new ShortenResponse(shortUrl));
    }
}
