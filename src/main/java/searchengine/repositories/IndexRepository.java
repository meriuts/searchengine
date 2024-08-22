package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;

import java.util.List;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    @Query(value = "SELECT i.* FROM page_index i WHERE i.lemma_id = :lemmaId", nativeQuery = true)
    List<IndexEntity> findAllByLemmaId(Integer lemmaId);

    @Query(value = "SELECT count(i.lemma_rank) FROM page_index i WHERE i.page_id = :pageId", nativeQuery = true)
    Integer countAbsRelevant(Integer pageId);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM page_index i WHERE i.id > -1", nativeQuery = true)
    void cleanTable();
}
