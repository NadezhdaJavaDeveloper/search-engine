package searchengine.exaptions;

public class CrawlingOfPagesFailed extends RuntimeException{



    public CrawlingOfPagesFailed() {
        super("Не удалось выполнить сканирование всех страниц, содержащихся на указанном сайте");
    }
}
