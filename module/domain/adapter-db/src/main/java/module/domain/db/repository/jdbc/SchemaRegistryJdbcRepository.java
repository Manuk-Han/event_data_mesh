package module.domain.db.repository.jdbc;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import module.domain.db.repository.SchemaRegistryRepository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SchemaRegistryJdbcRepository implements SchemaRegistryRepository {
    private final JdbcTemplate jdbc;

    @Override
    public Optional<String> findSchemaJson(String datasetName, String version) {
        var sql = "SELECT JSON_PRETTY(schema_json) AS schema_json " +
                "FROM schema_registry WHERE dataset_name=? AND version=?";
        var list = jdbc.query(sql, (rs, i) -> rs.getString("schema_json"),
                datasetName, version);
        return list.stream().findFirst();
    }

    @Override
    public void insert(String datasetName, String version, String schemaJson) {
        jdbc.update("""
            INSERT INTO schema_registry (dataset_name, version, schema_json)
            VALUES (?, ?, CAST(? AS JSON))
        """, datasetName, version, schemaJson);
    }

    @Override
    public int update(String datasetName, String version, String schemaJson) {
        return jdbc.update("""
            UPDATE schema_registry
               SET schema_json = CAST(? AS JSON)
             WHERE dataset_name = ? AND version = ?
        """, schemaJson, datasetName, version);
    }

    @Override
    public void upsert(String datasetName, String version, String schemaJson) {
        jdbc.update("""
            INSERT INTO schema_registry (dataset_name, version, schema_json)
            VALUES (?, ?, CAST(? AS JSON)) AS t
            ON DUPLICATE KEY UPDATE schema_json = t.schema_json
        """, datasetName, version, schemaJson);
    }
}

