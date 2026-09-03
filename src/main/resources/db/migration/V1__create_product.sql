CREATE TABLE product (
    id       BIGSERIAL PRIMARY KEY,
    title    TEXT NOT NULL,
    vendor   TEXT,
    price    NUMERIC(10, 2),
    handle   TEXT NOT NULL UNIQUE,
    variants JSONB
);
