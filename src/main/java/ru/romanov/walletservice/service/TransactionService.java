package ru.romanov.walletservice.service;

import lombok.RequiredArgsConstructor;
import org.mapstruct.factory.Mappers;
import org.springframework.cache.annotation.CacheEvict;
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
    @CacheEvict(value = "wallets", allEntries = true)
    public TransactionResponse transfer(TransactionRequest request) {
        UUID idempotencyKey = request.idempotencyKey();
        Optional<TransactionResponse> exitingTransaction = findByExistingIdempotencyKey(idempotencyKey);
        if (exitingTransaction.isPresent()) {
            return exitingTransaction.get();
        }

        if (request.fromWalletId() != null && request.fromWalletId().equals(request.toWalletId())) {
            throw new SelfWalletTransferException(
                    String.format("Cannot transfer to self wallet: %s", request.fromWalletId()));
        }

        Transaction transaction;
        if (request.fromWalletId() == null){
            transaction = processDeposit(request);
        } else {
            transaction = processPayment(request);
        }

        transaction.setIdempotencyKey(idempotencyKey);

        try {
            return mapper.toResponse(transactionRepository.saveAndFlush(transaction));
        } catch (DataIntegrityViolationException exception) {
            return findByExistingIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> exception);
        }
    }

    private Transaction processDeposit(TransactionRequest request) {
        Wallet receiver = walletRepository.findWithLockById(request.toWalletId())
                .orElseThrow(() -> new WalletNotFoundException(
                        String.format("Receivers wallet with id = %s not found", request.toWalletId())));
        return depositOperation(receiver, request.amount());
    }

    private Transaction processPayment(TransactionRequest request) {
        UUID senderId = request.fromWalletId();
        UUID receiverId = request.toWalletId();

        UUID commissionAccountId = getCommissionAccountId(senderId);

        Map<UUID, Wallet> lockedWallets = lockWalletsOrdered(
                List.of(senderId, receiverId, commissionAccountId));

        Wallet sender = lockedWallets.get(senderId);
        Wallet receiver = lockedWallets.get(receiverId);
        Wallet commissionAccount = lockedWallets.get(commissionAccountId);
        return paymentOperation(sender, receiver, commissionAccount, request.amount());
    }

    private UUID getCommissionAccountId(UUID senderId) {
        Wallet sender = walletRepository.findById(senderId)
                .orElseThrow(() -> new WalletNotFoundException(
                        String.format("Wallet with id = %s not found", senderId)));
        UUID commissionAccountId = properties.getTechAccounts().get(sender.getCurrency());
        if (commissionAccountId == null) {
            throw new CurrencyMistakeException(
                    String.format("No tech commission account for %s currency", sender.getCurrency()));
        }
        return commissionAccountId;
    }

    private Map<UUID, Wallet> lockWalletsOrdered(List<UUID> walletIds) {
        List<UUID> sortedIds = new ArrayList<>(walletIds);
        sortedIds.sort(Comparator.naturalOrder());

        Map<UUID, Wallet> lockedWallets = new HashMap<>();
        for (UUID walletId : sortedIds) {
            Wallet wallet = walletRepository.findWithLockById(walletId)
                    .orElseThrow(() -> new WalletNotFoundException(
                            String.format("Wallet with id = %s not found", walletId)));
            lockedWallets.put(walletId, wallet);
        }
        return lockedWallets;
    }

    private Optional<TransactionResponse> findByExistingIdempotencyKey(UUID idempotencyKey) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }
        return transactionRepository.findByIdempotencyKey(idempotencyKey)
                .map(mapper::toResponse);
    }

    private Transaction paymentOperation(Wallet sender, Wallet receiver, Wallet commissionAccount,
                                         BigDecimal amount) {
        if (!sender.getCurrency().equals(receiver.getCurrency())) {
            throw new CurrencyMistakeException(
                    String.format("Currency mistake from %s to %s",
                            sender.getCurrency(), receiver.getCurrency()));
        }

        BigDecimal commission = amount.multiply(properties.getPercent())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal amountWithCommission = amount.add(commission);

        if (sender.getBalance().compareTo(amountWithCommission) < 0) {
            throw new NotEnoughAmountException(
                    String.format("Sender %s not have enough amount", sender.getId()));
        }

        sender.setBalance(sender.getBalance().subtract(amountWithCommission));
        receiver.setBalance(receiver.getBalance().add(amount));
        commissionAccount.setBalance(commissionAccount.getBalance().add(commission));
        return buildTransaction(sender, receiver, amount, TransactionType.PAYMENT, commission);
    }

    private Transaction depositOperation(Wallet receiver, BigDecimal amount) {
        receiver.setBalance(receiver.getBalance().add(amount));
        return buildTransaction(null, receiver, amount, TransactionType.DEPOSIT, BigDecimal.ZERO);
    }

    private Transaction buildTransaction(Wallet sender, Wallet receiver, BigDecimal amount,
                                         TransactionType type, BigDecimal commission) {
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