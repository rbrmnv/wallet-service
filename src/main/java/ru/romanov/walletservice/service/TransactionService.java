package ru.romanov.walletservice.service;

import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.romanov.walletservice.dto.TransactionRequest;
import ru.romanov.walletservice.dto.TransactionResponse;
import ru.romanov.walletservice.exception.CurrencyMistakeException;
import ru.romanov.walletservice.exception.NotEnoughAmountException;
import ru.romanov.walletservice.exception.WalletNotFoundException;
import ru.romanov.walletservice.mapper.MyMapper;
import ru.romanov.walletservice.model.Transaction;
import ru.romanov.walletservice.model.TransactionType;
import ru.romanov.walletservice.model.Wallet;
import ru.romanov.walletservice.repository.TransactionRepository;
import ru.romanov.walletservice.repository.WalletRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;


@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final MyMapper mapper = Mappers.getMapper(MyMapper.class);

    @Transactional
    public TransactionResponse transfer(TransactionRequest request) {
        Transaction transaction;
        BigDecimal amount = request.amount();
        Wallet reciver = walletRepository.findWithLockById(request.toWalletId())
                .orElseThrow(() -> new WalletNotFoundException(
                        String.format("Wallet with id = %s not found", request.toWalletId())
                ));

        if (request.fromWalletId() == null){
            transaction = depositOperation(reciver, amount);
        } else {
            Wallet sender = walletRepository.findWithLockById(request.fromWalletId())
                    .orElseThrow(() -> new WalletNotFoundException(
                            String.format("Wallet with id = %s not found", request.fromWalletId())
                    ));
            transaction = paymentOperation(sender, reciver, amount);
        }

        Transaction currentTransaction = transactionRepository.save(transaction);
        return mapper.toResponse(currentTransaction);
    }


    private Transaction paymentOperation(Wallet sender, Wallet receiver, BigDecimal amount){
        if (!sender.getCurrency().equals(receiver.getCurrency())){
            throw new CurrencyMistakeException(
                    String.format("Currency mistake from %s to %s",
                            sender.getCurrency(), receiver.getCurrency())
            );
        }

        if (sender.getBalance().compareTo(amount) < 0){
            throw new NotEnoughAmountException(
                    String.format("Sender %s not have enough amount", sender.getId())
            );
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        return buildTransaction(sender,receiver,amount,TransactionType.PAYMENT);
    }

    private Transaction depositOperation(Wallet reciver, BigDecimal amount){
        reciver.setBalance(reciver.getBalance().add(amount));
        return buildTransaction(null,reciver,amount,TransactionType.DEPOSIT);
    }


    private Transaction buildTransaction(Wallet sender, Wallet receiver,
                                         BigDecimal amount, TransactionType type){
        return Transaction.builder()
                .sender(sender)
                .receiver(receiver)
                .amount(amount)
                .type(type)
                .createdAt(OffsetDateTime.now())
                .build();
    }
}

