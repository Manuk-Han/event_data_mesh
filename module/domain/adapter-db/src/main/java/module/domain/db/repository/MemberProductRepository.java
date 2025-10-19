package module.domain.db.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberProductRepository {
    private final JdbcTemplate jdbc;

    public void upsert(String id, String email, String name) {
        jdbc.update("""
      INSERT INTO member_product(member_id,email,name)
      VALUES (?,?,?)
      ON DUPLICATE KEY UPDATE email=VALUES(email), name=VALUES(name)
    """, id, email, name);
    }
}
