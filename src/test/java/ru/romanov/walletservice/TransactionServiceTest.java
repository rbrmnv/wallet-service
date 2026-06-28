package ru.romanov.walletservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.romanov.walletservice.dto.TransactionRequest;
import ru.romanov.walletservice.exception.NotEnoughAmountException;
import ru.romanov.walletservice.model.Transaction;
import ru.romanov.walletservice.model.Wallet;
import ru.romanov.walletservice.repository.TransactionRepository;
import ru.romanov.walletservice.repository.WalletRepository;
import ru.romanov.walletservice.service.TransactionService;

import java.math.BigDecimal;
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

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void checkRejectOperationTest() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        Wallet sender = Wallet.builder()
                .id(senderId)
                .balance(new BigDecimal("50.00"))
                .currency("RUB")
                .build();

        Wallet receiver = Wallet.builder()
                .id(receiverId)
                .balance(new BigDecimal("100.00"))
                .currency("RUB")
                .build();

        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(walletRepository.findWithLockById(senderId)).thenReturn(Optional.of(sender));
        when(walletRepository.findWithLockById(receiverId)).thenReturn(Optional.of(receiver));

        TransactionRequest request =
                new TransactionRequest(UUID.randomUUID(), UUID.randomUUID(),
                        senderId, receiverId, new BigDecimal("100.00"));

        assertThatThrownBy(() -> transactionService.transfer(request))
                .isInstanceOf(NotEnoughAmountException.class);

        assertThat(sender.getBalance()).isEqualByComparingTo("50.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("100.00");
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void checkCompleteOperationTest() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();

        Wallet sender = Wallet.builder()
                .id(receiverId)
                .balance(new BigDecimal("100.00"))
                .currency("RUB")
                .build();

        Wallet receiver = Wallet.builder()
                .id(receiverId)
                .balance(new BigDecimal("100.00"))
                .currency("RUB")
                .build();

        when(transactionRepository.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(walletRepository.findWithLockById(senderId)).thenReturn(Optional.of(sender));
        when(walletRepository.findWithLockById(receiverId)).thenReturn(Optional.of(receiver));
        when(transactionRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionRequest request =
                new TransactionRequest(UUID.randomUUID(), UUID.randomUUID(),
                        senderId, receiverId, new BigDecimal("30.00"));

        transactionService.transfer(request);

        assertThat(sender.getBalance()).isEqualByComparingTo("70.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("130.00");
        verify(transactionRepository).saveAndFlush(any(Transaction.class));
    }
}