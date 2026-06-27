package ru.romanov.walletservice.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import ru.romanov.walletservice.dto.CurrencyTotalGroups;
import ru.romanov.walletservice.model.Wallet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    @Query("""
    select new ru.romanov.walletservice.dto.CurrencyTotalGroups(wallet.currency, sum(wallet.balance)) 
    from Wallet wallet 
    group by wallet.currency
    """)
    List<CurrencyTotalGroups> getCurrencyTotalGroupsList();

    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    Optional<Wallet> findWithLockById(UUID id);
}
