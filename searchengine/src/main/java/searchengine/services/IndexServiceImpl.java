package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StartAndStopIndexingResponse;
import searchengine.exaptions.CrawlingOfPagesFailed;
import searchengine.model.IndexingStatus;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexServiceImpl implements IndexService {
    private final SitesList sites;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private static CountDownLatch latchThreads;
    private static ForkJoinPool forkJoinPool = new ForkJoinPool(8);

    private static StartAndStopIndexingResponse indexingResponse;
    private volatile boolean isIndexingGoing;

    public StartAndStopIndexingResponse startIndexing() {

        List<Site> sitesList = sites.getSites();

        for (Site site : sitesList) {

            String rootLink = site.getUrl();
            String siteName = site.getName();
            log.info(siteName);

            Optional<SiteEntity> siteEntity =
                    siteRepository.findByName(site.getName());
            if (siteEntity.isPresent()) {
                pageRepository.deleteBySite(siteEntity.get());
                siteRepository.delete(siteEntity.get());
            }
            SiteEntity currentSiteEntity = new SiteEntity();
            currentSiteEntity.setIndexingStatus(IndexingStatus.INDEXING);
            isIndexingGoing = true;
            currentSiteEntity.setUrl(rootLink);
            currentSiteEntity.setName(siteName);
            currentSiteEntity.setStatusTime(LocalDateTime.now());
            currentSiteEntity.setErrorText(null);

            siteRepository.save(currentSiteEntity);

            latchThreads = new CountDownLatch(8);

            SiteCrawler siteCrawler = new SiteCrawler(rootLink, currentSiteEntity,
                    siteRepository, pageRepository, latchThreads);

            forkJoinPool.invoke(siteCrawler);

            try {
                latchThreads.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }


            siteRepository.save(currentSiteEntity);

            isIndexingGoing = false;


            if (currentSiteEntity.getIndexingStatus().equals(IndexingStatus.FAILED)) {

                currentSiteEntity.setErrorText(new CrawlingOfPagesFailed().getMessage());
                siteRepository.save(currentSiteEntity);
                indexingResponse = new StartAndStopIndexingResponse();
                indexingResponse.setResult(false);
                indexingResponse.setError(currentSiteEntity.getErrorText());

            } else if (!forkJoinPool.isShutdown()) {
                
                currentSiteEntity.setIndexingStatus(IndexingStatus.INDEXED);
                siteRepository.save(currentSiteEntity);
                indexingResponse = new StartAndStopIndexingResponse();
            }


        }

//        long countIndexingSites = siteRepository.findAll()
//                .stream()
//                .map(SiteEntity::getIndexingStatus)
//                .filter(indexingStatus -> indexingStatus.equals(IndexingStatus.INDEXING)).count();
//        if (countIndexingSites > 0) {
//            isIndexingGoing = true;
//        }
        return indexingResponse;
    }

    public StartAndStopIndexingResponse stopIndexing() {

        forkJoinPool.shutdown();
        if (isIndexingGoing) {
            indexingResponse = new StartAndStopIndexingResponse();
        } else {
            indexingResponse = new StartAndStopIndexingResponse();
            indexingResponse.setResult(false);
            indexingResponse.setError("Индексация не запущена");
        }
        return indexingResponse;

    }

}

