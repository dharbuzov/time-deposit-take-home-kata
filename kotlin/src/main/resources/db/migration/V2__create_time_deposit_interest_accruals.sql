CREATE TABLE time_deposit_interest_accruals (
    time_deposit_id INTEGER NOT NULL,
    accrual_period VARCHAR(7) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_time_deposit_interest_accrual
        UNIQUE (time_deposit_id, accrual_period),
    CONSTRAINT fk_time_deposit_interest_accruals_time_deposit
        FOREIGN KEY (time_deposit_id)
        REFERENCES "timeDeposits" (id)
);
