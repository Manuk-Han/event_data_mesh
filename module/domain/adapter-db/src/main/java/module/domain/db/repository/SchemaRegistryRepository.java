package module.domain.db.repository;

import java.util.Optional;

public interface SchemaRegistryRepository {
    Optional<String> findSchemaJson(String datasetName, String version);
    void insert(String datasetName, String version, String schemaJson);
    int update(String datasetName, String version, String schemaJson);
    void upsert(String datasetName, String version, String schemaJson);
}

