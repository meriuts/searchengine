package searchengine.repositories;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {

    @Cacheable(value = "path", key = "#path + ':' + #siteId.id", unless = "#result == null")
    @Query("SELECT p FROM PageEntity p WHERE p.path LIKE %:path% AND p.siteId = :siteId")
    PageEntity findByPathAndSiteId(String path, SiteEntity siteId);

    @Query(value = "SELECT count(p.id) FROM pages p WHERE p.site_id = :siteId", nativeQuery = true)
    Integer getAmountPageBySiteId(Integer siteId);

    @Transactional
    @CacheEvict(cacheNames = {"parsedUrl", "path"}, allEntries = true)
    @Modifying
    @Query(value = "DELETE FROM pages p WHERE p.id > -1", nativeQuery = true)
    void cleanTable();

}
