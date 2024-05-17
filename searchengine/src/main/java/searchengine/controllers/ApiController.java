package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StartAndStopIndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.IndexService;
import searchengine.services.IndexServiceImpl;
import searchengine.services.StatisticsService;

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
    public ResponseEntity<StartAndStopIndexingResponse> startIndexing() {

        return new ResponseEntity<>(indexService.startIndexing(), HttpStatus.OK);
    }

    @PostMapping("/indexPage")
    public void indexPage(@RequestBody String url) {



    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<StartAndStopIndexingResponse> stopIndexing() {

        return new ResponseEntity<>(indexService.stopIndexing(), HttpStatus.OK);
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }
}
