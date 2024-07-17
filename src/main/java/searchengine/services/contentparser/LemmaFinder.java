package searchengine.services.contentparser;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.*;


public class LemmaFinder {

    private final LuceneMorphology luceneMorphology;
    private static final Set<String> particlesNames = new HashSet<>(Set.of("МЕЖД", "ПРЕДЛ", "СОЮЗ"));

    private LemmaFinder(LuceneMorphology luceneMorphology) {
        this.luceneMorphology = luceneMorphology;
    }
    public static LemmaFinder getInstance() {
        LuceneMorphology morphology;
        try {
            morphology = new RussianLuceneMorphology();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new LemmaFinder(morphology);
    }

    public Map<String, Integer> collectLemmas(String text) {
        HashMap<String, Integer> lemmas = new HashMap<>();
        List<String> normalWords = getWords(text).stream()
                .filter(word -> !word.isBlank())
                .filter(word -> !isParticle(word))
                .map(luceneMorphology::getNormalForms)
                .flatMap(Collection :: stream).toList();
        for (String normalWord : normalWords) {
            if(lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(normalWord) + 1);
                continue;
            }
            lemmas.put(normalWord, 1);
        }
        return lemmas;
    }

    private List<String> getWords(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT)
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+")).toList();
    }

    private boolean isParticle(String word) {
        List<String> morphInfo = luceneMorphology.getMorphInfo(word);
        for (String property : particlesNames) {
            for (String info : morphInfo) {
                if (info.toUpperCase().contains(property)) {
                    return true;
                }
            }
        }
        return false;
    }
}
