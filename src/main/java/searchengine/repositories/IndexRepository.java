package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;

@Repository
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM page_index i WHERE i.id > -1", nativeQuery = true)
    void cleanTable();
}
