package module.domain.db.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Getter @Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "event_outbox")
public class Outbox {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String aggregateType;

    private String aggregateId;

    private String eventType;

    @Column(columnDefinition = "JSON")
    private String payload;

    @Column(columnDefinition = "JSON")
    private String headers;

    private boolean published = false;

    private OffsetDateTime createdAt = OffsetDateTime.now();
}
