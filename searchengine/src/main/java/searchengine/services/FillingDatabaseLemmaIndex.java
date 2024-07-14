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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
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

        HashMap<String, Integer> lemma2count = getListLemmaOnPage(pageEntity.getContent());

        for (Map.Entry<String, Integer> entry : lemma2count.entrySet()) {
            String currentLemma = entry.getKey();
            Integer countOfLemmaOnPage = entry.getValue();

            LemmaEntity lemmaEntity;
            synchronized (lemmaRepository) {
                Optional<LemmaEntity> lemmaFromDB = lemmaRepository.findByLemmaAndSite(currentLemma, siteEntity);
                if (lemmaFromDB.isPresent()) {
                    lemmaEntity = lemmaFromDB.get();
                    int frequency = lemmaEntity.getFrequency();
                    lemmaEntity.setFrequency(frequency + 1);
                    lemmaRepository.save(lemmaEntity);
                } else {
                    lemmaEntity = createLemmaEntity(siteEntity, currentLemma);
                    synchronized (lemmaRepository) {
                        lemmaRepository.save(lemmaEntity);
                    }
                }
            }
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
    private HashMap<String, Integer> getListLemmaOnPage(String content) throws IOException {

        ConvertingWordIntoLemma converter = ConvertingWordIntoLemma.getInstance();

        String convertedText = converter.removingHtmlTags(content);


        return converter.creatingListOfLemmas(convertedText);
    }

}
