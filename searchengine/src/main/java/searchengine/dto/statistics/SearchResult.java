package searchengine.dto.statistics;

import lombok.Data;

@Data
public class SearchResult {
    private String site, siteName, uri, title, snippet;
    private double relevance;

}
