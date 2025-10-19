package module.domain.db.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConsumerCheckpointRepository {
    private final JdbcTemplate jdbc;

    public boolean exists(String topic, int partition, long offset) {
        Integer c = jdbc.queryForObject(
                "SELECT COUNT(*) FROM consumer_checkpoint WHERE topic=? AND partition_no=? AND offset_no=?",
                Integer.class, topic, partition, offset);
        return c != null && c > 0;
    }

    public void save(String topic, int partition, long offset) {
        jdbc.update("""
      INSERT INTO consumer_checkpoint(topic, partition_no, offset_no)
      VALUES (?,?,?)
    """, topic, partition, offset);
    }
}
