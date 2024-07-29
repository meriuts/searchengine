package searchengine.repositories;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    @Cacheable(value = "lemma", key = "#lemma + ':' + #siteId.id", unless = "#result == null")
    @Query("SELECT l FROM LemmaEntity l WHERE l.lemma = :lemma AND l.siteId = :siteId")
    LemmaEntity findByLemmaAndSiteId(String lemma, SiteEntity siteId);

    @Query("SELECT l FROM LemmaEntity l WHERE l.lemma IN :lemmas AND l.siteId = :siteId")
    List<LemmaEntity> findAllByLemmaInAndSiteId(Iterable<String> lemmas, SiteEntity siteId);

    @Query("SELECT l FROM LemmaEntity l WHERE l.lemma IN :lemmas")
    List<LemmaEntity> findAllByLemmaIn(Iterable<String> lemmas);

    @Transactional
    @CacheEvict(value = "lemma", allEntries = true)
    @Modifying
    @Query(value = "DELETE FROM lemmas l WHERE l.id > -1", nativeQuery = true)
    void cleanTable();
}
