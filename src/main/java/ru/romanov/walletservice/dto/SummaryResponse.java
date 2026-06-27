package ru.romanov.walletservice.dto;

import java.math.BigDecimal;
import java.util.Map;

public record SummaryResponse(
    Map<String, BigDecimal> totalAmoutByCurrencyGroups,
    long countOfSuccsesfulTransactions
){}
