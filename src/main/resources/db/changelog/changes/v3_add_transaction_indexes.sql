--liquibase formatted sql

--changeset robert:3

CREATE INDEX index_transaction_sender ON transaction (sender_id);
CREATE INDEX index_transaction_receiver ON transaction (receiver_id);