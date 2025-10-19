package module.domain.app.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import module.domain.db.repository.QuarantineRepository;
import module.domain.db.repository.SchemaRegistryRepository;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EventGovernance {

    private final SchemaRegistryRepository schemaRepo;
    private final QuarantineRepository quarantineRepo;
    private final ObjectMapper om;

    public String validateOrQuarantine(String dataset, String schemaVersion, String payloadJson) {
        schemaRepo.findSchemaJson(dataset, schemaVersion)
                .orElseThrow(() -> new IllegalStateException("no schema: " + dataset + "/" + schemaVersion));

        try {
            JsonNode p = om.readTree(payloadJson);

            List<String> required = List.of("memberId", "email", "name");
            for (String f : required) {
                if (!p.hasNonNull(f) || p.get(f).asText().isBlank()) {
                    quarantineRepo.insert(dataset, payloadJson, "REQUIRED_FIELD_MISSING", OffsetDateTime.now());
                    throw new IllegalStateException("required field missing: " + f);
                }
            }

            if (p instanceof ObjectNode obj && obj.hasNonNull("email")) {
                String email = obj.get("email").asText();
                int at = email.indexOf('@');
                if (at > 1) {
                    String masked = email.charAt(0) + "****" + email.substring(at);
                    obj.put("email", masked);
                }
            }

            return om.writeValueAsString(p);

        } catch (IllegalStateException rethrow) {
            throw rethrow;
        } catch (Exception e) {
            quarantineRepo.insert(dataset, payloadJson, "INVALID_JSON", OffsetDateTime.now());
            throw new IllegalStateException("invalid json", e);
        }
    }
}
