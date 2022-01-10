package com.playtomic.tests.wallet.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.playtomic.tests.wallet.entity.Transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionRepository extends JpaRepository<Transaction, Long>{
	Page<Transaction> findByWalletId(Long walletId, Pageable pageable);
}
