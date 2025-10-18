package module.domain.db.repository;

import module.domain.db.entity.Outbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    @Query("""
              select o from Outbox o
              where o.published = false
              order by o.createdAt asc
            """)
    List<Outbox> findBatch(Pageable pageable);
}
