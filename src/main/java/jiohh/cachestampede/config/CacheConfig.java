package jiohh.cachestampede.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jiohh.cachestampede.model.Item;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {

        // 1) Item 전용 value serializer
        Jackson2JsonRedisSerializer<Item> itemSerializer =
                new Jackson2JsonRedisSerializer<>(Item.class);

        RedisCacheConfiguration itemCacheConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(itemSerializer)
                        )
                        .entryTtl(Duration.ofSeconds(10));

        // 2) 다른 캐시들은 기본 설정 쓰고
        RedisCacheConfiguration defaultConfig =
                RedisCacheConfiguration.defaultCacheConfig();

        // 3) "item" 캐시에만 위 config 적용
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("item", itemCacheConfig) // @Cacheable(value="item") 이랑 연결
                .build();
    }

    @Bean
    public RedisTemplate<String, Item> itemRedisTemplate(RedisConnectionFactory cf, ObjectMapper om) {
        RedisTemplate<String, Item> t = new RedisTemplate<>();
        t.setConnectionFactory(cf);

        StringRedisSerializer keySer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Item> valSer = new Jackson2JsonRedisSerializer<>(om, Item.class);// JavaTimeModule 등 설정해둔 ObjectMapper

        t.setKeySerializer(keySer);
        t.setHashKeySerializer(keySer);
        t.setValueSerializer(valSer);
        t.setHashValueSerializer(valSer);
        t.afterPropertiesSet();
        return t;
    }




    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return om;
    }
}
