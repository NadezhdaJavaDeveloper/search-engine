package searchengine.exaptions;

public class ForcedStopOfIndexing extends RuntimeException{

    //Индексация остановлена пользователем
    public ForcedStopOfIndexing(String error) {
        super(error);
    }
}
