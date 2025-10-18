CREATE TABLE IF NOT EXISTS `event_outbox` (
                                              `id` BIGINT NOT NULL AUTO_INCREMENT,
                                              `aggregate_type` VARCHAR(255) NOT NULL,
    `aggregate_id`   VARCHAR(255) NOT NULL,
    `event_type`     VARCHAR(255) NOT NULL,
    `payload` JSON NOT NULL,
    `headers` JSON NULL,
    `published` BOOLEAN NOT NULL DEFAULT FALSE,
    `created_at` TIMESTAMP(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    KEY `ix_outbox_created` (`created_at`),
    KEY `ix_outbox_unpublished` (`published`,`created_at`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;