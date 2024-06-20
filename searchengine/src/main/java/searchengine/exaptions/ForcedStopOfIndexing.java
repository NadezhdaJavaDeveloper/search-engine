package searchengine.exaptions;

public class ForcedStopOfIndexing extends RuntimeException{



    public ForcedStopOfIndexing(String error) {
        super(error);
    }
}
