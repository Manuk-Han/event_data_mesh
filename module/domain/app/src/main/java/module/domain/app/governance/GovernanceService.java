package module.domain.app.governance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GovernanceService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper om; // 스프링 빈 주입

    public record Policy(String name, String ruleType, String ruleConfig) {}

    @Transactional(readOnly = true)
    public List<Policy> loadPoliciesForDataset(String datasetName) {
        String sql = """
            SELECT p.name, p.rule_type, p.rule_config
              FROM policy_binding b
              JOIN governance_policy p ON p.name = b.policy_name
             WHERE b.dataset_name = ?
        """;
        return jdbc.query(sql, (rs, i) -> new Policy(
                rs.getString("name"),
                rs.getString("rule_type"),
                rs.getString("rule_config")
        ), datasetName);
    }

    private JsonNode applyMasking(JsonNode node, String field, String strategy) {
        if (!(node instanceof ObjectNode obj)) return node;
        if (!obj.hasNonNull(field)) return node;
        if (!"EMAIL_LOCAL_PART".equals(strategy)) return node;

        String email = obj.get(field).asText();
        int at = email.indexOf('@');
        if (at <= 1) return node;

        String masked = email.charAt(0) + "****" + email.substring(at);
        obj.put(field, masked);
        return obj;
    }

    public JsonNode applyPolicies(String dataset, JsonNode payload) {
        var policies = loadPoliciesForDataset(dataset);
        JsonNode current = payload.deepCopy();
        for (var p : policies) {
            if ("MASK".equalsIgnoreCase(p.ruleType())) {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cfg = om.readValue(p.ruleConfig(), Map.class);
                    current = applyMasking(current,
                            (String) cfg.get("field"),
                            (String) cfg.get("strategy"));
                } catch (Exception ignore) {
                    // 정책 파싱 실패 → 무시하고 다음 정책 진행
                }
            }
            // TODO: VALIDATE, PII_BLOCK 등 정책 타입 확장 시 여기 추가
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
