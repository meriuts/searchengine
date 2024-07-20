package searchengine.repositories;


import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Set;

@Repository
public class LemmaRepositoryNative {
    @PersistenceContext
    EntityManager entityManager;
    public List<LemmaEntity> findAllByLemmaInAndSiteIdForUpdate(Set<String> lemmaList, Integer siteId) {
        String sql = "SELECT l.* FROM lemmas l WHERE l.site_id = :siteId AND l.lemma IN :lemmaList FOR UPDATE";
        Query query = entityManager.createNativeQuery(sql, LemmaEntity.class);
        query.setParameter("lemmaList", lemmaList);
        query.setParameter("siteId", siteId);

        return query.getResultList();
    }

}
