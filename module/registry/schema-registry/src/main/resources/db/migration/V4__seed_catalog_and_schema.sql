-- Data Product 등록
INSERT INTO platform_catalog.data_product(domain_key, product_key, name, description)
VALUES ('sample','member-product','Sample Member Product','Member aggregate event stream')
ON CONFLICT DO NOTHING;

-- STREAM 데이터셋 등록 (Kafka 토픽 메타 포함)
INSERT INTO platform_catalog.dataset(product_id, dataset_key, type, storage)
SELECT id, 'member-events', 'STREAM', jsonb_build_object('topic','domain.sample.member.events')
FROM platform_catalog.data_product
WHERE domain_key='sample' AND product_key='member-product'
ON CONFLICT DO NOTHING;

-- 스키마(v1) 등록 (JSON 예시)
INSERT INTO platform_registry.schema_registry(schema_name, version, format, definition, compatibility)
VALUES (
  'member-event', 1, 'JSON',
  '{"type":"record","name":"MemberRegistered","fields":[{"name":"memberId","type":"string"},{"name":"email","type":"string"},{"name":"name","type":"string"}]}',
  'BACKWARD'
)
ON CONFLICT DO NOTHING;
