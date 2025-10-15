package module.contract.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SchemaDto {
    private String name;     // ex) "catalog-product-v1"
    private String format;   // "AVRO"|"PROTOBUF"|"JSONSCHEMA"
    private String content;  // 원문 스키마
    private Integer version; // 1,2,3...
}
