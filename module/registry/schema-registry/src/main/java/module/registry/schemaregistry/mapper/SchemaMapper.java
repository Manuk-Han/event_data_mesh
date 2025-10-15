package module.registry.schemaregistry.mapper;

import module.contract.catalog.dto.SchemaDto;
import module.registry.schemaregistry.entity.SchemaEntity;
import org.springframework.stereotype.Component;

@Component
public class SchemaMapper {

    public SchemaDto toDto(SchemaEntity e) {
        if (e == null) return null;
        return SchemaDto.builder()
                .name(e.getName())
                .format(e.getFormat())
                .content(e.getContent())
                .version(e.getVersion())
                .build();
    }

    public SchemaEntity toNewEntityForUpsert(SchemaDto dto, int nextVersion) {
        var e = new SchemaEntity();
        e.setName(dto.getName());
        e.setFormat(dto.getFormat());
        e.setContent(dto.getContent());
        e.setVersion(nextVersion);
        return e;
    }
}
