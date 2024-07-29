package searchengine.repositories;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

@Repository
public class LemmaRepositoryNative {
    @Autowired
    EntityManager entityManager;

    public List<LemmaEntity> findLemmaForUpdate(String lemma, Integer siteId) {
        String sql = "SELECT l.* FROM lemmas l WHERE l.site_id = :siteId AND l.lemma = :lemma FOR UPDATE";
        Query query = entityManager.createNativeQuery(sql, LemmaEntity.class);
        query.setParameter("lemma", lemma);
        query.setParameter("siteId", siteId);

        return query.getResultList();
    }
}
