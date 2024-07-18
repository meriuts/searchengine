package searchengine.repositories;

import org.apache.catalina.LifecycleState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    @Query("SELECT l FROM LemmaEntity l WHERE l.lemma = :lemma AND l.siteId = :siteId")
    Optional<LemmaEntity> findByLemmaAndSiteId(String lemma, SiteEntity siteId);

    @Query("SELECT l FROM LemmaEntity l WHERE l.lemma IN :lemmas AND l.siteId = :siteId")
    List<LemmaEntity> findAllByLemmaInAndSiteId(Iterable<String> lemmas, SiteEntity siteId);


}
