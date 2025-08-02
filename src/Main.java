import controller.UrlShortenerController;
import service.UrlShortenerService;
import view.ConsoleView;

public class Main {
    public static void main(String[] args) {
        UrlShortenerService service = new UrlShortenerService();
        UrlShortenerController controller = new UrlShortenerController(service);
        ConsoleView view = new ConsoleView(controller);
        view.start();
    }
}