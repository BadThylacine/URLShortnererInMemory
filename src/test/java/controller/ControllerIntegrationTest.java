package controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shorten_validUrl_returnsShortenResponse() throws Exception {
        String payload = "{\"url\": \"https://example.com/page\"}";

        MvcResult result = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.has("shortUrl")).isTrue();
        String shortUrl = body.get("shortUrl").asText();
        assertThat(shortUrl).startsWith("http://localhost:8080/");
    }

    @Test
    void shorten_missingUrl_returnsBadRequest() throws Exception {
        String payload = "{}";

        MvcResult result = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("message").asText()).isEqualTo("URL is required");
    }

    @Test
    void shorten_invalidUrl_returnsBadRequestWithMessage() throws Exception {
        String payload = "{\"url\": \"javascript:alert(1)\"}";

        MvcResult result = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("message").asText()).isEqualTo("Invalid input");
    }

    @Test
    void redirect_knownCode_returnsFoundAndLocation() throws Exception {
        String original = "https://example.com/page";
        String payload = "{\"url\": \"" + original + "\"}";

        // create short url
        MvcResult shorten = mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(shorten.getResponse().getContentAsString());
        String shortUrl = body.get("shortUrl").asText();
        String code = shortUrl.substring(shortUrl.lastIndexOf('/') + 1);

        // request redirect
        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", original));
    }

    @Test
    void redirect_unknownCode_returnsNotFound() throws Exception {
        mockMvc.perform(get("/zzzz"))
                .andExpect(status().isNotFound());
    }
}
