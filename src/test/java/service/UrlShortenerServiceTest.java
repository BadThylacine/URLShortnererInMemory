package test.java.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import service.UrlShortenerService;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class UrlShortenerServiceTest {

    private static final String BASE_URL = "http://localhost:8080/";
    private static final String SAFE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~";
    private static final int CODE_LENGTH = 4;

    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        service = new UrlShortenerService("http://localhost:8080");
    }

    // --- shortenUrl ---

    @Nested
    class ShortenUrl {

        @Test
        void validUrl_returnsShortUrlWithCorrectPrefix() {
            String shortUrl = service.shortenUrl("https://example.com/page");
            assertTrue(shortUrl.startsWith(BASE_URL));
        }

        @Test
        void validUrl_codeIsExactlyFourChars() {
            String shortUrl = service.shortenUrl("https://example.com/page");
            String code = extractCode(shortUrl);
            assertEquals(CODE_LENGTH, code.length());
        }

        @Test
        void validUrl_codeContainsOnlySafeChars() {
            String shortUrl = service.shortenUrl("https://example.com/page");
            String code = extractCode(shortUrl);
            for (char c : code.toCharArray()) {
                assertTrue(SAFE_CHARS.indexOf(c) >= 0,
                        "Unexpected character in code: " + c);
            }
        }

        @Test
        void consecutiveCalls_produceUniqueCode() {
            String code1 = extractCode(service.shortenUrl("https://example.com/one"));
            String code2 = extractCode(service.shortenUrl("https://example.com/two"));
            assertNotEquals(code1, code2);
        }

        @Test
        void sameLongUrl_returnsSameCode() {
            String code1 = extractCode(service.shortenUrl("https://example.com/page"));
            String code2 = extractCode(service.shortenUrl("https://example.com/page"));
            assertEquals(code1, code2);
        }

        @Test
        void urlWithQueryParams_preservedExactly() {
            String original = "https://example.com/search?q=hello&lang=en";
            String shortUrl = service.shortenUrl(original);
            String code = extractCode(shortUrl);
            assertEquals(original, service.resolveByCode(code));
        }

        @Test
        void urlWithFragment_preservedExactly() {
            String original = "https://example.com/docs#section-3";
            String shortUrl = service.shortenUrl(original);
            String code = extractCode(shortUrl);
            assertEquals(original, service.resolveByCode(code));
        }

        @ParameterizedTest(name = "rejects scheme: {0}")
        @ValueSource(strings = {
                "javascript:alert(1)",
                "file:///etc/passwd",
                "ftp://example.com",
                "data:text/html,<h1>hi</h1>",
                "not-a-url",
                "",
                "http//missing-colon.com"
        })
        void invalidOrUnsafeUrl_returnsError(String input) {
            assertEquals("Error: Invalid input", service.shortenUrl(input));
        }

        @ParameterizedTest(name = "accepts scheme: {0}")
        @ValueSource(strings = {
                "http://example.com",
                "https://example.com",
                "https://example.com/path?query=1#fragment"
        })
        void httpAndHttpsUrls_areAccepted(String input) {
            String result = service.shortenUrl(input);
            assertTrue(result.startsWith(BASE_URL),
                    "Expected short URL but got: " + result);
        }
    }

    // --- resolveByCode ---

    @Nested
    class ResolveByCode {

        @Test
        void knownCode_returnsOriginalUrl() {
            String original = "https://example.com/page";
            String shortUrl = service.shortenUrl(original);
            String code = extractCode(shortUrl);
            assertEquals(original, service.resolveByCode(code));
        }

        @Test
        void unknownCode_returnsNull() {
            assertNull(service.resolveByCode("aaaa"));
        }

        @ParameterizedTest(name = "invalid code: \"{0}\"")
        @ValueSource(strings = {
                "",
                "abc",       // too short
                "abcde",     // too long
                "ab!c",      // invalid char
                "ab c",      // space
                "ab/c",      // slash
        })
        void invalidCode_returnsNull(String code) {
            assertNull(service.resolveByCode(code));
        }

        @Test
        void nullCode_returnsNull() {
            assertNull(service.resolveByCode(null));
        }
    }

    // --- Thread safety ---

    @Nested
    class Concurrency {

        @Test
        void concurrentUniqueCalls_produceUniqueCodesWithNoCollisions() throws InterruptedException {
            int threadCount = 100;
            Set<String> codes = Collections.newSetFromMap(new ConcurrentHashMap<>());
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final int index = i;
                executor.submit(() -> {
                    try {
                        String shortUrl = service.shortenUrl("https://example.com/page/" + index);
                        codes.add(extractCode(shortUrl));
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();
            assertEquals(threadCount, codes.size(), "Duplicate codes detected under concurrent load");
        }

        @Test
        void concurrentDuplicateCalls_returnSameCode() throws InterruptedException {
            String original = "https://example.com/page";
            int threadCount = 50;
            Set<String> codes = Collections.newSetFromMap(new ConcurrentHashMap<>());
            CountDownLatch latch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        codes.add(extractCode(service.shortenUrl(original)));
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();
            assertEquals(1, codes.size(), "Same URL should always map to the same code");
        }
    }

    // --- Helpers ---

    private String extractCode(String shortUrl) {
        return shortUrl.substring(shortUrl.lastIndexOf('/') + 1);
    }
}