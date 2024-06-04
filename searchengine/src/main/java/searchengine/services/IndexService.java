package searchengine.services;

import searchengine.config.Page;
import searchengine.dto.statistics.StartAndStopIndexingResponse;

public interface IndexService {

    StartAndStopIndexingResponse startIndexing();
    StartAndStopIndexingResponse stopIndexing();



    StartAndStopIndexingResponse indexPage(Page page);
}
