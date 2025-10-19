package module.domain.db.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import module.domain.db.repository.QuarantineRepository;

import java.time.OffsetDateTime;

@Repository
@RequiredArgsConstructor
public class QuarantineJdbcRepository implements QuarantineRepository {

    private final JdbcTemplate jdbc;

    @Override
    public void insert(String datasetName, String payloadJson, String reason, OffsetDateTime createdAt) {
        jdbc.update("""
            INSERT INTO quarantine_event (dataset_name, payload, reason, created_at)
            VALUES (?, CAST(? AS JSON), ?, ?)
        """, ps -> {
            ps.setString(1, datasetName);
            ps.setString(2, payloadJson);
            ps.setString(3, reason);
            ps.setTimestamp(4, java.sql.Timestamp.from(createdAt.toInstant()));
        });
    }
}
