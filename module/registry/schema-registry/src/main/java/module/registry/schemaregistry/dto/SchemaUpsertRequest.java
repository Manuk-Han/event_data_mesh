package module.registry.schemaregistry.dto;

import lombok.Data;

@Data
public class SchemaUpsertRequest {
    private String name;
    private String format;
    private Object content;
}
