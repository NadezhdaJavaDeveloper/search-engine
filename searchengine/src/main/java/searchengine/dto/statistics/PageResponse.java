package searchengine.dto.statistics;

public class PageResponse {

    private final String site;
    private final String path;
    private final int code;
    private final String content;

    public PageResponse(String site, String path, int code, String content) {
        this.site = site;
        this.path = path;
        this.code = code;
        this.content = content;
    }

    public String getSite() {
        return site;
    }

    public String getPath() {
        return path;
    }

    public int getCode() {
        return code;
    }

    public String getContent() {
        return content;
    }


}
