package searchengine.exaptions;

public class CrawlingOfPagesFailed extends RuntimeException{

    //Не удалось выполнить сканирование всех страниц, содержащихся на указанном сайте
    public CrawlingOfPagesFailed(String error) {
        super(error);
    }
}
