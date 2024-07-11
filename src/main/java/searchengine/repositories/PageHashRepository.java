package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageHashEntity;


@Repository
public interface PageHashRepository extends CrudRepository<PageHashEntity, String> {

}
