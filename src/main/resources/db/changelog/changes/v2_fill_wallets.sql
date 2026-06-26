--liquibase formatted sql

--changeset robert:2

INSERT INTO wallet (balance, currency)
SELECT (random() * 9000 + 1000)::numeric(19,2), 'RUB'
FROM generate_series(1, 10);