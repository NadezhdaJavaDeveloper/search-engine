package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.IndexingStatus;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.SiteRepository;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;

    @Override
    public StatisticsResponse getStatistics() {

        TotalStatistics total = new TotalStatistics();

        List<SiteEntity> siteEntityList = siteRepository.findAll();

        total.setSites(siteEntityList.size());

        List<IndexingStatus> statusList = siteEntityList.stream()
                .map(SiteEntity::getIndexingStatus)
                .filter(indexingStatus -> !indexingStatus.equals(IndexingStatus.INDEXING)).collect(Collectors.toList());

        total.setIndexing(statusList.size() <= 0);

        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        for (int i = 0; i < siteEntityList.size(); i++) {
            SiteEntity site = siteEntityList.get(i);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());

            List<PageEntity> pageEntities = site.getPageEntities();
            List<LemmaEntity> lemmaList = site.getLemmaList();

            int pages = pageEntities.size();
            int lemmas = lemmaList.size();
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(site.getIndexingStatus().toString());

            String error = site.getErrorText() == null ? "" : site.getErrorText();
            item.setError(error);

            LocalDateTime timeIndexSite = site.getStatusTime();
            ZonedDateTime zdt = ZonedDateTime.of(timeIndexSite, ZoneId.systemDefault());
            item.setStatusTime(zdt.toInstant().toEpochMilli());

            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);

            detailed.add(item);
        }

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);

        return response;
    }

}
