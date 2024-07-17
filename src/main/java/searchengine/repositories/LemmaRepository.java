package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    @Query("SELECT l FROM LemmaEntity l WHERE l.lemma = :lemma AND l.siteId = :siteId")
    Optional<LemmaEntity> findByLemmaAndSiteId(String lemma, SiteEntity siteId);
}
