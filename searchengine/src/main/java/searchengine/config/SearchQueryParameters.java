package searchengine.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Data
public class SearchQueryParameters {



    private String query; //поисковый запрос
    private List<String> site; // url сайта
    private int offset; // сдвиг от 0 для постраничного вывода

    private int limit; // количество результатов, которое необходимо вывести
    // (параметр необязательный; если не установлен, то значение по умолчанию равно 20).

    public SearchQueryParameters(String query, List<String> site, int offset, int limit) {
        this.query = query;
        this.site = site;
        this.offset = offset;
        this.limit = limit;
    }

    public SearchQueryParameters(String query, List<String> site) {

        this(query, site, 0,20);

    }




    public SearchQueryParameters(SitesList sites, String query, String site, int offset, int limit) {

        this.query = query;
        this.site = List.of(site);
        this.offset = offset;
        this.limit = limit;
    }





}
