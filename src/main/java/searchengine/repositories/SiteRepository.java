package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteEntity;

@Repository
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    @Query("SELECT s FROM SiteEntity s WHERE s.url LIKE %:host%")
    SiteEntity findByUrl(String host);

}
