package module.domain.app.governance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GovernanceService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper om = new ObjectMapper();

    public record Policy(String name, String ruleType, String ruleConfig) {}

    public List<Policy> loadPoliciesForDataset(String datasetName) {
        String sql = """
            select p.name, p.rule_type, p.rule_config
            from policy_binding b
            join governance_policy p on p.name = b.policy_name
            where b.dataset_name = ?
        """;
        return jdbc.query(sql, (rs, i) -> new Policy(
                rs.getString("name"),
                rs.getString("rule_type"),
                rs.getString("rule_config")
        ), datasetName);
    }

    public JsonNode applyMasking(JsonNode node, String field, String strategy) {
        if (!node.has(field) || node.get(field).isNull()) return node;
        if (!"EMAIL_LOCAL_PART".equals(strategy)) return node;

        String email = node.get(field).asText();
        int at = email.indexOf('@');
        if (at <= 1) return node;
        String masked = email.charAt(0) + "****" + email.substring(at);
        ((com.fasterxml.jackson.databind.node.ObjectNode) node).put(field, masked);
        return node;
    }

    public JsonNode applyPolicies(String dataset, JsonNode payload) {
        var policies = loadPoliciesForDataset(dataset);
        JsonNode current = payload.deepCopy();
        for (var p : policies) {
            if ("MASK".equalsIgnoreCase(p.ruleType())) {
                try {
                    var cfg = om.readValue(p.ruleConfig(), Map.class);
                    current = applyMasking(current, (String) cfg.get("field"), (String) cfg.get("strategy"));
                } catch (Exception ignore) {}
            }
            // 필요시 VALIDATE, PII_BLOCK 등 확대
        }
        return current;
    }

    public boolean violatesRequired(JsonNode node, String... fields) {
        for (String f : fields) {
            if (!node.hasNonNull(f) || node.get(f).asText().isBlank()) return true;
        }
        return false;
    }
}
