package service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;

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

    public UrlShortenerService(@Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    public String shortenUrl(String originalUrl) {
        if (!isValidUrl(originalUrl)) {
            return "Error: Invalid input";
        }

        String existingCode = urlToCode.get(originalUrl);
        if (existingCode != null) {
            return baseUrl + existingCode;
        }

        String code = generateCode();
        codeToUrl.put(code, originalUrl);
        urlToCode.put(originalUrl, code);
        return baseUrl + code;
    }

    public String resolveByCode(String code) {
        if (!isValidCode(code)) return null;
        return codeToUrl.get(code);
    }

    private String generateCode() {
        long id = counter.getAndIncrement();
        String code = toBase62(id);

        // Pad with leading 'a' (zero in Base62) to ensure fixed length
        while (code.length() < CODE_LENGTH) {
            code = CHARS.charAt(0) + code;
        }
        return code;
    }

    private String toBase62(long id) {
        if (id == 0) return String.valueOf(CHARS.charAt(0));

        StringBuilder code = new StringBuilder();
        while (id > 0) {
            code.append(CHARS.charAt((int) (id % BASE)));
            id /= BASE;
        }
        return code.reverse().toString();
    }

    private boolean isValidCode(String code) {
        if (code == null || code.length() != CODE_LENGTH) return false;
        for (char c : code.toCharArray()) {
            if (CHARS.indexOf(c) < 0) return false;
        }
        return true;
    }

    private boolean isValidUrl(String fullUrl) {
        try {
            URL url = new URL(fullUrl);
            String scheme = url.getProtocol();
            return scheme.equals("http") || scheme.equals("https");
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) return "http://localhost:8080/";
        return baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
    }
}