package ru.romanov.walletservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.romanov.walletservice.model.Transaction;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
}
