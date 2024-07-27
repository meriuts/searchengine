package searchengine.config;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import searchengine.message.MessagePublisher;
import searchengine.message.RedisMessagePublisher;
import searchengine.message.RedisMessageSubscriber;
import searchengine.model.PageRedisEntity;
import searchengine.services.siteparser.PageParser;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {
    @Bean
    public RedisConnectionFactory connectionFactory(RedisProperties redisProperties) {
        LettuceConnectionFactory connectionFactory =
                new LettuceConnectionFactory(redisProperties.getHost(), redisProperties.getPort());

        return connectionFactory;
    }

    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig();
        Map<String, RedisCacheConfiguration> redisCacheConfiguration = new HashMap<>();

        redisCacheConfiguration.put("lemma", RedisCacheConfiguration.defaultCacheConfig());
        redisCacheConfiguration.put("parsedUrl", RedisCacheConfiguration.defaultCacheConfig());

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(redisCacheConfiguration)
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        final RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setValueSerializer(new GenericToStringSerializer<>(Object.class));
        return template;
    }

    @Bean
    MessageListenerAdapter messageListener(PageParser pageParser) {
        return new MessageListenerAdapter(new RedisMessageSubscriber(pageParser));
    }

    @Bean
    RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory, MessageListenerAdapter messageListener) {
        final RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListener, topic());
        return container;
    }

    @Bean
    MessagePublisher redisPublisher(RedisTemplate<String, Object> redisTemplate) {
        return new RedisMessagePublisher(redisTemplate, topic());
    }

    @Bean
    ChannelTopic topic() {
        return new ChannelTopic("pubsub:queue");
    }


//    @Bean
//    public RedisTemplate<String, PageRedisEntity> redisTemplate(RedisConnectionFactory connectionFactory) {
//        RedisTemplate<String, PageRedisEntity> redisTemplate = new RedisTemplate<>();
//        redisTemplate.setConnectionFactory(connectionFactory);
//        redisTemplate.setKeySerializer(new StringRedisSerializer());
//        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(PageRedisEntity.class));
//        redisTemplate.setEnableTransactionSupport(true);
//        return redisTemplate;
//    }
}
