package module.domain.db.adapter;

import lombok.RequiredArgsConstructor;
import module.domain.core.model.EntityView;
import module.domain.core.port.DataCommandPort;
import module.domain.db.entity.GenericEntity;
import module.domain.db.repository.GenericEntityRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@Profile("db")
@RequiredArgsConstructor
public class JpaDataCommandAdapter implements DataCommandPort {

    private final GenericEntityRepository repo;

    @Override
    @Transactional
    public EntityView create(String name, String label, Map<String,Object> attrs) {
        GenericEntity genericEntity = GenericEntity.builder()
                .name(name)
                .label(label)
                .attrs(attrs)
                .build();
        genericEntity = repo.save(genericEntity);

        return EntityView.builder()
                .id(genericEntity.getId())
                .name(genericEntity.getName())
                .label(genericEntity.getLabel())
                .attrs(genericEntity.getAttrs())
                .build();
    }
}
