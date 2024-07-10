package searchengine.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("myCache");
    }
}

//    @Bean
//    public CacheManager redisCacheManager(LettuceConnectionFactory lettuceConnectionFactory) {
//        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig();
//
//        return RedisCacheManager.builder(lettuceConnectionFactory)
//                .cacheDefaults(defaultConfig)
//                .build();
//    }

