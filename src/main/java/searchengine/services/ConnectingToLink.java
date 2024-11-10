package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import searchengine.config.QueryParametersForIndexedSite;


import java.io.IOException;



@Component
public class ConnectingToLink {
    
    private static QueryParametersForIndexedSite parameters;
    
    @Autowired
    public ConnectingToLink(QueryParametersForIndexedSite parameters) {
        this.parameters = parameters;
    }
    public static Connection.Response getConnectionToLink(String link) throws IOException {

        return Jsoup.connect(link)
                .ignoreHttpErrors(true)
                .followRedirects(false)
                .userAgent(parameters.getUserAgent())
                .referrer(parameters.getReferrer())
                .timeout(parameters.getTimeOut())
                .execute();
    }

}


//
//    private QueryParametersForIndexedSite queryParameters = new QueryParametersForIndexedSite();
//    @Value("${query-parameters.userAgent}")
//    private String userAgent;
//    @Value("${query-parameters.referrer}")
//    private String referrer;
//    @Value("${query-parameters.timeOut}")
//    private int timeout;