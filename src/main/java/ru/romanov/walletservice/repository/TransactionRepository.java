package ru.romanov.walletservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.romanov.walletservice.model.Transaction;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("select t from Transaction t where t.sender.id = :walletId or t.receiver.id = :walletId")
    List<Transaction> findByWalletId(@Param("walletId") UUID walletId);

    long countByCreatedAtGreaterThanEqual(OffsetDateTime time);
}
