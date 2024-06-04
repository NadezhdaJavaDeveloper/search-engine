package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Transactional
public class FillingDatabasePageLemmaIndex {
    private final LemmaRepository lemmaRepository;

    private final IndexRepository indexRepository;


    public void createLemmaAndIndex(PageEntity pageEntity, SiteEntity siteEntity) throws IOException {

        HashMap<String, Integer> lemma2count = getListLemmaOnPage(pageEntity.getContent());


        for (Map.Entry<String, Integer> entry : lemma2count.entrySet()) {
            String currentLemma = entry.getKey();
            Integer countOfLemmaOnPage = entry.getValue();

            LemmaEntity lemmaEntity = createLemmaEntity(siteEntity, currentLemma);

            IndexEntity indexEntity = createIndexEntity(pageEntity, lemmaEntity, countOfLemmaOnPage);
            synchronized (indexRepository) {
                indexRepository.save(indexEntity);
            }
        }
    }
    private LemmaEntity createLemmaEntity(SiteEntity siteEntity, String lemma) {

        LemmaEntity lemmaEntity = new LemmaEntity();
        lemmaEntity.setSite(siteEntity);
        lemmaEntity.setLemma(lemma);

        int frequency = lemmaRepository.findByLemmaAndSite(lemma, siteEntity).isPresent() ?
                lemmaRepository.findByLemmaAndSite(lemma, siteEntity).get().getFrequency() : 0;
        lemmaEntity.setFrequency(frequency + 1);
        log.info(lemma);
        log.info(String.valueOf(LocalDateTime.now()));
        synchronized (lemmaRepository) {
            lemmaRepository.save(lemmaEntity);
        }

        return lemmaEntity;
    }
    private HashMap<String, Integer> getListLemmaOnPage(String content) throws IOException {

        ConvertingWordIntoLemma converter = ConvertingWordIntoLemma.getInstance();

        String convertedText = converter.removingHtmlTags(content);

        return converter.creatingListOfLemmas(convertedText);
    }
    private IndexEntity createIndexEntity(PageEntity pageEntity, LemmaEntity lemmaEntity, int countLemmaOnPage) {

        IndexEntity indexEntity = new IndexEntity();
        indexEntity.setPage(pageEntity);
        indexEntity.setLemma(lemmaEntity);
        indexEntity.setCountLemmaOnPage((float) countLemmaOnPage);
        return indexEntity;

    }
}
