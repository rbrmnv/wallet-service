package ru.romanov.walletservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionRequest(
    UUID id,
    UUID fromWalletId,
    @NotNull UUID toWalletId,
    @NotNull @Positive BigDecimal amount
){}
