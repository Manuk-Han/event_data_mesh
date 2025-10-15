package module.registry.schemaregistry.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(
        name = "schema_registry",
        indexes = {
                @Index(name = "ix_schema_name", columnList = "name")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_schema_name_version", columnNames = {"name","version"})
        }
)
@Getter @Setter
public class SchemaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 50)
    private String format;     // AVRO / PROTOBUF / JSONSCHEMA

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    @Column(nullable = false)
    private Integer version;

    @CreationTimestamp
    private Instant createdAt;
}

