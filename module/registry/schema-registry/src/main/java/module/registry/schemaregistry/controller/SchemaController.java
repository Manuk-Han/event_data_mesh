package module.registry.schemaregistry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import module.contract.catalog.dto.SchemaDto;
import module.registry.schemaregistry.dto.SchemaUpsertRequest;
import module.registry.schemaregistry.entity.SchemaEntity;
import module.registry.schemaregistry.mapper.SchemaMapper;
import module.registry.schemaregistry.repository.SchemaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/catalog/schema")
@RequiredArgsConstructor
public class SchemaController {
    private final SchemaRepository schemaRepository;
    private final SchemaMapper schemaMapper;
    private final ObjectMapper objectMapper;

    @GetMapping("/{name}")
    public ResponseEntity<?> get(@PathVariable String name){
        return schemaRepository.findTopByNameOrderByVersionDesc(name)
                .map(schemaMapper::toDto)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public SchemaDto upsert(@Validated @RequestBody SchemaUpsertRequest req) throws Exception {
        String contentStr = (req.getContent() instanceof String)
                ? (String) req.getContent()
                : objectMapper.writeValueAsString(req.getContent());

        objectMapper.readTree(contentStr);

        int nextVersion = schemaRepository.findTopByNameOrderByVersionDesc(req.getName())
                .map(SchemaEntity::getVersion).orElse(0) + 1;

        SchemaEntity e = new SchemaEntity();
        e.setId(null);
        e.setName(req.getName());
        e.setFormat(req.getFormat());
        e.setContent(contentStr);
        e.setVersion(nextVersion);

        SchemaEntity saved = schemaRepository.save(e);
        return toDto(saved);
    }

    private SchemaDto toDto(SchemaEntity e) {
        return SchemaDto.builder()
                .name(e.getName())
                .format(e.getFormat())
                .content(e.getContent())
                .version(e.getVersion())
                .build();
    }
}
