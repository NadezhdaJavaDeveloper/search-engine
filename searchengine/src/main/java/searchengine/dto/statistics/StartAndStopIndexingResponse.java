package searchengine.dto.statistics;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartAndStopIndexingResponse {
    private boolean result;
    private String error;

    public StartAndStopIndexingResponse() {
        result = true;
        error = null;
    }

    public StartAndStopIndexingResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

}
