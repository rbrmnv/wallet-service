--liquibase formatted sql

--changeset robert:6

ALTER TABLE transaction ADD COLUMN idempotency_key UUID;
CREATE UNIQUE INDEX index_transaction_idempotency_key ON transaction (idempotency_key);