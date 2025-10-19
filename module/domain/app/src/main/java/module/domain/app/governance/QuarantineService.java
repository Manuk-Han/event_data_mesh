package module.domain.app.governance;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuarantineService {
    private final JdbcTemplate jdbc;

    public void save(String dataset, String payloadJson, String reason) {
        jdbc.update("""
            insert into quarantine_event (dataset_name, payload, reason)
            values (?, CAST(? as JSON), ?)
        """, ps -> {
            ps.setString(1, dataset);
            ps.setString(2, payloadJson);
            ps.setString(3, reason);
        });
    }
}
