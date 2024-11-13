package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
@Transactional
public class FillingDatabaseLemmaIndex {
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    public void createLemmaAndIndex(PageEntity pageEntity, SiteEntity siteEntity) throws IOException {

        HashMap<String, Integer> lemma2rank = getListLemmaOnPage2rank(pageEntity.getContent());


        for (Map.Entry<String, Integer> entry : lemma2rank.entrySet()) {
            String currentLemma = entry.getKey();
            Integer rank = entry.getValue();

            LemmaEntity lemmaEntity;
            synchronized (lemmaRepository) {
                Optional<LemmaEntity> lemmaFromDB = lemmaRepository.findByLemmaAndSiteId(currentLemma, siteEntity.getId());
                if (lemmaFromDB.isPresent()) {
                    lemmaEntity = lemmaFromDB.get();
                    int frequency = lemmaEntity.getFrequency();
                    lemmaEntity.setFrequency(frequency + 1);
                    lemmaRepository.save(lemmaEntity);
                } else {
                    lemmaEntity = createLemmaEntity(siteEntity, currentLemma);
                    lemmaRepository.save(lemmaEntity);
                }
            }
            IndexEntity indexEntity = createIndexEntity(pageEntity, lemmaEntity, rank);
            synchronized (indexRepository) {
                indexRepository.save(indexEntity);
            }
        }
    }

    private LemmaEntity createLemmaEntity(SiteEntity siteEntity, String lemma) {
        LemmaEntity lemmaEntity = new LemmaEntity();
        lemmaEntity.setSite(siteEntity);
        lemmaEntity.setLemma(lemma);
        lemmaEntity.setFrequency(1);
        return lemmaEntity;
    }

    private IndexEntity createIndexEntity(PageEntity pageEntity, LemmaEntity lemmaEntity, int countLemmaOnPage) {

        IndexEntity indexEntity = new IndexEntity();
        indexEntity.setPage(pageEntity);
        indexEntity.setLemma(lemmaEntity);
        indexEntity.setCountLemmaOnPage((float) countLemmaOnPage);
        return indexEntity;

    }

    private HashMap<String, Integer> getListLemmaOnPage2rank(String content) throws IOException {

        ConvertingWordIntoLemma converter = ConvertingWordIntoLemma.getInstance();

        String convertedText = converter.removingHtmlTags(content);

        return converter.creatingListOfLemmas2rank(convertedText);
    }


}
