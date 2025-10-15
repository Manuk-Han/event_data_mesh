package module.domain.core.port;

import module.domain.core.model.EntityView;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface SearchEntitiesUseCase {
    List<EntityView> searchUpdatedSince(Instant since, int limit);
    List<EntityView> searchByEquals(String key, String value, int limit);
    List<EntityView> searchByAll(Map<String, String> filters, int limit);
}