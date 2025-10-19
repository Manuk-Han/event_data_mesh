package module.domain.db.repository;

import module.domain.db.entity.QuarantineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuarantineEventRepository extends JpaRepository<QuarantineEvent, Long> { }
