package searchengine.exceptions;

public class ForcedStopOfIndexingException extends RuntimeException{

    //Индексация остановлена пользователем
    public ForcedStopOfIndexingException(String error) {
        super(error);
    }
}
