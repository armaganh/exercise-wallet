package com.playtomic.tests.wallet.service.impl;

import com.playtomic.tests.wallet.entity.Transaction;
import com.playtomic.tests.wallet.entity.Wallet;
import com.playtomic.tests.wallet.enums.TransactionStatus;
import com.playtomic.tests.wallet.enums.TransactionType;
import com.playtomic.tests.wallet.exception.PlaytomicException;
import com.playtomic.tests.wallet.exception.StripeAmountTooSmallException;
import com.playtomic.tests.wallet.model.TransactionRequest;
import com.playtomic.tests.wallet.service.StripeService;
import com.playtomic.tests.wallet.service.TransactionService;
import com.playtomic.tests.wallet.service.WalletService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@EnableJpaAuditing
@ActiveProfiles(profiles = "test")
public class TransactionServiceTest {
	private final Logger log = LoggerFactory.getLogger(TransactionServiceTest.class);
	@Autowired
	TransactionService transactionService;

	@Autowired
	WalletService walletService;

	@MockBean
	StripeService stripeService;

	Wallet wallet = new Wallet();

	private volatile boolean failed  = false;

	@BeforeEach
	public void setUp() {
		doNothing().when(stripeService).charge(Mockito.anyString(), Mockito.any());
		doNothing().when(stripeService).refund(Mockito.anyString());
		doThrow(new StripeAmountTooSmallException()).when(stripeService).charge(Mockito.anyString(),
				Mockito.eq(BigDecimal.valueOf(5)));
		
		wallet.setName("Test");
		wallet = walletService.createWallet(wallet);
	}

	@Test
	public void topUp() {
		TransactionRequest transactionReq = new TransactionRequest();
		transactionReq.setAmount(BigDecimal.valueOf(100));
		transactionReq.setCreditCardNumber("42424242424242424242");
		transactionService.topUp(wallet.getId(), transactionReq);
		assertTrue(BigDecimal.valueOf(100).compareTo(walletService.getWalletById(wallet.getId()).getBalance()) == 0);
	}
	
	@Test
	public void topUpWith5AndNoTransaction() {
		TransactionRequest transactionReq = new TransactionRequest();
		transactionReq.setAmount(BigDecimal.valueOf(5));
		transactionReq.setCreditCardNumber("42424242424242424242");
		Assertions.assertThrows(com.playtomic.tests.wallet.exception.StripeAmountTooSmallException.class, () -> {
			transactionService.topUp(wallet.getId(), transactionReq);
		});
		assertTrue(BigDecimal.valueOf(0).compareTo(walletService.getWalletById(wallet.getId()).getBalance()) == 0);
		Page<Transaction> pages = transactionService.getTransactionHistory(wallet.getId(), 0, 5);
		assertEquals(Boolean.TRUE, pages.getContent().isEmpty());
	}
	
	@Test
	public void refund() {
		// Top-up/Charge
		TransactionRequest transactionReq = new TransactionRequest();
		transactionReq.setAmount(BigDecimal.valueOf(100));
		transactionReq.setCreditCardNumber("42424242424242424242");
		Long transactionId = transactionService.topUp(wallet.getId(), transactionReq).getId();
		assertTrue(BigDecimal.valueOf(100).compareTo(walletService.getWalletById(wallet.getId()).getBalance()) == 0);
	
		
		// Refund
		Long refundTransactionId = transactionService.refund(wallet.getId(), transactionId).getId();
		Transaction refundTransaction = transactionService.getTransaction(refundTransactionId);
		assertEquals(TransactionType.REFUND, refundTransaction.getType());
		assertEquals(TransactionStatus.ACTIVE, refundTransaction.getStatus());
		assertTrue(BigDecimal.valueOf(100).compareTo(refundTransaction.getAmount()) == 0);
		
		// Check Charge status
		assertEquals(TransactionStatus.CANCEL, transactionService.getTransaction(transactionId).getStatus());
		
	    // Check Wallet Balance
		assertTrue(BigDecimal.valueOf(0).compareTo(walletService.getWalletById(wallet.getId()).getBalance()) == 0);
	}

	@Test
	public void refundConcurrency() throws InterruptedException {
		// Top-up/Charge
		TransactionRequest transactionReq = new TransactionRequest();
		transactionReq.setAmount(BigDecimal.valueOf(100));
		transactionReq.setCreditCardNumber("42424242424242424242");
		Long transactionId = transactionService.topUp(wallet.getId(), transactionReq).getId();
		assertTrue(BigDecimal.valueOf(100).compareTo(walletService.getWalletById(wallet.getId()).getBalance()) == 0);

		final ExecutorService executor = Executors.newFixedThreadPool(3);

		for (int i = 0; i < 3; i++) {
			executor.execute(() -> {
				try {
					Transaction refundTransaction = transactionService.refund(wallet.getId(), transactionId);
					if ( BigDecimal.valueOf(100).compareTo(refundTransaction.getAmount()) != 0 ||
							!TransactionStatus.CANCEL.equals(transactionService.getTransaction(transactionId).getStatus())||
							BigDecimal.valueOf(0).compareTo(walletService.getWalletById(wallet.getId()).getBalance()) != 0
					) {
						failed = true;
					}

				} catch (PlaytomicException exception) {
					// The Optimistic lock retry again and send Error message Already cancelled or status Passive
					log.error(exception.getErrorMessage());
				} catch (Exception exception) {
					exception.printStackTrace();
					failed = true;
				}
			});
		}
		Thread.sleep(5000);
		if(failed) {
			Assertions.fail();
		}

		executor.shutdown();
	}
	
	@Test
	public void purchase() {
		TransactionRequest transactionReq = new TransactionRequest();
		transactionReq.setAmount(BigDecimal.valueOf(100));
		transactionReq.setCreditCardNumber("42424242424242424242");
		transactionService.topUp(wallet.getId(), transactionReq);
		assertTrue(BigDecimal.valueOf(100).compareTo(walletService.getWalletById(wallet.getId()).getBalance()) == 0);
	
		transactionReq.setAmount(BigDecimal.valueOf(50));
		
		Long transactionId = transactionService.purchase(wallet.getId(), transactionReq).getId();
		Transaction purchaseTransaction = transactionService.getTransaction(transactionId);

		assertTrue(BigDecimal.valueOf(50).compareTo(walletService.getWalletById(wallet.getId()).getBalance()) == 0);
		assertTrue(BigDecimal.valueOf(50).compareTo(purchaseTransaction.getAmount()) == 0);
		assertEquals(TransactionType.PURCHASE, purchaseTransaction.getType());
		assertEquals(TransactionStatus.ACTIVE, purchaseTransaction.getStatus());
		
	}
	@Test
	public void purchaseThrowInsuffitionFundError() {
		TransactionRequest transactionReq = new TransactionRequest();
		transactionReq.setAmount(BigDecimal.valueOf(50));
		transactionReq.setCreditCardNumber("42424242424242424242");
		transactionService.topUp(wallet.getId(), transactionReq);
		assertTrue(BigDecimal.valueOf(50).compareTo(walletService.getWalletById(wallet.getId()).getBalance()) == 0);
	
		transactionReq.setAmount(BigDecimal.valueOf(100));
		
		Assertions.assertThrows(com.playtomic.tests.wallet.exception.PlaytomicException.class, () -> {
			transactionService.purchase(wallet.getId(), transactionReq);
		});
	
	}
	
	@Test
	public void cancelPurchase() {
		// Top-up/Charge
		TransactionRequest transactionReq = new TransactionRequest();
		transactionReq.setAmount(BigDecimal.valueOf(100));
		transactionReq.setCreditCardNumber("42424242424242424242");
		transactionService.topUp(wallet.getId(), transactionReq);
		assertTrue(BigDecimal.valueOf(100).compareTo(walletService.getWalletById(wallet.getId()).getBalance()) == 0);
	

		// Purchase
		transactionReq.setAmount(BigDecimal.valueOf(50));
		Long transactionId = transactionService.purchase(wallet.getId(), transactionReq).getId();
		Transaction purchaseTransaction = transactionService.getTransaction(transactionId);
		assertEquals(TransactionType.PURCHASE, purchaseTransaction.getType());
		assertEquals(TransactionStatus.ACTIVE, purchaseTransaction.getStatus());
		assertTrue(BigDecimal.valueOf(50).compareTo(purchaseTransaction.getAmount()) == 0);
		
		// Cancelled Purchase
		Long cancelTransactionId = transactionService.cancelPurchase(wallet.getId(), transactionId).getId();
		Transaction cancelTransaction = transactionService.getTransaction(cancelTransactionId);
		assertEquals(TransactionType.CANCEL_PURCHASE, cancelTransaction.getType());
		assertEquals(TransactionStatus.ACTIVE, cancelTransaction.getStatus());
		assertTrue(BigDecimal.valueOf(50).compareTo(cancelTransaction.getAmount()) == 0);
		
		// Check Purchase status
		purchaseTransaction = transactionService.getTransaction(transactionId);
		assertEquals(TransactionStatus.CANCEL, purchaseTransaction.getStatus());
		

	    // Check Wallet Balance
		assertTrue(BigDecimal.valueOf(100).compareTo(walletService.getWalletById(wallet.getId()).getBalance()) == 0);
	}

}
