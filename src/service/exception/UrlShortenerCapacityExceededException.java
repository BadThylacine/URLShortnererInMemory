package service.exception;

public class UrlShortenerCapacityExceededException extends RuntimeException {

    public UrlShortenerCapacityExceededException(String message) {
        super(message);
    }
}
