package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.dto.statistics.SearchResponse;
import searchengine.dto.statistics.SearchResult;
import searchengine.exaptions.UntimelyCommand;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private static final String OPENING_TAG = "<b>";
    private static final String CLOSING_TAG = "</b>";

    private static final int SNIPPET_LENGTH = 300;
    private static final Logger logger = LoggerFactory.getLogger(SiteCrawler.class);

    @Override
    public SearchResponse getSearchResults(String userQuery, List<Site> siteList, int offset, int limit) {

        if (userQuery.isBlank()) throw new UntimelyCommand("Задан пустой поисковый запрос");

        List<SiteEntity> siteEntityList = getSiteEntityList(siteList);

        SearchResponse searchResponse = new SearchResponse();

        try {
            Map<String, Integer> sortedQueryList = getListOfLemmasSortedByFrequency(userQuery, siteEntityList);

            List<String> lisOfRelevantUrl = getLisOfRelevantUrl(sortedQueryList, siteEntityList);

            searchResponse.setResult(true);
            searchResponse.setCount(lisOfRelevantUrl.size());

            if (lisOfRelevantUrl.isEmpty()) {
                return searchResponse;
            }

            List<SearchResult> searchResultList = new LinkedList<>();
            SearchResult searchResult;

            Map<String, Double> pathSortedByRelevancePage = getPathSortedByRelevancePage(lisOfRelevantUrl, sortedQueryList, siteEntityList);

            for (String urlOfPage : pathSortedByRelevancePage.keySet()) {

                searchResult = new SearchResult();
                SiteEntity site = getCurrentSiteEntity(urlOfPage, siteEntityList);
                searchResult.setSite(site.getUrl());
                searchResult.setSiteName(site.getName());
                searchResult.setUri(urlOfPage.replace(site.getUrl(), ""));
                Document doc = getDoc(urlOfPage);
                searchResult.setTitle(doc.title());
                searchResult.setSnippet(getSnippet(sortedQueryList.keySet(), doc.toString()));
                searchResult.setRelevance(pathSortedByRelevancePage.get(urlOfPage));
                searchResultList.add(searchResult);
            }

            List<SearchResult> demonstratedSearchResultList = getDemonstratedSearchResultList(lisOfRelevantUrl, offset, limit, searchResultList);
            searchResponse.setData(demonstratedSearchResultList);


        } catch (IOException e) {
            logger.error(String.valueOf(e));
            throw new RuntimeException(e);
        }

        return searchResponse;
    }

    private List<SiteEntity> getSiteEntityList (List<Site> siteList) {

        List<SiteEntity> siteEntityList = new ArrayList<>();
        siteList.forEach(site -> siteEntityList.add(siteRepository.findByUrl(site.getUrl()).orElse(null)));
        siteEntityList.removeIf(Objects::isNull);

        return siteEntityList;
    }

    private Map<String, Integer> getListOfLemmasSortedByFrequency (String userQuery, List<SiteEntity> siteEntityList) throws IOException {

        ConvertingWordIntoLemma converter = ConvertingWordIntoLemma.getInstance();
        Set<String> queriesInFormOfLemma = converter.getListNormalForm2originForm(userQuery).keySet();

        TreeMap<String, Integer> query2frequency = new TreeMap<>();

        for (SiteEntity currentSite : siteEntityList) {
            for (String query : queriesInFormOfLemma) {
                Optional<LemmaEntity> queryFromDB = lemmaRepository.findByLemmaAndSiteId(query, currentSite.getId());
                queryFromDB.ifPresent(lemmaEntity -> query2frequency.put(query, getFrequency(lemmaEntity, query2frequency)));
            }
        }

        if (query2frequency.isEmpty()) {
            throw new UntimelyCommand("Попробуйте сформулировать запрос иначе");
        }

        int averageValueFrequency = getAverageValueFrequency(query2frequency);

        TreeMap<String, Integer> optimizedListQuery2frequency = query2frequency.entrySet().stream()
                .filter(entry -> entry.getValue() <= averageValueFrequency)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, TreeMap::new));

        return optimizedListQuery2frequency.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

    }

    private int getFrequency(LemmaEntity query, TreeMap<String, Integer> query2frequency) {

        int frequency = query.getFrequency();
        if (query2frequency.containsKey(query.getLemma())) {
            frequency = Math.min(frequency, query2frequency.get(query.getLemma()));

        }
        return frequency;

    }

    private int getAverageValueFrequency(TreeMap<String, Integer> query2frequency) {

        double clarifyingCoefficient = 1.15;
        int count = 0;
        int sum = 0;
        for (int frequency : query2frequency.values()) {
            sum += frequency;
            count++;
        }

        return (int) (sum/count*clarifyingCoefficient);

    }

    private List<String> getLisOfRelevantUrl(Map<String, Integer> sortedQueryList, List<SiteEntity> siteEntityList) {

        String rarestLemma = sortedQueryList.keySet().stream().findFirst().get();

        List<String> lisOfRelevantUrl = creatingListOfPagesForRarestLemma(rarestLemma, siteEntityList);

        for (String query : sortedQueryList.keySet()) {
            if (query.equals(rarestLemma)) {
                continue;
            }
            lisOfRelevantUrl = findListOfMostRelevantPages(query, siteEntityList, lisOfRelevantUrl);
        }

        return lisOfRelevantUrl;
    }

    private List<String> creatingListOfPagesForRarestLemma(String rarestLemma, List<SiteEntity> siteEntityList) {

        List<String> urlListForRarestLemma = new ArrayList<>();

        for(SiteEntity currentSite : siteEntityList) {

            Optional<LemmaEntity> lemmaEntity = lemmaRepository.findByLemmaAndSiteId(rarestLemma, currentSite.getId());
            if (lemmaEntity.isEmpty()) continue;
            List<Integer> pageIdList = getPageId(lemmaEntity.get(), currentSite);

            for (int pageId : pageIdList) {
                urlListForRarestLemma.add(pageRepository.findPathById(pageId));
            }
        }

        return urlListForRarestLemma;
    }

    private List<Integer> getPageId(LemmaEntity lemma, SiteEntity currentSite) {

        List<Integer> pageIdRelatedToCurrentSite = pageRepository.findBySiteId(currentSite.getId());

        return indexRepository.findByLemmaId(lemma.getId())
                .stream()
                .filter(pageIdRelatedToCurrentSite::contains)
                .collect(Collectors.toList());
    }

    private List<String> findListOfMostRelevantPages(String query, List<SiteEntity> siteEntityList,
                                                                List<String> lisOfRelevantUrlBeforeFilter ) {

        List<String> lisOfRelevantUrlAfterFilter = new ArrayList<>();

        for(SiteEntity currentSite : siteEntityList) {

            Optional<LemmaEntity> lemmaEntity = lemmaRepository.findByLemmaAndSiteId(query, currentSite.getId());
            if (lemmaEntity.isEmpty()) continue;

            List<Integer> pageIdList = getPageId(lemmaEntity.get(), currentSite);

            for (int pageId : pageIdList) {
                String currentUrl = pageRepository.findPathById(pageId);
                if (lisOfRelevantUrlBeforeFilter.contains(currentUrl)) {
                    lisOfRelevantUrlAfterFilter.add(currentUrl);
                }
            }
        }
        return lisOfRelevantUrlAfterFilter;
    }

    private Map<String, Double> getPathSortedByRelevancePage
            (List<String> lisOfRelevantUrl , Map<String, Integer> sortedQueryList, List<SiteEntity> siteEntityList) {

        Map<String, Integer> path2absoluteRelevance = getPath2absoluteRelevance(lisOfRelevantUrl, sortedQueryList, siteEntityList);
        double maxAbsoluteRelevance = Collections.max(path2absoluteRelevance.values());

        Map<String, Double> path2relativeRelevance = new HashMap<>();
        for (String url : lisOfRelevantUrl) {
            double relativeRelevance = path2absoluteRelevance.get(url) / maxAbsoluteRelevance;
            path2relativeRelevance.put(url, relativeRelevance);
        }
        return path2relativeRelevance.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private Map<String, Integer> getPath2absoluteRelevance
            (List<String> lisOfRelevantUrl, Map<String, Integer> sortedQueryList, List<SiteEntity> siteEntityList) {

        Map<String, Integer> path2absoluteRelevance = new HashMap<>();

            for (String url : lisOfRelevantUrl) {
                int pageId = pageRepository.findByPath(url);
                SiteEntity site = getCurrentSiteEntity(url, siteEntityList);
                int absoluteRelevance = 0;
                for (String query : sortedQueryList.keySet()) {
                    Optional<LemmaEntity> lemmaFromDB = lemmaRepository.findByLemmaAndSiteId(query, site.getId());
                    if(lemmaFromDB.isEmpty()) continue;
                    int lemmaId = lemmaFromDB.get().getId();
                    absoluteRelevance += indexRepository.findByLemmaIdAndPageId(lemmaId, pageId);
                }
                path2absoluteRelevance.put(url, absoluteRelevance);

            }

        return path2absoluteRelevance;
    }

    private SiteEntity getCurrentSiteEntity (String url, List<SiteEntity> siteEntityList) {

        return siteEntityList.stream().filter(siteEntity -> url.startsWith(siteEntity.getUrl())).collect(Collectors.toList()).get(0);

    }

    private List<SearchResult> getDemonstratedSearchResultList(List<String> lisOfRelevantUrl, int offset,
                                                               int limit, List<SearchResult> searchResultList) {

        List<SearchResult> demonstratedSearchResultList = new ArrayList<>();

        int maxCountOfResults = lisOfRelevantUrl.size();
        if (offset > maxCountOfResults) {
            offset = 0;
        }
        int finalOutputIndex = offset + limit;
        if (finalOutputIndex > maxCountOfResults) {
            finalOutputIndex = maxCountOfResults;
        }
        for (int i = offset; i < finalOutputIndex; i++) {
            demonstratedSearchResultList.add(searchResultList.get(i));
        }

        return demonstratedSearchResultList;
    }

    private Document getDoc(String path) throws IOException {
        Connection.Response response = ConnectingToLink.getConnectionToLink(path);
        return response.parse();
    }

    private String getSnippet(Set<String> queryList, String originText) throws IOException {

        ArrayList<String> snippetKeyWords = new ArrayList<>();



        ConvertingWordIntoLemma converter = ConvertingWordIntoLemma.getInstance();
        String convertedText = converter.removingHtmlTags(originText).replaceAll("\\s{2,}", " ").toLowerCase();
        HashMap<String, String> lemma2originText = converter.getListNormalForm2originForm(convertedText);

        int lengthSnippetKeywords = 0;

        for (Map.Entry<String, String> entry : lemma2originText.entrySet()) {
            if (queryList.contains(entry.getKey())) {
                String keyWord = entry.getValue();
                snippetKeyWords.add(keyWord);
                lengthSnippetKeywords += keyWord.length();
            }
        }


        int countOfIntermediateBlock = snippetKeyWords.size();
        int numberOfIntermediateCharacters = (SNIPPET_LENGTH - lengthSnippetKeywords) / countOfIntermediateBlock;
        TreeMap<String, Integer> word2index = new TreeMap<>();

        for (String snippetKeyword : snippetKeyWords) {
            //if (convertedText.contains(snippetKeyword)) {
                convertedText = convertedText.replaceAll(snippetKeyword, OPENING_TAG + snippetKeyword + CLOSING_TAG);
                word2index.put(snippetKeyword, convertedText.indexOf(snippetKeyword));
           // }
        }

        Map<String, Integer> sortedSnippetKeywords = word2index.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        int lastIndexOnPage = convertedText.length() - 1;

        StringJoiner result = new StringJoiner("...");

        for (String snippetKeyword : sortedSnippetKeywords.keySet()) {

            if (result.toString().contains(snippetKeyword)) {
                continue;
            }
            int firstIndexOfKeyWord = convertedText.indexOf(snippetKeyword) - OPENING_TAG.length();
            int lastIndexOfKeyWord = firstIndexOfKeyWord + OPENING_TAG.length() + snippetKeyword.length() + CLOSING_TAG.length();
            int calculatedFinalIndex = calculatingFinishIndex(lastIndexOfKeyWord, numberOfIntermediateCharacters, lastIndexOnPage);
            int endIndex = convertedText.indexOf(" ", calculatedFinalIndex);
            result.add(convertedText.substring(firstIndexOfKeyWord, endIndex));
        }
        return result.toString();
    }

    private int calculatingFinishIndex(int finishIndexOfSnippet, int numberOfIntermediateCharacters,
                                       int lastIndexOnPage) {
        while (finishIndexOfSnippet + numberOfIntermediateCharacters > lastIndexOnPage) {
            numberOfIntermediateCharacters--;
        }
        return finishIndexOfSnippet + numberOfIntermediateCharacters;
    }







}
