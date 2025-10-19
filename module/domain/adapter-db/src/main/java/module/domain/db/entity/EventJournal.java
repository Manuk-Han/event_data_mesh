package module.domain.db.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "event_journal")
public class EventJournal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="event_id", nullable=false)
    private Long eventId;

    @Column(name="correlation_id")
    private String correlationId;

    @Column(name="schema_version", nullable=false)
    private String schemaVersion;

    @Column(nullable=false)
    private String topic;

    @Column(name="partition_no")
    private Integer partitionNo;

    @Column(name="offset_no")
    private Long offsetNo;

    @Column(name="recorded_at", nullable=false)
    private OffsetDateTime recordedAt;
}
