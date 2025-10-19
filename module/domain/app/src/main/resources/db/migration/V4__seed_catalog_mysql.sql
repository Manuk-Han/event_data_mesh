-- 카탈로그 기본 테이블들 생성
CREATE TABLE IF NOT EXISTS catalog_dataset (
                                               id         BIGINT NOT NULL AUTO_INCREMENT,
                                               name       VARCHAR(128) NOT NULL,
    domain     VARCHAR(128) NOT NULL,
    type       VARCHAR(32)  NOT NULL,  -- TABLE / STREAM 등
    created_at TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_catalog_dataset_name (name)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS schema_registry (
                                               id            BIGINT NOT NULL AUTO_INCREMENT,
                                               dataset_name  VARCHAR(128) NOT NULL,
    version       VARCHAR(32)  NOT NULL,
    schema_json   JSON         NOT NULL,
    created_at    TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_schema_name_ver (dataset_name, version)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
