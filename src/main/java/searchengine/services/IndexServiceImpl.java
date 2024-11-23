package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.exceptions.CrawlingOfPagesFailedException;
import searchengine.exceptions.ForcedStopOfIndexingException;
import searchengine.exceptions.InconsistencyWithConfigurationFileException;
import searchengine.exceptions.UntimelyCommandException;
import searchengine.model.*;
import searchengine.repository.*;

import java.io.IOException;

import java.net.URLDecoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService {

    private final SitesList sites;
    private List<Site> sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private ForkJoinPool forkJoinPool = new ForkJoinPool();
    private static final AtomicBoolean stopFlag = new AtomicBoolean(true);
    private static final Logger logger = LoggerFactory.getLogger(IndexServiceImpl.class);

    public static boolean isStopFlag() {
        return stopFlag.get();
    }

    public IndexingResponse startIndexing() {

        if (!isStopFlag()) {
            throw new UntimelyCommandException("Индексация уже запущена");
        } else {
            stopFlag.set(false);
        }
        sitesList = sites.getSites();
        for (Site site : sitesList) {
            String rootLink = site.getUrl();
            String siteName = site.getName();
            Optional<SiteEntity> siteEntityFromDB = siteRepository.findByName(siteName);
            siteEntityFromDB.ifPresent(this::clearDatabase);
            SiteEntity currentSiteEntity = createSiteEntity(rootLink, siteName);
            siteRepository.save(currentSiteEntity);
            boolean isRootLink = true;
            SiteCrawler siteCrawler = new SiteCrawler(rootLink, currentSiteEntity, siteRepository,
                    pageRepository, lemmaRepository, indexRepository, isRootLink);
            forkJoinPool.execute(siteCrawler);
        }
        return new IndexingResponse();
    }

    public IndexingResponse stopIndexing() {
        if (!stopFlag.get()) {
            stopFlag.set(true);
        } else {
            throw new UntimelyCommandException("Индексация не запущена");
        }
        return new IndexingResponse();
    }

    @Override
    public IndexingResponse indexPage(String introducedPath) {
        String path = URLDecoder.decode(introducedPath);
        if (isPageBelongsToExistingListOfSites(path).isEmpty()) {
            throw new InconsistencyWithConfigurationFileException("Данная страница находится за пределами сайтов,указанных в конфигурационном файле");
        }
        Site site = isPageBelongsToExistingListOfSites(path).get();
        stopFlag.set(false);
        String siteUrl = site.getUrl();
        String siteName = site.getName();
        if (siteRepository.findByName(siteName).isEmpty()) {
            siteRepository.save(createSiteEntity(siteUrl, siteName));
        }
        SiteEntity currentSiteEntity = siteRepository.findByName(siteName).get();
        currentSiteEntity.setIndexingStatus(IndexingStatus.INDEXING);
        try {
            Optional<PageEntity> pageEntityFromDB = pageRepository.findByPathAndSiteId(path, currentSiteEntity.getId());
            PageEntity pageEntity;
            if (pageEntityFromDB.isPresent()) {
                pageEntity = pageEntityFromDB.get();
                deletePageData(pageEntity);
            } else {
                pageEntity = createPageEntity(currentSiteEntity, path);
                pageRepository.save(pageEntity);
            }
            if (pageEntity.getCode() >= 400) {
                throw new CrawlingOfPagesFailedException("Не удается индексировать страницу");
            }
            FillingDatabaseLemmaIndex fillingDatabaseLemmaIndex = new FillingDatabaseLemmaIndex(lemmaRepository, indexRepository);
            fillingDatabaseLemmaIndex.createLemmaAndIndex(pageEntity, currentSiteEntity);
            if (!isStopFlag()) {
                currentSiteEntity.setErrorText("");
                currentSiteEntity.setIndexingStatus(IndexingStatus.INDEXED);
            } else {
                throw new ForcedStopOfIndexingException("Индексация остановлена пользователем");
            }
        } catch (Exception e) {
            currentSiteEntity.setIndexingStatus(IndexingStatus.FAILED);
            logger.error(String.valueOf(e));
            currentSiteEntity.setErrorText("Не удается индексировать страницу");
        } finally {
            currentSiteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(currentSiteEntity);
        }
        return new IndexingResponse();
    }

    private void deletePageData(PageEntity pageEntity) {
        List<Integer> lemmaIdList = indexRepository.findByPageId(pageEntity.getId());
        indexRepository.deleteByPageId(pageEntity.getId());
        List<LemmaEntity> lemmaList = lemmaRepository.findAllById(lemmaIdList);
        for (LemmaEntity lemma : lemmaList) {
            clearingDatabaseLemma(lemma);
        }
    }


    private void clearDatabase(SiteEntity siteEntity) {

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
        if (lemma.getFrequency() == 1) {
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

        Connection.Response response = ConnectingToLink.getConnectionToLink(path);

        Document doc = response.parse();
        int statusCode = response.statusCode();

        return SiteCrawler.createPageEntity(siteEntity, path, statusCode, doc.toString());
    }


}

