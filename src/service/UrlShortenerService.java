package service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UrlShortenerService {

    public static final int CODE_LENGTH = 4;
    private static final String CHARS =
            "abcdefghijklmnopqrstuvwxyz"
                    + "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                    + "0123456789"
                    + "-._~";
    private static final int BASE = CHARS.length(); // 66

    private final String baseUrl;
    private final Map<String, String> codeToUrl = new ConcurrentHashMap<>();
    private final Map<String, String> urlToCode = new ConcurrentHashMap<>();
    private final AtomicLong counter = new AtomicLong(1);

    // Initialises the service, injecting and normalising the configured base URL.
    public UrlShortenerService(@Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    // Validates and shortens a URL,
    // reusing an existing code if already assigned,
    // or returning an error if input is invalid or capacity is exceeded.
    public String shortenUrl(String originalUrl) {
        if (!isValidUrl(originalUrl)) {
            return "Error: Invalid input";
        }

        String existingCode = urlToCode.get(originalUrl);
        if (existingCode != null) {
            return baseUrl + existingCode;
        }

        try {
            String code = urlToCode.computeIfAbsent(originalUrl, key -> {
                String newCode;
                do {
                    newCode = generateCode();
                } while (codeToUrl.putIfAbsent(newCode, key) != null);
                return newCode;
            });
            return baseUrl + code;
        } catch (IllegalStateException e) {
            return "Error: URL shortener capacity exceeded";
        }
    }

    // Looks up the original URL for a given short code,
    // returning null if the code is invalid or unknown.
    public String resolveByCode(String code) {
        if (!isValidCode(code)) return null;
        return codeToUrl.get(code);
    }

    // Generates the next short code by encoding the counter value,
    // padding to CODE_LENGTH, and asserting the result fits.
    private String generateCode() {
        long id = counter.getAndIncrement();
        String code = toBase66(id);

        int paddingNeeded = CODE_LENGTH - code.length();
        if (paddingNeeded < 0) {
            throw new IllegalStateException("Code generated exceeds defined length");
        }

        return "a".repeat(paddingNeeded) + code;
    }

    // Converts a numeric ID to a base-66 string using the defined character set.
    private String toBase66(long id) {
        if (id == 0) return String.valueOf(CHARS.charAt(0));

        StringBuilder code = new StringBuilder();
        while (id > 0) {
            code.append(CHARS.charAt((int) (id % BASE)));
            id /= BASE;
        }
        return code.reverse().toString();
    }

    // Returns true only if the code is non-null, exactly CODE_LENGTH characters,
    // and composed entirely of allowed characters.
    private boolean isValidCode(String code) {
        if (code == null || code.length() != CODE_LENGTH) return false;
        for (char c : code.toCharArray()) {
            if (CHARS.indexOf(c) < 0) return false;
        }
        return true;
    }

    // Returns true only if the URL is non-blank, syntactically valid,
    // and uses an http or https scheme with a present host.
    private boolean isValidUrl(String fullUrl) {
        if (fullUrl == null || fullUrl.isBlank()) return false;
        try {
            URI uri = new URI(fullUrl.trim());
            String scheme = uri.getScheme();
            if (scheme == null) return false;
            if (!scheme.equals("http") && !scheme.equals("https")) return false;
            String host = uri.getHost();
            return host != null && !host.isBlank();
        } catch (URISyntaxException e) {
            return false;
        }
    }

    // Ensures the base URL is non-blank and always ends with a trailing slash.
    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) return "http://localhost:8080/";
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }
}