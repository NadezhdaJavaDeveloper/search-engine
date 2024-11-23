package searchengine.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.statistics.ErrorResponse;

@ControllerAdvice
public class DefaultAdvice {

    @ExceptionHandler({CrawlingOfPagesFailedException.class, ForcedStopOfIndexingException.class,
            UntimelyCommandException.class, InconsistencyWithConfigurationFileException.class})
    public ResponseEntity<ErrorResponse> handlerException(Exception exception) {
        ErrorResponse response = new ErrorResponse(exception.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

}
