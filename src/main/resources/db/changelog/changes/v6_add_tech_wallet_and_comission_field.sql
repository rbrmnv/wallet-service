--liquibase formatted sql

--changeset robert:7

INSERT INTO wallet (id, balance, currency) VALUES
  ('00000000-0000-0000-0000-000000000001', 0.00, 'RUB'),
  ('00000000-0000-0000-0000-000000000002', 0.00, 'EUR'),
  ('00000000-0000-0000-0000-000000000003', 0.00, 'USD');

--changeset robert:8

ALTER TABLE transaction ADD COLUMN commission NUMERIC(19,2);