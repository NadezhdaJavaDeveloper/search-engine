package searchengine.exaptions;

public class UntimelyCommand extends RuntimeException {


    public UntimelyCommand(String error) {
        super(error);
    }
}
