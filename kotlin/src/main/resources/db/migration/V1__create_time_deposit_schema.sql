CREATE TABLE "timeDeposits" (
    id INTEGER PRIMARY KEY,
    "planType" VARCHAR NOT NULL,
    days INTEGER NOT NULL,
    balance DECIMAL NOT NULL
);

CREATE TABLE withdrawals (
    id INTEGER PRIMARY KEY,
    "timeDepositId" INTEGER NOT NULL,
    amount DECIMAL NOT NULL,
    date DATE NOT NULL,
    CONSTRAINT fk_withdrawals_time_deposit
        FOREIGN KEY ("timeDepositId")
        REFERENCES "timeDeposits" (id)
);
