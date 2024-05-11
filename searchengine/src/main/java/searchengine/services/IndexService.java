package searchengine.services;

import searchengine.dto.statistics.StartAndStopIndexingResponse;

public interface IndexService {

    StartAndStopIndexingResponse startIndexing();
    StartAndStopIndexingResponse stopIndexing();
}
