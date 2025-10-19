package module.domain.db.repository;

import module.domain.db.entity.EventJournal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventJournalRepository extends JpaRepository<EventJournal, Long> { }
