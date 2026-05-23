CREATE TABLE vehicle_enrichment_override (
    id BIGSERIAL PRIMARY KEY,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    version VARCHAR(150) NOT NULL,
    attribute_name VARCHAR(120) NOT NULL,
    attribute_value TEXT NOT NULL,
    source_note VARCHAR(255)
);

CREATE INDEX idx_override_vehicle
    ON vehicle_enrichment_override (lower(brand), lower(model), lower(version));

CREATE TABLE vehicle_query_history (
    id BIGSERIAL PRIMARY KEY,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    version VARCHAR(150) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE vehicle_query_attribute_result (
    id BIGSERIAL PRIMARY KEY,
    query_history_id BIGINT NOT NULL REFERENCES vehicle_query_history(id) ON DELETE CASCADE,
    attribute_name VARCHAR(120) NOT NULL,
    attribute_value TEXT NOT NULL,
    data_source VARCHAR(50) NOT NULL
);
