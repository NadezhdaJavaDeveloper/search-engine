package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import searchengine.config.QueryParametersForIndexedSite;


import java.io.IOException;


@RequiredArgsConstructor
public class ConnectingToLink {

    private final QueryParametersForIndexedSite parameters;
    private ConnectingToLink() {
        parameters = new QueryParametersForIndexedSite();
    }

    public static Connection.Response getConnectionToLink(String link) throws IOException {

        ConnectingToLink connection = new ConnectingToLink();


        return Jsoup.connect(link)
                .ignoreHttpErrors(true)
                .followRedirects(false)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .timeout(5000)
                .execute();

//        return Jsoup.connect(link)
//                .ignoreHttpErrors(true)
//                .followRedirects(false)
//                .userAgent(connection.parameters.getUserAgent())
//                .referrer(connection.parameters.getReferrer())
//                .timeout(connection.parameters.getTimeOut())
//                .execute();
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