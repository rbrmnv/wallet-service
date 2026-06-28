package ru.romanov.walletservice.service;

import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.romanov.walletservice.config.CommissionProperties;
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
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;


@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final TransactionMapper mapper = Mappers.getMapper(TransactionMapper.class);
    private final CommissionProperties properties;

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

            Wallet firstParticipant = walletRepository.findById(firstParticipantId)
                    .orElseThrow(() -> new WalletNotFoundException(
                            String.format("Wallet with id = %s not found", firstParticipantId)));
            String currency = firstParticipant.getCurrency();

            UUID commissionAccountId = properties.getTechAccounts().get(currency);
            if (commissionAccountId == null) {
                throw new CurrencyMistakeException(
                        String.format("No tech commission account for %s currency ", currency));
            }

            List<UUID> currentListOfWallets = new ArrayList<>(List.of(firstParticipantId,
                    secondParticipantId, commissionAccountId));
            currentListOfWallets.sort(Comparator.naturalOrder());

            Map<UUID, Wallet> lockedMapOfWallets = new HashMap<>();
            for (UUID walletId : currentListOfWallets) {
                Wallet wallet = walletRepository.findWithLockById(walletId)
                        .orElseThrow(() -> new WalletNotFoundException(
                                String.format("Wallet with id = %s not found", walletId)));
                lockedMapOfWallets.put(walletId, wallet);
            }

            Wallet sender = lockedMapOfWallets.get(firstParticipantId);
            Wallet receiver = lockedMapOfWallets.get(secondParticipantId);
            Wallet commissionAccount = lockedMapOfWallets.get(commissionAccountId);

            transaction = paymentOperation(sender, receiver, commissionAccount, amount);
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


    private Transaction paymentOperation(Wallet sender, Wallet receiver, Wallet commissionAccount,
                                         BigDecimal amount){
        if (!sender.getCurrency().equals(receiver.getCurrency())){
            throw new CurrencyMistakeException(
                    String.format("Currency mistake from %s to %s",
                            sender.getCurrency(), receiver.getCurrency())
            );
        }

        BigDecimal commision = amount.multiply(properties.getPercent()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountWithComisiion = amount.add(commision);

        if (sender.getBalance().compareTo(amountWithComisiion) < 0){
            throw new NotEnoughAmountException(
                    String.format("Sender %s not have enough amount", sender.getId())
            );
        }

        sender.setBalance(sender.getBalance().subtract(amountWithComisiion));
        receiver.setBalance(receiver.getBalance().add(amount));
        commissionAccount.setBalance(commissionAccount.getBalance().add(commision));
        return buildTransaction(sender,receiver,amount,TransactionType.PAYMENT,commision);
    }

    private Transaction depositOperation(Wallet reciver, BigDecimal amount){
        reciver.setBalance(reciver.getBalance().add(amount));
        return buildTransaction(null,reciver,amount,TransactionType.DEPOSIT,BigDecimal.ZERO);
    }

    private Transaction buildTransaction(Wallet sender, Wallet receiver, BigDecimal amount,
                                         TransactionType type, BigDecimal commission){
        return Transaction.builder()
                .sender(sender)
                .receiver(receiver)
                .amount(amount)
                .type(type)
                .commission(commission)
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }
}

