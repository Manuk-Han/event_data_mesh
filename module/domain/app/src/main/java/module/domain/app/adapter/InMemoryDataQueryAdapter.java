package module.domain.app.adapter;

import module.domain.core.model.EntityView;
import module.domain.core.port.DataQueryPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Profile("memory")
@Component
public class InMemoryDataQueryAdapter implements DataQueryPort {
    private final List<EntityView> store = new ArrayList<>();

    public InMemoryDataQueryAdapter() {
        store.add(EntityView.builder()
                .id(1L)
                .name("E-001")
                .label("Alpha")
                .attr("type", "demo")
                .build());

        store.add(EntityView.builder()
                .id(2L)
                .name("E-002")
                .label("Beta")
                .attr("type", "demo")
                .attr("team", "core")
                .build());

        store.add(EntityView.builder()
                .id(3L)
                .name("E-003")
                .label("Gamma")
                .attr("team", "platform")
                .build());
    }

    @Override
    public List<EntityView> findUpdatedSince(Instant since, int limit) {
        return store.stream().limit(Math.max(1, limit)).toList();
    }

    @Override
    public List<EntityView> findByEquals(String key, String value, int limit) {
        return store.stream()
                .filter(v -> v.getAttrs() != null && value.equals(String.valueOf(v.getAttrs().get(key))))
                .limit(Math.max(1, limit))
                .toList();
    }

    @Override
    public List<EntityView> findByAll(Map<String, String> filters, int limit) {
        return store.stream()
                .filter(v -> {
                    if (filters == null || filters.isEmpty()) return true;
                    if (v.getAttrs() == null) return false;
                    for (var e : filters.entrySet()) {
                        if (!e.getValue().equals(String.valueOf(v.getAttrs().get(e.getKey())))) return false;
                    }
                    return true;
                })
                .limit(Math.max(1, limit))
                .toList();
    }
}
