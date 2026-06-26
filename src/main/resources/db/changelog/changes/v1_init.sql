--liquibase formatted sql

--changeset robert:1

CREATE TABLE wallet(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    balance NUMERIC(19, 2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL
);

CREATE TABLE transaction(
     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
     sender_id UUID REFERENCES wallet (id),
     receiver_id UUID NOT NULL REFERENCES wallet (id),
     amount NUMERIC(19, 2) NOT NULL,
     type VARCHAR(16) NOT NULL,
     created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);