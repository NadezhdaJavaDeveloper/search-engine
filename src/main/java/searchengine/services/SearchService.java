package searchengine.services;

import searchengine.config.SearchQueryParameters;
import searchengine.config.Site;
import searchengine.dto.statistics.SearchResponse;

import java.util.List;

public interface SearchService {

    SearchResponse getSearchResults(String userRequest, List<Site> siteList, int offset, int limit );
}
