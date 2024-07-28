package searchengine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@Configuration
@ConfigurationProperties(prefix = "query-parameters")
public class QueryParametersForIndexedSite {

    private String userAgent;
    private String referrer;
    private Integer timeOut;

}
