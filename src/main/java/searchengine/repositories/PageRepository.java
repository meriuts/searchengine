package searchengine.repositories;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    @Override
//    @Cacheable(value = "page", key = "#entity.path + ':' + #entity.siteId.id")
    PageEntity save(PageEntity entity);

    @Query("SELECT p FROM PageEntity p WHERE p.path LIKE %:path% AND p.siteId = :siteId")
    Optional<PageEntity> findByPathAndSiteId(String path, SiteEntity siteId);

    @Query("SELECT p FROM PageEntity p WHERE p.path LIKE %:path% AND p.siteId = :siteId")
    PageEntity deleteByPathAndSiteId(String path, SiteEntity siteId);
    @Transactional
    @Modifying
    @Query(value = "DELETE FROM pages p WHERE p.id > -1", nativeQuery = true)
    void cleanTable();

}
