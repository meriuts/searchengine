package searchengine.repositories;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class LemmaRepositoryNative {

    private final EntityManager entityManager;

    public List<LemmaEntity> findLemmaForUpdate(String lemma, Integer siteId) {
        String sql = "SELECT l.* FROM lemmas l WHERE l.site_id = :siteId AND l.lemma = :lemma FOR UPDATE";
        Query query = entityManager.createNativeQuery(sql, LemmaEntity.class);
        query.setParameter("lemma", lemma);
        query.setParameter("siteId", siteId);

        return query.getResultList();
    }
}
