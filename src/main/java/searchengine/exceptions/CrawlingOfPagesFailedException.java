package searchengine.exceptions;

public class CrawlingOfPagesFailedException extends RuntimeException{

    //Не удалось выполнить сканирование всех страниц, содержащихся на указанном сайте
    public CrawlingOfPagesFailedException(String error) {
        super(error);
    }
}
