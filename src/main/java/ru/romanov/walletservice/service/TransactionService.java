package ru.romanov.walletservice.service;

import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.romanov.walletservice.dto.TransactionRequest;
import ru.romanov.walletservice.dto.TransactionResponse;
import ru.romanov.walletservice.exception.CurrencyMistakeException;
import ru.romanov.walletservice.exception.NotEnoughAmountException;
import ru.romanov.walletservice.exception.SelfWalletTransferException;
import ru.romanov.walletservice.exception.WalletNotFoundException;
import ru.romanov.walletservice.mapper.TransactionMapper;
import ru.romanov.walletservice.model.Transaction;
import ru.romanov.walletservice.model.TransactionType;
import ru.romanov.walletservice.model.Wallet;
import ru.romanov.walletservice.repository.TransactionRepository;
import ru.romanov.walletservice.repository.WalletRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final TransactionMapper mapper = Mappers.getMapper(TransactionMapper.class);

    @Transactional
    public TransactionResponse transfer(TransactionRequest request) {
        UUID idempotencyKey = request.idempotencyKey();

        if (idempotencyKey != null) {
            Optional<TransactionResponse> currentTransaction = findByExistingIdempotencyKey(idempotencyKey);
            if (currentTransaction.isPresent()) {
                return currentTransaction.get();
            }
        }

        if (request.fromWalletId() != null && request.fromWalletId().equals(request.toWalletId())) {
            throw new SelfWalletTransferException(
                    String.format("Cannot transfer to self wallet: %s", request.fromWalletId()));
        }

        Transaction transaction;
        BigDecimal amount = request.amount();

        if (request.fromWalletId() == null) {
            Wallet receiver = walletRepository.findWithLockById(request.toWalletId())
                    .orElseThrow(() -> new WalletNotFoundException(
                            String.format("Receivers wallet  with id = %s not found", request.toWalletId())));
            transaction = depositOperation(receiver, amount);
        } else {
            UUID firstParticipantId = request.fromWalletId();
            UUID secondParticipantId = request.toWalletId();

            boolean isSenderParticipantIdSmaller  = firstParticipantId.compareTo(secondParticipantId) < 0;

            UUID smallerId;
            UUID higherId;
            if (isSenderParticipantIdSmaller ){
                smallerId = firstParticipantId;
                higherId = secondParticipantId;
            } else {
                smallerId = secondParticipantId;
                higherId = firstParticipantId;
            }

            Wallet lower = walletRepository.findWithLockById(smallerId)
                    .orElseThrow(() -> new WalletNotFoundException(
                            String.format("Wallet with id = %s not found", smallerId)));
            Wallet higher = walletRepository.findWithLockById(higherId)
                    .orElseThrow(() -> new WalletNotFoundException(
                            String.format("Wallet with id = %s not found", higherId)));

            Wallet sender;
            Wallet receiver;
            if (isSenderParticipantIdSmaller ){
                sender = lower;
                receiver = higher;
            } else {
                sender = higher;
                receiver = lower;
            }

            transaction = paymentOperation(sender, receiver, amount);
        }

        transaction.setIdempotencyKey(idempotencyKey);

        try {
            Transaction saved = transactionRepository.saveAndFlush(transaction);
            return mapper.toResponse(saved);
        } catch (DataIntegrityViolationException exception){
            return findByExistingIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> exception);
        }
    }

    private Optional<TransactionResponse> findByExistingIdempotencyKey(UUID idempotencyKey) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(mapper::toResponse);
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
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }
}

