package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import searchengine.model.*;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveAction;

public class SiteCrawler extends RecursiveAction {

    private final String rootLink;
    private final SiteEntity currentSiteEntity;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final boolean isRootLink;
    private static ConcurrentSkipListSet<String> linksList = new ConcurrentSkipListSet<>();

    private static final Logger logger = LoggerFactory.getLogger(SiteCrawler.class);

    public SiteCrawler(String rootLink, SiteEntity currentSiteEntity,
                       SiteRepository siteRepository, PageRepository pageRepository,
                       LemmaRepository lemmaRepository, IndexRepository indexRepository, boolean isRootLink) {
        this.rootLink = rootLink;
        this.currentSiteEntity = currentSiteEntity;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.isRootLink = isRootLink;
    }

    @Override
    protected void compute() {

        if (IndexServiceImpl.isStopFlag()) {
            currentSiteEntity.setIndexingStatus(IndexingStatus.FAILED);
            currentSiteEntity.setErrorText("Индексация остановлена пользователем");
            siteRepository.save(currentSiteEntity);
            return;
        }
        try {
            Connection.Response response = connectingToLink(rootLink);
            Document doc = response.parse();
            int responseCode = response.statusCode();
            if (responseCode >= 400) {
                return;
            }
            PageEntity currentPageEntity = createPageEntity(currentSiteEntity, rootLink, responseCode, doc.toString());

            pageRepository.save(currentPageEntity);
            FillingDatabaseLemmaIndex fillingDatabaseLemmaIndex = new FillingDatabaseLemmaIndex(lemmaRepository, indexRepository);
            fillingDatabaseLemmaIndex.createLemmaAndIndex(currentPageEntity, currentSiteEntity);
            currentSiteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(currentSiteEntity);

            List<SiteCrawler> subTasks = createSubTasks(doc);
            for (SiteCrawler task : subTasks) {
                task.fork();
            }
            for (SiteCrawler task : subTasks) {
                task.join();
            }
            if (isRootLink && currentSiteEntity.getErrorText() == null) {
                currentSiteEntity.setIndexingStatus(IndexingStatus.INDEXED);
            }
        } catch (DataIntegrityViolationException | SocketTimeoutException ex) {
            //перехват дубликатов страниц, тайм-аутов
            ex.printStackTrace();
        } catch (Exception e) {
            currentSiteEntity.setIndexingStatus(IndexingStatus.FAILED);
            currentSiteEntity.setErrorText("Не удалось выполнить сканирование всех страниц, содержащихся на указанном сайте");
            logger.error(String.valueOf(e));

        } finally {
            siteRepository.save(currentSiteEntity);
        }
    }

    private List<SiteCrawler> createSubTasks(Document doc) {
        List<SiteCrawler> subTaskList = new ArrayList<>();
        Elements lines = doc.select("a");
        for (Element line : lines) {
            String underLink = line.attr("abs:href");
            if (!linksList.contains(underLink) && underLink.startsWith(rootLink) && !underLink.contains("#")) {
                linksList.add(underLink);
                SiteCrawler subTask = new SiteCrawler(underLink, currentSiteEntity, siteRepository, pageRepository,
                        lemmaRepository, indexRepository, false);
                subTaskList.add(subTask);
            }
        }
        return subTaskList;
    }

    public static Connection.Response connectingToLink(String link) throws IOException {
        return ConnectingToLink.getConnectionToLink(link);

    }

    public static PageEntity createPageEntity(SiteEntity siteEntity, String path, int responseCode, String content) {
        PageEntity newPageEntity = new PageEntity();

        newPageEntity.setSite(siteEntity);
        newPageEntity.setPath(path);
        newPageEntity.setCode(responseCode);
        newPageEntity.setContent(content);

        return newPageEntity;
    }


}

