package searchengine.services.search;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import searchengine.dto.search.SearchResponse;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.services.contentparser.LemmaFinder;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService{
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    @Override
    public SearchResponse search(String query, String site, String offset, String limit) {
        Map<String, Integer> lemmas = LemmaFinder.getInstance().collectLemmas(query);
        List<LemmaEntity> lemmaEntityList = lemmaRepository.findAllByLemmaIn(lemmas.keySet());
        Collections.sort(lemmaEntityList, Comparator.comparing(LemmaEntity::getFrequency));

        List<IndexEntity> indexEntityList = indexRepository.findAllByLemmaId(
                lemmaEntityList.stream().findFirst().orElse(new LemmaEntity(-1)).getId());

        for (LemmaEntity lemmaEntity : lemmaEntityList) {
            indexEntityList = indexEntityList.stream()
                    .filter(indexEntity -> lemmaEntity.getId().equals(indexEntity.getLemmaId().getId()))
                    .collect(Collectors.toList());
        }

        if (indexEntityList.isEmpty()) {
            return new SearchResponse(true, 0, new ArrayList<>(), null);
        }

        return null;
    }
}
