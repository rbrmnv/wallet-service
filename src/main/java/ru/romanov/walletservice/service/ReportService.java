package ru.romanov.walletservice.service;

import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import ru.romanov.walletservice.dto.WalletBalanceResponse;
import ru.romanov.walletservice.exception.WalletNotFoundException;
import ru.romanov.walletservice.mapper.TransactionMapper;
import ru.romanov.walletservice.mapper.WalletMapper;
import ru.romanov.walletservice.model.Transaction;
import ru.romanov.walletservice.model.Wallet;
import ru.romanov.walletservice.repository.TransactionRepository;
import ru.romanov.walletservice.repository.WalletRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final WalletMapper walletMapper = Mappers.getMapper(WalletMapper.class);
    private final TransactionMapper transactionMapper = Mappers.getMapper(TransactionMapper.class);

    public WalletBalanceResponse getWalletBalance(UUID walletId){
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new WalletNotFoundException(
                        String.format("Wallet with id = %s not found", walletId)
                ));

        List<Transaction> listOfTTransactions = transactionRepository
                .findByWalletId(walletId);

        return new WalletBalanceResponse(
                walletMapper.toResponse(wallet),
                transactionMapper.toResponseList(listOfTTransactions)
        );
    }
}
