package controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import service.UrlShortenerService;
import service.exception.InvalidUrlException;
import service.exception.UrlShortenerCapacityExceededException;

@Controller
public class PageController {

    private final UrlShortenerService service;

    public PageController(UrlShortenerService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("submittedUrl", "");
        return "index";
    }

    @PostMapping("/shorten")
    public String shorten(@RequestParam(name = "url", required = false) String url, Model model) {
        String submittedUrl = url == null ? "" : url.trim();
        model.addAttribute("submittedUrl", submittedUrl);

        if (submittedUrl.isBlank()) {
            model.addAttribute("errorMessage", "URL is required");
            return "index";
        }

        try {
            String shortUrl = service.shortenUrl(submittedUrl);
            model.addAttribute("shortUrl", shortUrl);
        } catch (InvalidUrlException | UrlShortenerCapacityExceededException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "index";
        }
        return "index";
    }
}
