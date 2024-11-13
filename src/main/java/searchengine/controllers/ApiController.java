package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.dto.statistics.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexService;
import searchengine.services.IndexServiceImpl;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexService indexService;
    private final SearchService searchService;
    private final SitesList sites;


    public ApiController(StatisticsService statisticsService, IndexServiceImpl indexService, SearchService searchService, SitesList sites) {
        this.statisticsService = statisticsService;
        this.indexService = indexService;
        this.searchService = searchService;
        this.sites = sites;
    }


    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() throws ExecutionException, InterruptedException {
        return new ResponseEntity<>(indexService.startIndexing(), HttpStatus.OK);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam(name = "url") String path) {

        return new ResponseEntity<>(indexService.indexPage(path), HttpStatus.OK);

    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {

        return new ResponseEntity<>(indexService.stopIndexing(), HttpStatus.OK);
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> search(@RequestParam String query,
                                                 @RequestParam(required = false) String siteUrl,
                                                 @RequestParam (defaultValue = "0") int offset,
                                                 @RequestParam(defaultValue = "20") int limit) {

        if(siteUrl == null) {
            return ResponseEntity.ok(searchService.getSearchResults(query, sites.getSites(), offset, limit));
        } else {
            List<Site> siteList = sites.getSites().stream().filter(site -> site.getUrl().equals(siteUrl)).collect(Collectors.toList());
            return ResponseEntity.ok(searchService.getSearchResults(query, siteList, offset, limit));

        }


    }


}
