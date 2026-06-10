package controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import service.UrlShortenerService;

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

        String shortUrl = service.shortenUrl(submittedUrl);
        if (shortUrl.startsWith("Error:")) {
            model.addAttribute("errorMessage", shortUrl.substring(7).trim());
            return "index";
        }

        model.addAttribute("shortUrl", shortUrl);
        return "index";
    }
}
