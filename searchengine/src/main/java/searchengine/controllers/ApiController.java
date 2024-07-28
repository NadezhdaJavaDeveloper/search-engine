package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.Page;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexService;
import searchengine.services.IndexServiceImpl;
import searchengine.services.StatisticsService;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexService indexService;

    public ApiController(StatisticsService statisticsService, IndexServiceImpl indexService) {
        this.statisticsService = statisticsService;
        this.indexService = indexService;
    }


    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() throws ExecutionException, InterruptedException {

        return new ResponseEntity<>(indexService.startIndexing(), HttpStatus.OK);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestBody Page page) {
        return new ResponseEntity<>(indexService.indexPage(page), HttpStatus.OK);


    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {

        return new ResponseEntity<>(indexService.stopIndexing(), HttpStatus.OK);
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
}
