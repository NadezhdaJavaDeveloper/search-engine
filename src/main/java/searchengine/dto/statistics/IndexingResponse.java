package searchengine.dto.statistics;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IndexingResponse {
    private boolean result;
    private String error;

    public IndexingResponse() {
        result = true;
        error = null;
    }



}
