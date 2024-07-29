package searchengine.repositories;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Set;

@Repository
public class LemmaRepositoryNative {
    @Autowired
    EntityManager entityManager;
//    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public List<LemmaEntity> findAllByLemmaInAndSiteIdForUpdate(Set<String> lemmaList, Integer siteId) {
        String sql = "SELECT l.* FROM lemmas l WHERE l.site_id = :siteId AND l.lemma IN :lemmaList FOR UPDATE";
        Query query = entityManager.createNativeQuery(sql, LemmaEntity.class);
        query.setParameter("lemmaList", lemmaList);
        query.setParameter("siteId", siteId);

        return query.getResultList();
    }
    public List<LemmaEntity> findLemmaForUpdate(String lemma, Integer siteId) {
        String sql = "SELECT l.* FROM lemmas l WHERE l.site_id = :siteId AND l.lemma = :lemma FOR UPDATE";
        Query query = entityManager.createNativeQuery(sql, LemmaEntity.class);
        query.setParameter("lemma", lemma);
        query.setParameter("siteId", siteId);

        return query.getResultList();
    }


}
