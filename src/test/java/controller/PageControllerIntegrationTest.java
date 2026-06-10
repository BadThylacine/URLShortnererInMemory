package test.java.controller;

import app.UrlShortenerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = UrlShortenerApplication.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PageControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void homePage_rendersThymeleafTemplate() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>URL Shortener</title>")))
                .andExpect(content().string(containsString("action=\"/shorten\"")))
                .andExpect(content().string(containsString("Shorten URL")));
    }

    @Test
    void submittingUrl_rendersGeneratedShortLink() throws Exception {
        mockMvc.perform(post("/shorten")
                        .param("url", "https://example.com/page"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Short URL created:")))
                .andExpect(content().string(containsString("http://localhost:8080/")))
                .andExpect(content().string(containsString("Copy short URL")));
    }

    @Test
    void blankUrl_rendersValidationMessage() throws Exception {
        mockMvc.perform(post("/shorten")
                        .param("url", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("URL is required")));
    }
}
