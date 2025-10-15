package module.domain.core.service;


import module.domain.core.model.EntityView;
import module.domain.core.port.DataQueryPort;
import module.domain.core.port.SearchEntitiesUseCase;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class SearchEntitiesService implements SearchEntitiesUseCase {
    private final DataQueryPort port;
    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 1000;

    @Override
    public List<EntityView> searchUpdatedSince(Instant since, int limit) {
        return port.findUpdatedSince(since == null ? Instant.EPOCH : since, clamp(limit));
    }

    @Override
    public List<EntityView> searchByEquals(String key, String value, int limit) {
        if (key == null || key.isBlank() || value == null) {
            return port.findUpdatedSince(Instant.EPOCH, clamp(limit));
        }
        return port.findByEquals(key, value, clamp(limit));
    }

    @Override
    public List<EntityView> searchByAll(Map<String, String> filters, int limit) {
        if (filters == null || filters.isEmpty()) {
            return port.findUpdatedSince(Instant.EPOCH, clamp(limit));
        }
        return port.findByAll(filters, clamp(limit));
    }

    private int clamp(int limit) {
        if (limit <= 0) return DEFAULT_LIMIT;
        return Math.min(limit, MAX_LIMIT);
    }
}