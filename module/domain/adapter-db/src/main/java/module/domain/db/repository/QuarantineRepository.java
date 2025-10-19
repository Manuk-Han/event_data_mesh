package module.domain.db.repository;

import java.time.OffsetDateTime;

public interface QuarantineRepository {
    void insert(String datasetName, String payloadJson, String reason, OffsetDateTime createdAt);
}
