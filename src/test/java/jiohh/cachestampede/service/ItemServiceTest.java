package jiohh.cachestampede.service;

import jiohh.cachestampede.model.Item;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ItemServiceTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ItemService itemService;

    List<Long> targetId = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
//        for(Long target = 1L; target<101; target++){
//            targetId.add(target);
//            mockMvc.perform(get("/item/{id}", target))
//                    .andExpect(status().isOk());
//        }
        mockMvc.perform(get("/item/reset"))
                .andExpect(status().isOk());
    }

    @Test
    void 캐시_기능_테스트() {
        Long target = 1L;
        Item item1 = itemService.getItemByJitter(1L);
        Item item2 = itemService.getItemByJitter(1L);
        long cacheMissCounter = itemService.getCacheMissCounter();
        Assertions.assertThat(cacheMissCounter).isEqualTo(1);
    }

}