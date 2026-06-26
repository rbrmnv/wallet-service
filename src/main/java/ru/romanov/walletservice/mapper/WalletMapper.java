package ru.romanov.walletservice.mapper;

import org.mapstruct.Mapper;
import ru.romanov.walletservice.dto.WalletResponse;
import ru.romanov.walletservice.model.Wallet;

import java.util.List;

@Mapper
public interface WalletMapper {
    List<WalletResponse> toResponseList(List<Wallet> wallets);
    WalletResponse toResponse(Wallet wallet);
}
