package ru.romanov.walletservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        BigDecimal balance,
        String currency
){}