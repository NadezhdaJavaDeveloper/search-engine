package searchengine.exaptions;

public class CrawlingOfPagesFailed extends RuntimeException{



    public CrawlingOfPagesFailed(String error) {
        super(error);
    }
}
