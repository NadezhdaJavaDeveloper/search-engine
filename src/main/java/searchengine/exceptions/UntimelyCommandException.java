package searchengine.exceptions;

public class UntimelyCommandException extends RuntimeException {


    //Индексация уже запущена or Индексация не запущена
    public UntimelyCommandException(String error) {
        super(error);
    }
}
