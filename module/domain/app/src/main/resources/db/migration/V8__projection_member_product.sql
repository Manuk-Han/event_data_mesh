-- V8__projection_member_product.sql
CREATE TABLE IF NOT EXISTS member_product (
                                              member_id CHAR(36) NOT NULL PRIMARY KEY,
    email     VARCHAR(255) NOT NULL,
    name      VARCHAR(255) NOT NULL,
    updated_at TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 멱등성: 소비 기록(토픽/파티션/오프셋 또는 eventId 사용)
CREATE TABLE IF NOT EXISTS consumer_checkpoint (
                                                   topic       VARCHAR(255) NOT NULL,
    partition_no INT NOT NULL,
    offset_no    BIGINT NOT NULL,
    PRIMARY KEY (topic, partition_no, offset_no)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
