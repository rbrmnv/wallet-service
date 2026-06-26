package ru.romanov.walletservice.dto;

import ru.romanov.walletservice.model.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransactionResponse(
    UUID id,
    UUID fromWalletId,
    UUID toWalletId,
    BigDecimal amount,
    TransactionType type,
    OffsetDateTime createdAt
){}
