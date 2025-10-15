package module.domain.db.adapter;

import lombok.RequiredArgsConstructor;
import module.domain.core.model.EntityView;
import module.domain.core.port.DataQueryPort;
import module.domain.db.entity.GenericEntity;
import module.domain.db.repository.GenericEntityRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@Profile("db")
@RequiredArgsConstructor
public class JpaDataQueryAdapter implements DataQueryPort {
    private final GenericEntityRepository genericRepository;

    @Override
    public List<EntityView> findUpdatedSince(Instant since, int limit) {
        return genericRepository.findAll(PageRequest.of(0, Math.max(1, limit)))
                .stream().map(this::toView).toList();
    }

    @Override
    public List<EntityView> findByEquals(String key, String value, int limit) {
        if ("label".equals(key)) {
            var page = genericRepository.findByLabel(value, PageRequest.of(0, Math.max(1, limit)));
            return page.getContent().stream().map(this::toView).toList();
        }
        return findUpdatedSince(Instant.EPOCH, limit);
    }

    @Override
    public List<EntityView> findByAll(Map<String, String> filters, int limit) {
        if (filters == null || filters.isEmpty()) return findUpdatedSince(Instant.EPOCH, limit);
        if (filters.size()==1) {
            var e = filters.entrySet().iterator().next();
            return findByEquals(e.getKey(), e.getValue(), limit);
        }
        return findUpdatedSince(Instant.EPOCH, limit);
    }

    private EntityView toView(GenericEntity e) {
        return EntityView.builder()
                .id(e.getId())
                .name(e.getName())
                .label(e.getLabel())
                .attrs(e.getAttrs())
                .build();
    }
}
