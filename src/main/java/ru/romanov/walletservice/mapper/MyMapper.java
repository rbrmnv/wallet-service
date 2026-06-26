package ru.romanov.walletservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.romanov.walletservice.dto.TransactionResponse;
import ru.romanov.walletservice.dto.WalletResponse;
import ru.romanov.walletservice.model.Transaction;
import ru.romanov.walletservice.model.Wallet;

import java.util.List;

@Mapper
public interface MyMapper {
    @Mapping(target = "fromWalletId", source = "sender.id")
    @Mapping(target = "toWalletId", source = "receiver.id")
    TransactionResponse toResponse(Transaction transaction);

    List<WalletResponse> toResponseList(List<Wallet> wallets);
}
