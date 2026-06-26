package ru.romanov.walletservice.dto;

import java.util.List;

public record WalletBalanceResponse(
       WalletResponse walletResponse,
       List<TransactionResponse> listOfTTransactionResponses
){}
