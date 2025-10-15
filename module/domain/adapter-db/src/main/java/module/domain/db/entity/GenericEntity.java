package module.domain.db.entity;

import jakarta.persistence.*;
import lombok.*;
import module.domain.db.adapter.JsonMapConverter;

import java.util.Map;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder @Getter @Setter
@Table(name = "generic_entity")
public class GenericEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String label;

    @Column(name = "attrs_json", columnDefinition = "JSON")
    @Convert(converter = JsonMapConverter.class)
    private Map<String,Object> attrs;
}