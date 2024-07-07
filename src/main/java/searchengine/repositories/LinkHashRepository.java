package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.LinkHashEntity;

@Repository
public interface LinkHashRepository extends CrudRepository<LinkHashEntity,String> {
    @Override
    boolean existsById(String link);
}
