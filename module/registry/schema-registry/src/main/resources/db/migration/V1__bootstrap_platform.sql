CREATE SCHEMA IF NOT EXISTS platform_catalog;
CREATE SCHEMA IF NOT EXISTS platform_registry;
CREATE SCHEMA IF NOT EXISTS platform_governance;

CREATE TABLE IF NOT EXISTS platform_catalog.data_product(
  id BIGSERIAL PRIMARY KEY,
  domain_key TEXT NOT NULL,
  product_key TEXT NOT NULL,
  name TEXT NOT NULL,
  description TEXT,
  slos JSONB DEFAULT '{}'::jsonb,
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(domain_key, product_key)
);

CREATE TABLE IF NOT EXISTS platform_catalog.dataset(
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT NOT NULL REFERENCES platform_catalog.data_product(id) ON DELETE CASCADE,
  dataset_key TEXT NOT NULL,
  type TEXT NOT NULL,                 -- TABLE/STREAM/VIEW
  storage JSONB DEFAULT '{}'::jsonb,  -- { "topic": "...", "bucket": "...", ... }
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(product_id, dataset_key)
);

CREATE TABLE IF NOT EXISTS platform_catalog.endpoint(
  id BIGSERIAL PRIMARY KEY,
  product_id BIGINT NOT NULL REFERENCES platform_catalog.data_product(id) ON DELETE CASCADE,
  endpoint_key TEXT NOT NULL,
  kind TEXT NOT NULL,                 -- READ_API/WRITE_API/STREAM
  spec JSONB NOT NULL,                -- OpenAPI/AsyncAPI/GraphQL/Avro 등
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(product_id, endpoint_key)
);

CREATE TABLE IF NOT EXISTS platform_registry.schema_registry(
  id BIGSERIAL PRIMARY KEY,
  schema_name TEXT NOT NULL,
  version INT NOT NULL,
  format TEXT NOT NULL,               -- AVRO/JSON/PROTOBUF
  definition TEXT NOT NULL,           -- 스키마 원문
  compatibility TEXT NOT NULL DEFAULT 'BACKWARD',
  status TEXT NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(schema_name, version)
);

CREATE TABLE IF NOT EXISTS platform_governance.policy(
  id BIGSERIAL PRIMARY KEY,
  policy_key TEXT NOT NULL,
  type TEXT NOT NULL,                 -- ACCESS/DATA_QUALITY/PII_MASK 등
  rule JSONB NOT NULL,                -- OPA/CEL 표현식 등
  status TEXT NOT NULL DEFAULT 'ENABLED',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(policy_key)
);

CREATE TABLE IF NOT EXISTS platform_governance.policy_binding(
  id BIGSERIAL PRIMARY KEY,
  policy_id BIGINT NOT NULL REFERENCES platform_governance.policy(id) ON DELETE CASCADE,
  target_type TEXT NOT NULL,          -- PRODUCT/DATASET/ENDPOINT/SCHEMA
  target_key TEXT NOT NULL,           -- "sample.member-product.member-events" 등
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE(policy_id, target_type, target_key)
);

CREATE TABLE IF NOT EXISTS platform_governance.audit_event(
  id BIGSERIAL PRIMARY KEY,
  event_time TIMESTAMPTZ NOT NULL DEFAULT now(),
  actor TEXT,
  action TEXT NOT NULL,
  target TEXT,
  details JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_policy_binding_target
  ON platform_governance.policy_binding(target_type, target_key);
