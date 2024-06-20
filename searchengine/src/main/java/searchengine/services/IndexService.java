package searchengine.services;

import searchengine.config.Page;
import searchengine.dto.statistics.StartAndStopIndexingResponse;

import java.util.concurrent.ExecutionException;

public interface IndexService {

    StartAndStopIndexingResponse startIndexing() throws ExecutionException, InterruptedException;
    StartAndStopIndexingResponse stopIndexing();
    StartAndStopIndexingResponse indexPage(Page page);
}
