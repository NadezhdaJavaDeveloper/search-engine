package searchengine.exaptions;

public class UntimelyCommand extends RuntimeException {


    //Индексация уже запущена or Индексация не запущена
    public UntimelyCommand(String error) {
        super(error);
    }
}
