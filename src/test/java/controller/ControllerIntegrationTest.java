package controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import app.UrlShortenerApplication;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = UrlShortenerApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- POST /api/shorten ---

    @Nested
    class ShortenEndpoint {

        @Test
        void validUrl_returns200WithShortUrl() throws Exception {
            MvcResult result = performShorten("https://example.com/page")
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode body = parseBody(result);
            assertThat(body.has("shortUrl")).isTrue();

            String code = extractCode(body.get("shortUrl").asText());
            assertThat(code).hasSize(4);
            assertThat(code).matches("[a-zA-Z0-9\\-._~]{4}");
        }

        @Test
        void missingUrlField_returns400WithMessage() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            JsonNode body = parseBody(result);
            assertThat(body.get("message").asText()).isEqualTo("URL is required");
        }

        @Test
        void invalidUrl_returns400WithMessage() throws Exception {
            MvcResult result = performShorten("not-a-url")
                    .andExpect(status().isBadRequest())
                    .andReturn();

            JsonNode body = parseBody(result);
            assertThat(body.get("message").asText()).isEqualTo("Invalid input");
        }

        @Test
        void javascriptScheme_returns400() throws Exception {
            performShorten("javascript:alert(1)")
                    .andExpect(status().isBadRequest());
        }

        @Test
        void ftpScheme_returns400() throws Exception {
            performShorten("ftp://example.com")
                    .andExpect(status().isBadRequest());
        }

        @Test
        void emptyBody_returns400() throws Exception {
            mockMvc.perform(post("/api/shorten")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void sameUrlTwice_returnsSameShortUrl() throws Exception {
            String payload = "https://example.com/page";

            MvcResult first = performShorten(payload)
                    .andExpect(status().isOk())
                    .andReturn();
            MvcResult second = performShorten(payload)
                    .andExpect(status().isOk())
                    .andReturn();

            String firstUrl = parseBody(first).get("shortUrl").asText();
            String secondUrl = parseBody(second).get("shortUrl").asText();
            assertThat(firstUrl).isEqualTo(secondUrl);
        }
    }

    // --- GET /{code} ---

    @Nested
    class RedirectEndpoint {

        @Test
        void knownCode_returns302WithLocationHeader() throws Exception {
            String original = "https://example.com/page";
            String code = shortenAndExtractCode(original);

            mockMvc.perform(get("/" + code))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", original));
        }

        @Test
        void knownCode_locationPreservesQueryParams() throws Exception {
            String original = "https://example.com/search?q=hello&lang=en";
            String code = shortenAndExtractCode(original);

            mockMvc.perform(get("/" + code))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", original));
        }

        @Test
        void unknownCode_returns404() throws Exception {
            mockMvc.perform(get("/zzzz"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void tooLongCode_returns404() throws Exception {
            mockMvc.perform(get("/abcde"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void tooShortCode_returns404() throws Exception {
            mockMvc.perform(get("/abc"))
                    .andExpect(status().isNotFound());
        }
    }

    // --- Helpers ---

    private org.springframework.test.web.servlet.ResultActions performShorten(String url) throws Exception {
        String payload = "{\"url\": \"" + url + "\"}";
        return mockMvc.perform(post("/api/shorten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload));
    }

    private String shortenAndExtractCode(String originalUrl) throws Exception {
        MvcResult result = performShorten(originalUrl)
                .andExpect(status().isOk())
                .andReturn();
        String shortUrl = parseBody(result).get("shortUrl").asText();
        return extractCode(shortUrl);
    }

    private JsonNode parseBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String extractCode(String shortUrl) {
        return shortUrl.substring(shortUrl.lastIndexOf('/') + 1);
    }
}