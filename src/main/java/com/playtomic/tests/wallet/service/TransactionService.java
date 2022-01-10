package com.playtomic.tests.wallet.service;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.playtomic.tests.wallet.entity.Transaction;
import com.playtomic.tests.wallet.enums.TransactionStatus;
import com.playtomic.tests.wallet.enums.TransactionType;
import com.playtomic.tests.wallet.exception.PlaytomicException;
import com.playtomic.tests.wallet.model.TransactionRequest;
import com.playtomic.tests.wallet.repository.TransactionRepository;
import com.playtomic.tests.wallet.util.AssertUtil;

/**
 * This is Transaction service class, for each operation create a new transaction record on DB, and add/subtract Wallet total balance.
 * */
@Service
public class TransactionService {
	private static final Logger log = LoggerFactory.getLogger(WalletService.class);

	@Autowired
	private TransactionRepository transactionRepository;

	@Autowired
	private WalletService walletService;
	
	@Autowired
	private StripeService stripeService;

	/**
	 * This service return all transactions of wallet, using paging option to see all transactions
	 * */
	public Page<Transaction> getTransactionHistory(Long walletId, Integer page, Integer size) {
	      Pageable paging = PageRequest.of(page, size, Sort.by(new Order(Sort.Direction.DESC, "id")));
	      return transactionRepository.findByWalletId(walletId, paging);
	}
	
	public Transaction getTransaction(Long transactionId) {
		return transactionRepository.findById(transactionId)
				.orElseThrow(() -> new PlaytomicException(1001, "No data found!", HttpStatus.OK));
	}
	/**
	 * Top-up wallet (added amount the current balance of wallet) and create a transaction record
	 * */
	@Retryable(ObjectOptimisticLockingFailureException.class)
	@Transactional
	public Transaction topUp(Long walletId, TransactionRequest req) {
		Transaction transaction = new Transaction();
		transaction.setWalletId(walletId);
		transaction.setAmount(req.getAmount());
		transaction.setType(TransactionType.CHARGE);
		transaction.setPaymentId("paymentId"); // TODO : stripe Service can not return payment id, normally third party payment id must be set
		transactionRepository.save(transaction);
		walletService.updateWalletBalance(walletId, req.getAmount());
		stripeService.charge(req.getCreditCardNumber(), req.getAmount());
		return transaction;
	}
	/**
	 * Refund payment, update balance of wallet and create a new record (REFUND type), old CHARGE record's status is set CANCELLED
	 * Retryable annotation, the method will  re-trigger method if an optimistic lock exception is occurred(3 times).
	 * */
	@Retryable(ObjectOptimisticLockingFailureException.class)
	@Transactional
	public Transaction refund(Long walletId, Long transactionId) {
		// The charge record's status is set 'CANCEL' before creating new CANCEL_PURCHASE type transaction.
		log.debug("Refund started");
		Transaction charge = getTransaction(transactionId);
		log.info("{} charge will be refund", charge);
		AssertUtil.isTrue(charge.getStatus().equals(TransactionStatus.ACTIVE), 1003, "Top-up already has been cancelled", null);
		AssertUtil.isTrue(charge.getType().equals(TransactionType.CHARGE), 1004, "Transaction is not a Charge", null);
		charge.setStatus(TransactionStatus.CANCEL);
		transactionRepository.save(charge);
		// Create new transaction record for refund operation
		Transaction transaction = new Transaction();
		transaction.setWalletId(walletId);
		transaction.setAmount(charge.getAmount());
		transaction.setType(TransactionType.REFUND);
		transactionRepository.save(transaction);
		walletService.updateWalletBalance(walletId, charge.getAmount().negate());
		stripeService.refund(charge.getPaymentId());
		log.info("{} refund record created and charged record set cancelled status ", transaction);
		log.debug("Refund finished");
		return transaction;
	}
	@Retryable(ObjectOptimisticLockingFailureException.class)
	@Transactional
	public Transaction purchase(Long walletId, TransactionRequest req) {
		Transaction transaction = new Transaction();
		transaction.setWalletId(walletId);
		transaction.setAmount(req.getAmount());
		transaction.setType(TransactionType.PURCHASE);
		transactionRepository.save(transaction);
		walletService.updateWalletBalance(walletId, req.getAmount().negate());
		return transaction;
	}
	@Retryable(ObjectOptimisticLockingFailureException.class)
	@Transactional
	public Transaction cancelPurchase(Long walletId, Long transactionId) {
		// The purchased record's status is set 'CANCEL' before creating new CANCEL_PURCHASE type transaction.
		Transaction purchase = getTransaction(transactionId);
		AssertUtil.isTrue(purchase.getStatus().equals(TransactionStatus.ACTIVE), 1003, "Transaction already has been cancelled", null);
		AssertUtil.isTrue(purchase.getType().equals(TransactionType.PURCHASE), 1004, "Transaction is not a Purchase", null);
		purchase.setStatus(TransactionStatus.CANCEL);
		transactionRepository.save(purchase);
		// Create new transaction record for cancelPurchase operation
		Transaction transaction = new Transaction();
		transaction.setWalletId(walletId);
		transaction.setAmount(purchase.getAmount());
		transaction.setType(TransactionType.CANCEL_PURCHASE);
		transaction.setCanceledTransactionId(purchase.getId());
		transactionRepository.save(transaction);
		walletService.updateWalletBalance(walletId, purchase.getAmount());
		return transaction;
	}
}
