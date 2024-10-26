package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;


public class ConvertingWordIntoLemma {

    private final LuceneMorphology luceneMorphology;
    private static final String WORD_TYPE_REGEX = "[^а-яА-яЁё\\s]";
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

    public static ConvertingWordIntoLemma getInstance() throws IOException {
        LuceneMorphology luceneMorph = new RussianLuceneMorphology();
        return new ConvertingWordIntoLemma(luceneMorph);
    }

    private ConvertingWordIntoLemma(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }

    private ConvertingWordIntoLemma() {
        throw new RuntimeException("Укажите параметр - лемматизатор");
    }

    public HashMap<String, Integer> creatingListOfLemmas2rank(String originalText) {

        HashMap<String, String> lemma2originForm = getListNormalForm2originForm(originalText);

        HashMap<String, Integer> lemma2count = new HashMap<>();


        for(String lemma : lemma2originForm.keySet()) {
            if(lemma2count.containsKey(lemma)) {
                int value = lemma2count.get(lemma) + 1;
                lemma2count.put(lemma, value);
            } else {
                lemma2count.put(lemma, 1);
            }
        }
      return lemma2count;
    }

    public HashMap<String, String> getListNormalForm2originForm(String originalText) {

        String[] words = splitTextBySpace(originalText);
        HashMap<String, String> lemma2originForm = new HashMap<>();

        for (String word : words) {

            if (word.isBlank()) continue;
            List<String> morphInfoAboutWord = luceneMorphology.getMorphInfo(word);
            if (checkingForBelongingToParticles(morphInfoAboutWord)) continue;
            List<String> normalForms = luceneMorphology.getNormalForms(word);
            if (normalForms.isEmpty()) continue;
            lemma2originForm.put(normalForms.get(0), word);
        }
        return lemma2originForm;
    }

    public String removingHtmlTags(String contentWebPage) {
        return contentWebPage.replaceAll(WORD_TYPE_REGEX, "");
    }

    private String[] splitTextBySpace(String originalText) {
        return originalText.toLowerCase(Locale.ROOT)
                .replaceAll("[^а-я\\s]", "")
                .trim()
                .split("\\s+");
    }

    private boolean checkingForBelongingToParticles(List<String> morphInfoAboutWord) {

        return morphInfoAboutWord.stream().anyMatch(word -> isParticles(word));
    }

    private boolean isParticles(String morphInfoAboutWord) {
        for (String particle : particlesNames) {
            if (morphInfoAboutWord.toUpperCase().contains(particle)) {
                return true;
            }
        }
        return false;
    }


}
