package searchengine.exaptions;

public class InconsistencyWithConfigurationFile extends RuntimeException{



    public InconsistencyWithConfigurationFile(String error) {
        super(error);
    }
}