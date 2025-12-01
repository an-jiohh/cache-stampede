package jiohh.cachestampede.service;

import jiohh.cachestampede.model.Item;
import jiohh.cachestampede.repostiory.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;
    private final AtomicLong cacheMissCounter = new AtomicLong();
    private final RedisTemplate<String, Item> redisTemplate;
    private final CacheManager cacheManager;
    private final RedissonClient redissonClient;

    @Cacheable(value = "item", key = "#id")
    public Item getItem(Long id){
        cacheMissCounter.incrementAndGet(); //캐시 미스 발생시에만 증가
        return itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("아이템이 없습니다."));
    }

    public Item getItemByJitter(Long id){
        String key = "item::" + id;
        Item cached = (Item) redisTemplate.opsForValue().get(key);
        if(cached != null){
            return cached;
        }
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("아이템이 없습니다."));
        cacheMissCounter.incrementAndGet(); //캐시 미스 발생시에만 증가
        Duration ttl = jitter(Duration.ofSeconds(8), Duration.ofSeconds(4));
        redisTemplate.opsForValue().set(key,item,ttl);
        return item;
    }

    public Item getItemByLock(Long id){
        Cache cache = cacheManager.getCache("item");

        Item cached = cache.get(id, Item.class);
        if (cached != null){
            return cached;
        }

        String lockeKey = "lock:item:" + id;
        RLock lock = redissonClient.getLock(lockeKey);

        boolean locked = false;
        try {
            locked = lock.tryLock(500, TimeUnit.MILLISECONDS);
            if (!locked) {
                Thread.sleep(50); // 락 미획득시 조금 쉬었다가 캐시만 다시 확인
                Item fallback = cache.get(id, Item.class);
                return fallback;
            }
            Item cachedAgain = cache.get(id, Item.class);
            if (cachedAgain != null){
                return cachedAgain;
            }
            cacheMissCounter.incrementAndGet();
            Item item = itemRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("아이템이 없습니다. id=" + id));
            cache.put(id, item);
            return item;
        } catch (IllegalMonitorStateException e){
            log.warn("Lock already released");
            throw new RuntimeException(e);
        }  catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if(locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @CachePut(value = "item", key = "#result.id")
    public Item save(Item item){
        cacheMissCounter.incrementAndGet(); //강제 갱신도 실제 DB 접근 으로 카운트
        return itemRepository.save(item);
    }

    public long getCacheMissCounter() {return cacheMissCounter.get();}
    public void resetCacheMissCounter() {cacheMissCounter.set(0);}

    public static Duration jitter(Duration base, Duration maxJitter) {
        long j = ThreadLocalRandom.current().nextLong(0, maxJitter.toMillis() + 1); // 0..maxJitter
        return base.plusMillis(j);                                              // base TTL에 더해 반환
    }
}
