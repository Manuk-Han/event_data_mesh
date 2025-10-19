package module.domain.db.repository;

import module.domain.db.entity.Outbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<Outbox, Long> {
    @Query(
            value = """
        SELECT * FROM event_outbox
        WHERE published = false
        ORDER BY created_at
        LIMIT :#{#pageable.pageSize}
        FOR UPDATE SKIP LOCKED
      """,
            nativeQuery = true)
    List<Outbox> findBatch(Pageable pageable);

    long countByPublishedFalse();

}
