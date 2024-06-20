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
import searchengine.exaptions.CrawlingOfPagesFailed;
import searchengine.exaptions.ForcedStopOfIndexing;
import searchengine.exaptions.InconsistencyWithConfigurationFile;
import searchengine.exaptions.UntimelyCommand;
import searchengine.model.*;
import searchengine.repository.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
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
    private static final int COUNT_TREADS = 8;
    private static ForkJoinPool forkJoinPool;
    private static StartAndStopIndexingResponse indexingResponse;
    private volatile boolean isIndexingGoing;
    private List<Site> sitesList;

    public StartAndStopIndexingResponse startIndexing() throws ExecutionException, InterruptedException {
        if (isIndexingGoing) {throw new UntimelyCommand("Индексация уже запущена");}
        sitesList = sites.getSites();
        ExecutorService service = Executors.newFixedThreadPool(sitesList.size());
        Future<StartAndStopIndexingResponse> future = null;
            for (Site site : sitesList) {
                future = service.submit(() -> {
                    isIndexingGoing = true;
                    String rootLink = site.getUrl();
                    String siteName = site.getName();
                    Optional<SiteEntity> siteEntityFromDB = siteRepository.findByName(siteName);
                    if (siteEntityFromDB.isPresent()) clearingDatabase(siteEntityFromDB.get());
                    SiteEntity currentSiteEntity = createSiteEntity(rootLink, siteName);
                    siteRepository.save(currentSiteEntity);
                    CountDownLatch latchThreads = new CountDownLatch(COUNT_TREADS);
                    forkJoinPool = new ForkJoinPool(COUNT_TREADS);
                    SiteCrawler siteCrawler = new SiteCrawler(rootLink, currentSiteEntity, siteRepository, pageRepository, latchThreads, lemmaRepository, indexRepository);
                    forkJoinPool.invoke(siteCrawler);
                    try {
                        latchThreads.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (currentSiteEntity.getIndexingStatus().equals(IndexingStatus.FAILED) && !forkJoinPool.isShutdown()) {
                        currentSiteEntity.setErrorText("Не удалось выполнить сканирование всех страниц, содержащихся на указанном сайте");
                        siteRepository.save(currentSiteEntity);
                        throw new CrawlingOfPagesFailed(currentSiteEntity.getErrorText());
                    } else if (forkJoinPool.isShutdown()) {
                        currentSiteEntity.setErrorText("Индексация остановлена пользователем");
                        siteRepository.save(currentSiteEntity);
                        throw new ForcedStopOfIndexing(currentSiteEntity.getErrorText());
                    } else {
                        currentSiteEntity.setIndexingStatus(IndexingStatus.INDEXED);
                        siteRepository.save(currentSiteEntity);
                        indexingResponse = new StartAndStopIndexingResponse();
                    }
                    isIndexingGoing = false;
                    return indexingResponse;
                });
            }
            return future.get();
    }


    public StartAndStopIndexingResponse stopIndexing() {


        if (isIndexingGoing) {
            forkJoinPool.shutdownNow();
            indexingResponse = new StartAndStopIndexingResponse();
        } else {
            throw new UntimelyCommand("Индексация не запущена");
        }
        return indexingResponse;

    }

    @Override
    public StartAndStopIndexingResponse indexPage(Page page) {

        String url = page.getUrl();

        if (isPageBelongsToExistingListOfSites(url).isEmpty()) {
            throw new InconsistencyWithConfigurationFile("Данная страница находится за пределами сайтов,указанных в конфигурационном файле");
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

            List<Integer> lemmaIdList = indexRepository.findByPageId(currentPageEntity.get().getId());
            List<LemmaEntity> lemmaList = lemmaRepository.findAllById(lemmaIdList);

            for (LemmaEntity lemma : lemmaList) {
                clearingDatabaseLemma(lemma);
            }

            indexRepository.deleteByPage(currentPageEntity.get());
            pageRepository.deleteByPathAndSite(url, currentSiteEntity);

        }


        try {
            PageEntity pageEntity = createPageEntity(currentSiteEntity, url);
            if (pageEntity.getCode() >= 400) {
                throw new CrawlingOfPagesFailed("Не удается индексировать страницу");
            }
            pageRepository.save(pageEntity);

            FillingDatabaseLemmaIndex fillingDatabaseLemmaIndex = new FillingDatabaseLemmaIndex(lemmaRepository, indexRepository);

            fillingDatabaseLemmaIndex.createLemmaAndIndex(pageEntity, currentSiteEntity);

            indexingResponse = new StartAndStopIndexingResponse();
            currentSiteEntity.setIndexingStatus(IndexingStatus.INDEXED);

            siteRepository.save(currentSiteEntity);


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
        if (lemma.getFrequency() < 1) {
            lemmaRepository.delete(lemma);
        } else {
            lemma.setFrequency(lemma.getFrequency() - 1);
            lemmaRepository.save(lemma);
        }
    }


    private Optional<Site> isPageBelongsToExistingListOfSites(String url) {

        sitesList = sites.getSites();

        for (Site site : sitesList) {
            String siteUrl = site.getUrl();
            if (url.contains(siteUrl)) {
                return Optional.of(site);
            }
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

        Connection.Response response = SiteCrawler.connectingToLink(path);

        Document doc = response.parse();
        int statusCode = response.statusCode();

        return SiteCrawler.createPageEntity(siteEntity, path, statusCode, doc.toString());
    }


}

