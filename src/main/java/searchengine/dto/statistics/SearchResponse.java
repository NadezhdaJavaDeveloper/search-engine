package searchengine.dto.statistics;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SearchResponse {
    private boolean result;
    private int count;
    private List<SearchResult> data;

    public SearchResponse() {
        data = new ArrayList<>();
    }

}
