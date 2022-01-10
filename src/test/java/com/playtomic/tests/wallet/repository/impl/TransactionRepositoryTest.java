package com.playtomic.tests.wallet.repository.impl;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import com.playtomic.tests.wallet.entity.Transaction;
import com.playtomic.tests.wallet.entity.Wallet;
import com.playtomic.tests.wallet.enums.TransactionStatus;
import com.playtomic.tests.wallet.enums.TransactionType;
import com.playtomic.tests.wallet.repository.TransactionRepository;
import com.playtomic.tests.wallet.repository.WalletRepository;

@SpringBootTest
@EnableJpaAuditing
@ActiveProfiles(profiles = "test")
public class TransactionRepositoryTest {
	@Autowired
	WalletRepository walletRepository;

	@Autowired
	TransactionRepository transactionRepository;

	Wallet wallet;

	@BeforeEach
	public void setUp() {
		this.wallet = new Wallet();
		this.wallet.setName("Test");
		walletRepository.save(this.wallet);
	}

	@Test
	public void addTransaction() {
		Transaction t = new Transaction();
		t.setAmount(BigDecimal.TEN);
		t.setType(TransactionType.CHARGE);
		t.setWalletId(wallet.getId());
		transactionRepository.save(t);
		
		Assertions.assertEquals(t.getType(), TransactionType.CHARGE);
		Assertions.assertEquals(t.getAmount(), BigDecimal.TEN);
		Assertions.assertNotNull(t.getId());
		Assertions.assertEquals(t.getStatus(), TransactionStatus.ACTIVE);
	}
	
	
	
	@Test
	public void getTransactionsPagination() {
		 
		Transaction t = new Transaction();
		t.setAmount(BigDecimal.TEN);
		t.setType(TransactionType.CHARGE);
		t.setWalletId(wallet.getId());
		transactionRepository.save(t);

		t = new Transaction();
		t.setAmount(BigDecimal.TEN);
		t.setType(TransactionType.PURCHASE);
		t.setWalletId(wallet.getId());
		transactionRepository.save(t);
		
		t = new Transaction();
		t.setAmount(BigDecimal.TEN);
		t.setType(TransactionType.PURCHASE);
		t.setWalletId(wallet.getId());
		transactionRepository.save(t);
		
		t = new Transaction();
		t.setAmount(BigDecimal.TEN);
		t.setType(TransactionType.CANCEL_PURCHASE);
		t.setWalletId(wallet.getId());
		transactionRepository.save(t);
		
		t = new Transaction();
		t.setAmount(BigDecimal.TEN);
		t.setType(TransactionType.REFUND);
		t.setWalletId(wallet.getId());
		transactionRepository.save(t);
		
		Pageable paging = PageRequest.of(0, 2, Sort.by(new Order(Sort.Direction.DESC, "id")));
		
		Page<Transaction> pages = transactionRepository.findByWalletId(wallet.getId(), paging);
		
		Assertions.assertEquals(0, pages.getNumber());
		Assertions.assertEquals(5, pages.getTotalElements());
		Assertions.assertEquals(3, pages.getTotalPages());
		Assertions.assertEquals(t.getId(),pages.getContent().get(0).getId()); // Last element id must be first element
		
	}

}
