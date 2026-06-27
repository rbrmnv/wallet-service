--liquibase formatted sql

--changeset robert:4

CREATE INDEX index_transaction_created_at ON transaction (created_at);

--changeset robert:5

INSERT INTO wallet (balance, currency)
SELECT (random() * 9000 + 1000)::numeric(19,2), c.currency
FROM (VALUES ('USD'), ('EUR')) AS c(currency), generate_series(1, 3);