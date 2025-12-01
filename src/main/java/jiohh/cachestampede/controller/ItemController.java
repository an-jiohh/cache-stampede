package jiohh.cachestampede.controller;

import jiohh.cachestampede.model.Item;
import jiohh.cachestampede.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequiredArgsConstructor
@RequestMapping("/item")
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/{id}")
    public Item getItem(@PathVariable Long id){
        return itemService.getItem(id);
    }

    @GetMapping("/jitter/{id}")
    public Item getItemJitter(@PathVariable Long id){
        return itemService.getItemByJitter(id);
    }

    @GetMapping("/hotSafe/{id}")
    public Item getItemHotKey(@PathVariable Long id) {return itemService.getItemByLock(id);}

    @GetMapping("/stats")
    public Map<String, Object> stats(){
        return Map.of("hits", itemService.getCacheMissCounter(),"ts", System.currentTimeMillis());
    }

    @GetMapping("/reset")
    public String reset() {
        itemService.resetCacheMissCounter();
        return "ok";
    }
}
