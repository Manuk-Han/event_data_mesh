package module.domain.db.repository;

import module.domain.db.entity.GenericEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface GenericEntityRepository extends GenericRepository<GenericEntity, Long> {}

