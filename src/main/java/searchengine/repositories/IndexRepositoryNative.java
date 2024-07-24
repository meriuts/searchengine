package searchengine.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.List;

@Repository
public class IndexRepositoryNative {
    @Autowired
    EntityManager entityManager;

    @Transactional
    public void insertBatchIndex(List<IndexEntity> indexEntityList) {
        if (indexEntityList.isEmpty()) return;

        String sql = "INSERT INTO page_index (lemma_rank, lemma_id, page_id) VALUES ";
        List<String> values = new ArrayList<>();
        for (IndexEntity indexEntity : indexEntityList) {
            String value = "(" + String.join(", ",
                    indexEntity.getRank().toString(),
                    indexEntity.getLemmaId().getId().toString(),
                    indexEntity.getPageId().getId().toString()) + ")";

            values.add(value);
        }
        sql = sql + String.join(", ", values) + ";";

        Query query = entityManager.createNativeQuery(sql);
        try {
            query.executeUpdate();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
