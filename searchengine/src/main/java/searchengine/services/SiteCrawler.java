package searchengine.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Document;
import searchengine.model.IndexingStatus;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RecursiveAction;

@Slf4j
public class SiteCrawler extends RecursiveAction {
    private String rootLink;
    private final SiteEntity currentSiteEntity;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private CountDownLatch latchThreads;

    public SiteCrawler(String rootLink, SiteEntity currentSiteEntity, SiteRepository siteRepository, PageRepository pageRepository, CountDownLatch latchThreads) {
        this.rootLink = rootLink;
        this.currentSiteEntity = currentSiteEntity;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.latchThreads = latchThreads;
    }

    private static Connection.Response response = null;
    private static Set<String> linksList = ConcurrentHashMap.newKeySet();

    @Override
    protected void compute() {

        List<SiteCrawler> taskList = new ArrayList<>();
        try {
            Document doc = null;
            response = Jsoup.connect(rootLink)
                    .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                    .referrer("http://www.google.com")
                    .timeout(5000)
                    .execute();
            doc = response.parse();
            int statusCode = response.statusCode();
            PageEntity newPageEntity = createPageEntity(currentSiteEntity, rootLink, statusCode, doc.toString());

            pageRepository.save(newPageEntity);
            currentSiteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(currentSiteEntity);

            latchThreads.countDown();
            Elements lines = doc.select("a");
            for (Element line : lines) {
                String underLink = line.attr("abs:href");
                if (!linksList.contains(underLink) && underLink.startsWith(rootLink) && !underLink.contains("#")) {

                    SiteCrawler task = new SiteCrawler(underLink, currentSiteEntity, siteRepository, pageRepository, latchThreads);
                    task.fork();
                    taskList.add(task);
                    linksList.add(underLink);
                }
            }
            for (SiteCrawler task : taskList) {
                task.join();
            }
        } catch (Exception e) {
            currentSiteEntity.setIndexingStatus(IndexingStatus.FAILED);
            siteRepository.save(currentSiteEntity);
            e.printStackTrace();
        }
    }

    private PageEntity createPageEntity(SiteEntity siteEntity, String path, int httpResponse, String content) {
        PageEntity newPageEntity = new PageEntity();
        newPageEntity.setSite(siteEntity);
        newPageEntity.setPath(path);
        newPageEntity.setCode(httpResponse);
        newPageEntity.setContent(content);
        return newPageEntity;
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