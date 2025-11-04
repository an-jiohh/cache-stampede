package jiohh.cachestampede.repostiory;

import jiohh.cachestampede.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {
}
