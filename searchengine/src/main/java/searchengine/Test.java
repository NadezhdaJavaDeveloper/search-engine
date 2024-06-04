package searchengine;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import searchengine.services.ConvertingWordIntoLemma;

import javax.persistence.Convert;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test {
    public static void main(String[] args) {

        try {
            ConvertingWordIntoLemma convertingWordIntoLemma = ConvertingWordIntoLemma.getInstance();
            String text = "Повторное появление леопарда в Осетии позволяет предположить, что леопард постоянно обитает в некоторых районах Северного Кавказа.";
            HashMap<String, Integer> result = convertingWordIntoLemma.creatingListOfLemmas(text);
            for(Map.Entry<String, Integer> entry : result.entrySet()) {
                System.out.printf("%s - %s%n", entry.getKey(), entry.getValue());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
