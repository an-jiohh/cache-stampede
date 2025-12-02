package jiohh.cachestampede;

import jiohh.cachestampede.service.ItemService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
public class CacheStampedTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ItemService itemService;

    List<Long> targetId = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        Long target_size = 101L;
        for(Long target = 1L; target<target_size; target++){
            targetId.add(target);
            mockMvc.perform(get("/item/{id}", target))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(get("/item/reset"))
                .andExpect(status().isOk());
    }

    @Test
    void TTL_만료시_횟수_조회() throws Exception{
        Thread.sleep(12000);
        
        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (Long id:targetId) {
            tasks.add(() -> {
                mockMvc.perform(get("/item/{id}", id))
                        .andExpect(status().isOk());
                return null;
            });
        }
        List<Future<Void>> futures = pool.invokeAll(tasks);
        for (Future<Void> f : futures) f.get();
        pool.shutdown();

        long cacheMissCounter = itemService.getCacheMissCounter();
        log.info("hot key count : {}", cacheMissCounter);
        Assertions.assertThat(cacheMissCounter).isNotEqualTo(1);
    }


    @Test
    void 캐시_스탬피드_지속_재현() throws Exception {
        int requestsPerKey = 1;          // 키당 동시 스레드 수
        int durationSeconds = 60;         // 총 테스트 시간 (10초 동안 실행)
        int totalThreads = targetId.size() * requestsPerKey;

        ExecutorService pool = Executors.newFixedThreadPool(totalThreads);

        CountDownLatch ready = new CountDownLatch(totalThreads);
        CountDownLatch start = new CountDownLatch(1);

        for (Long id : targetId) {
            for (int i = 0; i < requestsPerKey; i++) {
                pool.submit(() -> {
                    try {
                        ready.countDown();
                        start.await();

                        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

                        while (System.currentTimeMillis() < endTime) {
                            try {
                                mockMvc.perform(get("/item/{id}", id))
                                        .andExpect(status().isOk());

                                // 1초 대기
                                Thread.sleep(100);
                            } catch (Exception e) {
                                log.error("요청 중 에러", e);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }

        // 동시에 시작
        ready.await();
        start.countDown();

        pool.shutdown();
        pool.awaitTermination(durationSeconds + 5, TimeUnit.SECONDS);

        long cacheMissCounter = itemService.getCacheMissCounter();
        log.info("cache stampede count : {}", cacheMissCounter);

        Assertions.assertThat(cacheMissCounter).isGreaterThan(0);
    }

    @Test
    void 캐시_스탬프드_지속_재현_Jitter() throws Exception {
        int requestsPerKey = 1;          // 키당 동시 스레드 수
        int durationSeconds = 60;         // 총 테스트 시간 (10초 동안 실행)
        int totalThreads = targetId.size() * requestsPerKey;

        ExecutorService pool = Executors.newFixedThreadPool(totalThreads);

        CountDownLatch ready = new CountDownLatch(totalThreads);
        CountDownLatch start = new CountDownLatch(1);

        for (Long id : targetId) {
            for (int i = 0; i < requestsPerKey; i++) {
                pool.submit(() -> {
                    try {
                        ready.countDown();
                        start.await();

                        long endTime = System.currentTimeMillis() + (durationSeconds * 1000);

                        while (System.currentTimeMillis() < endTime) {
                            try {
                                mockMvc.perform(get("/item/jitter/{id}", id))
                                        .andExpect(status().isOk());

                                // 1초 대기
                                Thread.sleep(100);
                            } catch (Exception e) {
                                log.error("요청 중 에러", e);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }

        // 동시에 시작
        ready.await();
        start.countDown();

        pool.shutdown();
        pool.awaitTermination(durationSeconds + 5, TimeUnit.SECONDS);

        long cacheMissCounter = itemService.getCacheMissCounter();
        log.info("cache stampede count : {}", cacheMissCounter);

        Assertions.assertThat(cacheMissCounter).isGreaterThan(0);
    }
}
