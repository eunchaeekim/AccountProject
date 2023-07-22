package com.example.Account.repository;

import com.example.Account.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {//<활용할 엔티티, pk 데이터 타입>
    Optional<Transaction> findByTransactionId(String transactionId);
}
