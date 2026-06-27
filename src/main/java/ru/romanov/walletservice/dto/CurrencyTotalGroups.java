package ru.romanov.walletservice.dto;

import java.math.BigDecimal;

public record CurrencyTotalGroups(
    String currency,
    BigDecimal totalAmount
){}
