-- governance 기본 테이블들
CREATE TABLE IF NOT EXISTS governance_policy (
                                                 id          BIGINT NOT NULL AUTO_INCREMENT,
                                                 name        VARCHAR(128) NOT NULL,
    rule_type   VARCHAR(64)  NOT NULL,
    rule_config JSON         NOT NULL,
    created_at  TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_governance_policy_name (name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS policy_binding (
                                              id           BIGINT NOT NULL AUTO_INCREMENT,
                                              dataset_name VARCHAR(128) NOT NULL,
    policy_name  VARCHAR(128) NOT NULL,
    created_at   TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_binding (dataset_name, policy_name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 격리 테이블(Quarantine)
CREATE TABLE IF NOT EXISTS quarantine_event (
                                                id           BIGINT NOT NULL AUTO_INCREMENT,
                                                dataset_name VARCHAR(255) NOT NULL,
    payload      JSON         NOT NULL,
    reason       VARCHAR(255) NOT NULL,
    created_at   TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY ix_quarantine_created (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
