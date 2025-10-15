package module.domain.app.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class JsonPayloadValidator {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonSchemaFactory FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);

    public void validate(String schemaContent, Object payload) {
        try {
            JsonNode schemaNode = MAPPER.readTree(schemaContent);
            JsonSchema schema = FACTORY.getSchema(schemaNode);
            JsonNode payloadNode = MAPPER.valueToTree(payload);
            Set<ValidationMessage> errors = schema.validate(payloadNode);
            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("Schema validation failed: " + errors);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Schema validation error: " + e.getMessage(), e);
        }
    }
}

