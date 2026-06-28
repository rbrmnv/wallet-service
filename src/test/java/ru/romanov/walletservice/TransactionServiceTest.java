package ru.romanov.walletservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.romanov.walletservice.config.CommissionProperties;
import ru.romanov.walletservice.dto.TransactionRequest;
import ru.romanov.walletservice.exception.NotEnoughAmountException;
import ru.romanov.walletservice.model.Transaction;
import ru.romanov.walletservice.model.Wallet;
import ru.romanov.walletservice.repository.TransactionRepository;
import ru.romanov.walletservice.repository.WalletRepository;
import ru.romanov.walletservice.service.TransactionService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CommissionProperties properties;

    @InjectMocks
    private TransactionService transactionService;

    private static final UUID COMMISSION_ACCOUNT_ID = UUID
            .fromString("00000000-0000-0000-0000-000000000001");

    private void mockCommissionConfig() {
        when(properties.getPercent()).thenReturn(new BigDecimal("0.01"));
        when(properties.getTechAccounts()).thenReturn(Map.of("RUB", COMMISSION_ACCOUNT_ID));
    }

    private Wallet buildRUBWallet(UUID id, String balance) {
        return Wallet.builder()
                .id(id)
                .balance(new BigDecimal(balance))
                .currency("RUB")
                .build();
    }

    @Test
    void checkRejectOperationTest() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        Wallet sender = buildRUBWallet(senderId, "50.00");
        Wallet receiver = buildRUBWallet(receiverId, "100.00");
        Wallet commissionAccount = buildRUBWallet(COMMISSION_ACCOUNT_ID, "0.00");

        mockCommissionConfig();
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(walletRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(walletRepository.findWithLockById(senderId)).thenReturn(Optional.of(sender));
        when(walletRepository.findWithLockById(receiverId)).thenReturn(Optional.of(receiver));
        when(walletRepository.findWithLockById(COMMISSION_ACCOUNT_ID))
                .thenReturn(Optional.of(commissionAccount));

        TransactionRequest request = new TransactionRequest(
                UUID.randomUUID(), UUID.randomUUID(), senderId, receiverId, new BigDecimal("100.00"));

        assertThatThrownBy(() -> transactionService.transfer(request))
                .isInstanceOf(NotEnoughAmountException.class);

        assertThat(sender.getBalance()).isEqualByComparingTo("50.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("100.00");
        assertThat(commissionAccount.getBalance()).isEqualByComparingTo("0.00");
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void checkCompleteOperationTest() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        Wallet sender = buildRUBWallet(senderId, "100.00");
        Wallet receiver = buildRUBWallet(receiverId, "100.00");
        Wallet commissionAccount = buildRUBWallet(COMMISSION_ACCOUNT_ID, "0.00");

        mockCommissionConfig();
        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(walletRepository.findById(senderId)).thenReturn(Optional.of(sender));
        when(walletRepository.findWithLockById(senderId)).thenReturn(Optional.of(sender));
        when(walletRepository.findWithLockById(receiverId)).thenReturn(Optional.of(receiver));
        when(walletRepository.findWithLockById(COMMISSION_ACCOUNT_ID))
                .thenReturn(Optional.of(commissionAccount));
        when(transactionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionRequest request = new TransactionRequest(
                UUID.randomUUID(), UUID.randomUUID(), senderId, receiverId, new BigDecimal("30.00"));

        transactionService.transfer(request);

        assertThat(sender.getBalance()).isEqualByComparingTo("69.70");
        assertThat(receiver.getBalance()).isEqualByComparingTo("130.00");
        assertThat(commissionAccount.getBalance()).isEqualByComparingTo("0.30");
        verify(transactionRepository).saveAndFlush(any(Transaction.class));
    }
}