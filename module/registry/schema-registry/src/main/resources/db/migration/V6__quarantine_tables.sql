CREATE TABLE IF NOT EXISTS platform_governance.quarantine_event(
  id BIGSERIAL PRIMARY KEY,
  event_id TEXT,
  reason TEXT,
  payload JSONB,
  received_at TIMESTAMPTZ DEFAULT now(),
  correlation_id TEXT,
  causation_id TEXT
);
