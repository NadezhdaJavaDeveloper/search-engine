package searchengine.exaptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.statistics.ErrorResponse;

@ControllerAdvice
public class DefaultAdvice {

    @ExceptionHandler(CrawlingOfPagesFailed.class)
    public ResponseEntity<ErrorResponse> handlerException(CrawlingOfPagesFailed exception) {
        ErrorResponse response = new ErrorResponse(exception.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

}
