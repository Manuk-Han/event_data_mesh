-- 정책
INSERT INTO governance_policy (name, rule_type, rule_config)
VALUES ('mask-email', 'MASK', JSON_OBJECT('field','email','strategy','EMAIL_LOCAL_PART')) AS t
ON DUPLICATE KEY UPDATE rule_type = t.rule_type, rule_config = t.rule_config;

-- 바인딩
INSERT INTO policy_binding (dataset_name, policy_name)
VALUES ('member-events','mask-email') AS t
ON DUPLICATE KEY UPDATE policy_name = t.policy_name;
