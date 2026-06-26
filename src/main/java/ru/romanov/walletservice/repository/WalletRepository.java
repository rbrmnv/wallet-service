package ru.romanov.walletservice.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import ru.romanov.walletservice.model.Wallet;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findWithLockById(UUID id);
}
