package searchengine.repositories;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    @Override
    @Cacheable(value = "page", key = "#entity.path + ':' + #entity.siteId.id")
    PageEntity save(PageEntity entity);

    @Query("SELECT p FROM PageEntity p WHERE p.path LIKE %:path% AND p.siteId = :siteId")
    Optional<PageEntity> findByPathAndSiteId(String path, SiteEntity siteId);
}
