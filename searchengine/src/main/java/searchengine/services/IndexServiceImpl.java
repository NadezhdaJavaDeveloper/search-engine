package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.config.Page;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StartAndStopIndexingResponse;
import searchengine.model.*;
import searchengine.repository.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexServiceImpl implements IndexService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;

    //  private final Repositories repositories;
    private static final int COUNT_TREADS = 8;
    private static ForkJoinPool forkJoinPool;
    private static StartAndStopIndexingResponse indexingResponse;
    private volatile boolean isIndexingGoing;
    private List<Site> sitesList;


    public StartAndStopIndexingResponse startIndexing() {

        sitesList = sites.getSites();
        for (Site site : sitesList) {

            isIndexingGoing = true;

            String rootLink = site.getUrl();
            String siteName = site.getName();

            Optional<SiteEntity> siteEntityFromDB = siteRepository.findByName(siteName);

            if (siteEntityFromDB.isPresent()) clearingDatabase(siteEntityFromDB.get());

            SiteEntity currentSiteEntity = createSiteEntity(rootLink, siteName);

            siteRepository.save(currentSiteEntity);

            CountDownLatch latchThreads = new CountDownLatch(COUNT_TREADS);

            forkJoinPool = new ForkJoinPool(COUNT_TREADS);

            SiteCrawler siteCrawler = new SiteCrawler(rootLink, currentSiteEntity,
                    siteRepository, pageRepository, latchThreads, lemmaRepository, indexRepository);

            forkJoinPool.invoke(siteCrawler);

            try {
                latchThreads.await();
               // siteRepository.save(currentSiteEntity); - видимо ненужный этап

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (currentSiteEntity.getIndexingStatus().equals(IndexingStatus.FAILED)) {

              //  currentSiteEntity.setErrorText(new CrawlingOfPagesFailed().getMessage());
             //   siteRepository.save(currentSiteEntity);
                indexingResponse = new StartAndStopIndexingResponse(false, currentSiteEntity.getErrorText());

            } else if (!forkJoinPool.isShutdown()) {
                currentSiteEntity.setIndexingStatus(IndexingStatus.INDEXED);
                siteRepository.save(currentSiteEntity);
                indexingResponse = new StartAndStopIndexingResponse();
            }
            isIndexingGoing = false;
        }

        return indexingResponse;
    }

    public StartAndStopIndexingResponse stopIndexing() {


        if (isIndexingGoing) {
            forkJoinPool.shutdownNow();
            indexingResponse = new StartAndStopIndexingResponse();
        } else {
            indexingResponse = new StartAndStopIndexingResponse();
            indexingResponse.setResult(false);
            indexingResponse.setError("Индексация не запущена");
        }
        return indexingResponse;

    }

    @Override
    public StartAndStopIndexingResponse indexPage(Page page) {

        String url = page.getUrl();

        if (isPageBelongsToExistingListOfSites(url).isEmpty()) {
            indexingResponse = new StartAndStopIndexingResponse();
            indexingResponse.setResult(false);
            indexingResponse.setError("Данная страница находится за пределами сайтов,указанных в конфигурационном файле");
            return indexingResponse;
        }

        Site site = isPageBelongsToExistingListOfSites(url).get();

        String siteUrl = site.getUrl();
        String siteName = site.getName();

        if (siteRepository.findByName(siteName).isEmpty()) {
            siteRepository.save(createSiteEntity(siteUrl, siteName));
        }

        SiteEntity currentSiteEntity = siteRepository.findByName(siteName).get();

        Optional<PageEntity> currentPageEntity = pageRepository.findByPathAndSite(url, currentSiteEntity);

        if (currentPageEntity.isPresent()) {

            List<LemmaEntity> lemmaList = indexRepository.findByPage(currentPageEntity.get());

            for (LemmaEntity lemma : lemmaList) {
                clearingDatabaseLemma(lemma);
            }

            indexRepository.deleteByPage(currentPageEntity.get());
            pageRepository.deleteByPathAndSite(url, currentSiteEntity);

        }


        try {
            PageEntity pageEntity = createPageEntity(currentSiteEntity, url);
            if(pageEntity.getCode()>=400) {
                indexingResponse = new StartAndStopIndexingResponse();
                indexingResponse.setResult(false);
                indexingResponse.setError("Не удается индексировать страницу");
                return indexingResponse;
            }
            pageRepository.save(pageEntity);

            FillingDatabaseLemmaIndex fillingDatabaseLemmaIndex = new FillingDatabaseLemmaIndex(lemmaRepository, indexRepository);

            fillingDatabaseLemmaIndex.createLemmaAndIndex(pageEntity, currentSiteEntity);

            indexingResponse = new StartAndStopIndexingResponse();
            currentSiteEntity.setIndexingStatus(IndexingStatus.INDEXED);
            siteRepository.save(currentSiteEntity);

//            HashMap<String, Integer> lemma2count = getListLemmaOnPage(pageEntity.getContent());
//
//            for(Map.Entry<String, Integer> entry : lemma2count.entrySet()) {
//                String currentLemma = entry.getKey();
//                Integer countOfLemmaOnPage = entry.getValue();
//
//                LemmaEntity lemmaEntity = createLemmaEntity(currentSiteEntity, currentLemma);
//                lemmaRepository.save(lemmaEntity);
//
//                IndexEntity indexEntity = createIndexEntity(pageEntity, lemmaEntity, countOfLemmaOnPage);
//                indexRepository.save(indexEntity);
//
//            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }



        return indexingResponse;
    }

    private void clearingDatabase(SiteEntity siteEntity) {

        List<IndexEntity> indexEntityList = siteEntity.getPageEntities()
                .stream()
                .flatMap(pageEntity -> pageEntity.getIndexEntityList().stream())
                .collect(Collectors.toList());



        indexRepository.deleteAll(indexEntityList);
        lemmaRepository.deleteBySite(siteEntity);
        pageRepository.deleteBySite(siteEntity);
        siteRepository.delete(siteEntity);
    }


    private void clearingDatabaseLemma(LemmaEntity lemma) {
        if (lemmaRepository.findByLemma(lemma.getLemma()).isPresent()) {
            if (lemma.getFrequency() < 1) {
                lemmaRepository.delete(lemma);
            } else {
                lemma.setFrequency(lemma.getFrequency() - 1);
                lemmaRepository.save(lemma);
            }
        }
    }

    private Optional<Site> isPageBelongsToExistingListOfSites(String url) {

        sitesList = sites.getSites();

        for (Site site : sitesList) {
            String siteUrl = site.getUrl();
            log.info(siteUrl);
            log.info(url);
            if (url.contains(siteUrl)) return Optional.of(site);
        }
        return Optional.empty();
    }

    private SiteEntity createSiteEntity(String rootLink, String siteName) {
        SiteEntity currentSiteEntity = new SiteEntity();
        currentSiteEntity.setIndexingStatus(IndexingStatus.INDEXING);
        currentSiteEntity.setUrl(rootLink);
        currentSiteEntity.setName(siteName);
        currentSiteEntity.setStatusTime(LocalDateTime.now());
        currentSiteEntity.setErrorText(null);
        return currentSiteEntity;
    }

    private PageEntity createPageEntity(SiteEntity siteEntity, String path) throws IOException {

        Connection.Response response = Jsoup.connect(path)
                .ignoreHttpErrors(true)
                .followRedirects(false)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com")
                .timeout(5000)
                .execute();

        Document doc = response.parse();
        int statusCode = response.statusCode();
        String content = doc.toString();

        PageEntity newPageEntity = new PageEntity();
        newPageEntity.setSite(siteEntity);
        newPageEntity.setPath(path);
        newPageEntity.setCode(statusCode);
        newPageEntity.setContent(content);
        return newPageEntity;


    }

//    private HashMap<String, Integer> getListLemmaOnPage(String content) throws IOException {
//
//        ConvertingWordIntoLemma converter = ConvertingWordIntoLemma.getInstance();
//
//        String convertedText = converter.removingHtmlTags(content);
//
//        HashMap<String, Integer> lemma2count = converter.creatingListOfLemmas(convertedText);
//
//        return lemma2count;
//    }
//
//    private LemmaEntity createLemmaEntity(SiteEntity siteEntity, String lemma) {
//
//        LemmaEntity lemmaEntity = new LemmaEntity();
//        lemmaEntity.setSite(siteEntity);
//        lemmaEntity.setLemma(lemma);
//
//
//        int frequency = lemmaRepository.findByLemmaAndSite(lemma, siteEntity).isPresent() ?
//                lemmaRepository.findByLemmaAndSite(lemma, siteEntity).get().getFrequency() : 0;
//
//        lemmaEntity.setFrequency(frequency+1);
//        return lemmaEntity;
//
//    }
//
//    private IndexEntity createIndexEntity(PageEntity pageEntity, LemmaEntity lemmaEntity, int countLemmaOnPage) {
//
//        IndexEntity indexEntity = new IndexEntity();
//        indexEntity.setPageEntity(pageEntity);
//        indexEntity.setLemmaEntity(lemmaEntity);
//        indexEntity.setCountLemmaOnPage((float) countLemmaOnPage);
//        return indexEntity;
//
//    }


}

