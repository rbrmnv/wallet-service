package ru.romanov.walletservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.romanov.walletservice.dto.TransactionRequest;
import ru.romanov.walletservice.model.Wallet;
import ru.romanov.walletservice.repository.WalletRepository;
import ru.romanov.walletservice.service.TransactionService;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class TransactionServiceConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void propertiesSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void deadlockTest() throws InterruptedException {
        Wallet a = walletRepository.save(
                Wallet.builder()
                        .balance(new BigDecimal("1000.00"))
                        .currency("RUB")
                        .build()
        );
        Wallet b = walletRepository.save(
                Wallet.builder()
                        .balance(new BigDecimal("1000.00"))
                        .currency("RUB")
                        .build()
        );

        UUID aId = a.getId();
        UUID bId = b.getId();

        int rounds = 50;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch done = new CountDownLatch(rounds * 2);

        for (int i = 0; i < rounds; i++) {
            executor.submit(() -> {
                try {
                    transactionService.transfer(
                            new TransactionRequest(null, aId, bId, new BigDecimal("10.00")));
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
            executor.submit(() -> {
                try {
                    transactionService.transfer(
                            new TransactionRequest(null, bId, aId, new BigDecimal("10.00")));
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        boolean finished = done.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
    }
}