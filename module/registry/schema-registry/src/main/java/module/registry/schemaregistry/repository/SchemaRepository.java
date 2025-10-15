package module.registry.schemaregistry.repository;


import module.registry.schemaregistry.entity.SchemaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchemaRepository extends JpaRepository<SchemaEntity, Long> {
    Optional<SchemaEntity> findTopByNameOrderByVersionDesc(String name);
}
