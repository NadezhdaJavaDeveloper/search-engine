package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Document;
import org.springframework.dao.DataIntegrityViolationException;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RecursiveAction;

@Slf4j
@RequiredArgsConstructor
public class SiteCrawler extends RecursiveAction {
    private final String rootLink;
    private final SiteEntity currentSiteEntity;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    private final CountDownLatch latchThreads;

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;

    private static ConcurrentSkipListSet<String> linksList = new ConcurrentSkipListSet<>();


    @Override
    protected void compute() {

        List<SiteCrawler> taskList = new ArrayList<>();
        try {
            Connection.Response response = connectingToLink(rootLink);
            Document doc = response.parse();
            int statusCode = response.statusCode();
            if (statusCode < 400) {
                PageEntity currentPageEntity = createPageEntity(currentSiteEntity, rootLink, statusCode, doc.toString());
                try {
                    pageRepository.save(currentPageEntity);
                    FillingDatabaseLemmaIndex fillingDatabaseLemmaIndex = new FillingDatabaseLemmaIndex(lemmaRepository, indexRepository);
                    fillingDatabaseLemmaIndex.createLemmaAndIndex(currentPageEntity, currentSiteEntity);

                    currentSiteEntity.setStatusTime(LocalDateTime.now());
                    siteRepository.save(currentSiteEntity);
                } catch (DataIntegrityViolationException ex) {
                    //перехват дубликатов страниц
                    ex.printStackTrace();
                }
            }
            Elements lines = doc.select("a");
            for (Element line : lines) {
                String underLink = line.attr("abs:href");
                if (!linksList.contains(underLink) && underLink.startsWith(rootLink) && !underLink.contains("#")) {
                    linksList.add(underLink);
                    SiteCrawler task = new SiteCrawler(underLink, currentSiteEntity, siteRepository, pageRepository,
                            latchThreads, lemmaRepository, indexRepository);
                    task.fork();
                    taskList.add(task);
                }
            }
            for (SiteCrawler task : taskList) {
                task.join();
            }
            latchThreads.countDown();
        } catch (Exception e) {

            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            currentSiteEntity.setErrorText(errors.toString());
            currentSiteEntity.setIndexingStatus(IndexingStatus.FAILED);
            siteRepository.save(currentSiteEntity);

        }
    }

    public static Connection.Response connectingToLink(String link) throws IOException {
        return Jsoup.connect(link)
                .ignoreHttpErrors(true)
                .followRedirects(false)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .timeout(5000)
                .execute();
    }

    public static PageEntity createPageEntity(SiteEntity siteEntity, String path, int httpResponse, String content) {
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