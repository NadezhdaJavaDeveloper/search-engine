package searchengine.dto.statistics;

import searchengine.model.IndexingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SiteResponse {

    private final IndexingStatus status;
    private final LocalDateTime statusTime;
    private final String errorText;
    private final String url;
    private final String siteName;

    public SiteResponse(IndexingStatus status, LocalDateTime statusTime, String errorText, String url, String siteName) {
        this.status = status;
        this.statusTime = statusTime;
        this.errorText = errorText;
        this.url = url;
        this.siteName = siteName;
    }

    public IndexingStatus getStatus() {
        return status;
    }

    public LocalDateTime getStatusTime() {
        return statusTime;
    }

    public String getErrorText() {
        return errorText;
    }

    public String getUrl() {
        return url;
    }

    public String getSiteName() {
        return siteName;
    }


}
