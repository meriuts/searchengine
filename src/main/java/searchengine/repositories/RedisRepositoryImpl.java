package searchengine.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import searchengine.model.PageRedisEntity;

@Repository
public class RedisRepositoryImpl {
    private static final String KEY_PATTERN = "pages:*";
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public RedisRepositoryImpl(RedisTemplate<String, Object> redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    public void savePageRedisEntity(PageRedisEntity pageRedisEntity) {
        String key = "pages:" + pageRedisEntity.getPageRedisId().toString();
        redisTemplate.opsForValue().set(key, pageRedisEntity);
    }
    public PageRedisEntity getPage() {
        String keyPage = redisTemplate.keys(KEY_PATTERN).stream().findFirst().orElseThrow();
        PageRedisEntity page = (PageRedisEntity) redisTemplate.opsForValue().get(keyPage);
        redisTemplate.delete(keyPage);
        return page;
    }

}
