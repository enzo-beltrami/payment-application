CREATE TABLE payment (
    id                     UUID         PRIMARY KEY,
    first_name             VARCHAR(100) NOT NULL,
    last_name              VARCHAR(100) NOT NULL,
    zip_code               VARCHAR(20)  NOT NULL,
    card_number_encrypted  TEXT         NOT NULL,
    card_last_four         VARCHAR(4)   NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE webhook_subscription (
    id          UUID        PRIMARY KEY,
    url         TEXT        NOT NULL,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_subscription_active ON webhook_subscription (active);

CREATE TABLE webhook_delivery (
    id                  UUID        PRIMARY KEY,
    payment_id          UUID        NOT NULL REFERENCES payment (id),
    subscription_id     UUID        NOT NULL REFERENCES webhook_subscription (id),
    url                 TEXT        NOT NULL,
    status              VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'DELIVERED', 'FAILED')),
    attempts            INT         NOT NULL DEFAULT 0,
    next_attempt_at     TIMESTAMPTZ NOT NULL,
    last_response_code  INT,
    last_error          TEXT,
    payload_json        TEXT        NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_delivery_status_due ON webhook_delivery (status, next_attempt_at);
