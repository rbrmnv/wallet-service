package ru.romanov.walletservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.romanov.walletservice.dto.TransactionResponse;
import ru.romanov.walletservice.model.Transaction;

import java.util.List;

@Mapper
public interface TransactionMapper {
    @Mapping(target = "fromWalletId", source = "sender.id")
    @Mapping(target = "toWalletId", source = "receiver.id")
    TransactionResponse toResponse(Transaction transaction);

    List<TransactionResponse> toResponseList(List<Transaction> transactions);
}
