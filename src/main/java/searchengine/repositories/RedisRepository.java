package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageRedisEntity;

@Repository
public interface RedisRepository extends CrudRepository<PageRedisEntity, String> {

}
