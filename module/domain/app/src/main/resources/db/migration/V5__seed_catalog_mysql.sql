-- dataset 등록
INSERT INTO catalog_dataset (name, domain, type)
VALUES ('member-product','member','TABLE')
    ON DUPLICATE KEY UPDATE domain=VALUES(domain), type=VALUES(type);

INSERT INTO catalog_dataset (name, domain, type)
VALUES ('member-events','member','STREAM')
    ON DUPLICATE KEY UPDATE domain=VALUES(domain), type=VALUES(type);

-- 스키마 등록 (v1)
INSERT INTO schema_registry (dataset_name, version, schema_json)
VALUES ('member-events','v1',
        JSON_OBJECT(
                'type','record',
                'name','MemberRegistered',
                'fields', JSON_ARRAY(
                        JSON_OBJECT('name','memberId','type','string'),
                        JSON_OBJECT('name','email','type','string'),
                        JSON_OBJECT('name','name','type','string')
                          )
        ))
    ON DUPLICATE KEY UPDATE schema_json=VALUES(schema_json);
