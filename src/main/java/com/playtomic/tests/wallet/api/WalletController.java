package com.playtomic.tests.wallet.api;

import java.util.HashMap;
import java.util.Map;

import com.playtomic.tests.wallet.model.TransactionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.playtomic.tests.wallet.entity.Transaction;
import com.playtomic.tests.wallet.entity.Wallet;
import com.playtomic.tests.wallet.model.TransactionRequest;
import com.playtomic.tests.wallet.model.WalletRequest;
import com.playtomic.tests.wallet.model.WalletResponse;
import com.playtomic.tests.wallet.service.TransactionService;
import com.playtomic.tests.wallet.service.WalletService;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {
	private final Logger log = LoggerFactory.getLogger(WalletController.class);

	@Autowired
	WalletService walletService;

	@Autowired
	TransactionService transactionService;

	@GetMapping("/")
	public ResponseEntity<String> check() {
		log.info("Logging from /");
		return ResponseEntity.ok("OK");
	}

	@PostMapping
	public ResponseEntity<Wallet> createNewWallet(@RequestBody WalletRequest reqWallet) {
		Wallet wallet = new Wallet();
		wallet.setName(reqWallet.getName());
		return new ResponseEntity<Wallet>(wallet, HttpStatus.CREATED);
	}

	@GetMapping("/{id}/balance")
	public ResponseEntity<WalletResponse> getWalletBalance(@PathVariable("id") Long id) {
		
		return ResponseEntity.ok(new WalletResponse(id, null, walletService.getWalletById(id).getBalance()));
	}

	@GetMapping("{id}/transactions")
	public ResponseEntity<Map<String, Object>> getBookById(@PathVariable("id") Long id,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		Page<Transaction> pages = transactionService.getTransactionHistory(id, page, size);
		if (pages.getContent().isEmpty()) {
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}

		Map<String, Object> response = new HashMap<String, Object>();
		response.put("transactions", pages.getContent());
		response.put("currentPage", pages.getNumber());
		response.put("totalItems", pages.getTotalElements());
		response.put("totalPages", pages.getTotalPages());

		return ResponseEntity.ok(response);
	}

	@PostMapping("{id}/charge")
	public ResponseEntity<TransactionResponse> topUp(@PathVariable("id") Long id, @RequestBody TransactionRequest transactionRequest) {
		Transaction transaction = transactionService.topUp(id, transactionRequest);
		return ResponseEntity.ok(new TransactionResponse(transaction.getId(), transaction.getType(), transaction.getAmount()));
	}
	@PostMapping("{id}/refund/{transactionId}")
	public ResponseEntity<TransactionResponse> refund(@PathVariable("id") Long id, @PathVariable("transactionId") Long transactionId) {
		Transaction transaction = transactionService.refund(id, transactionId);
		return ResponseEntity.ok(new TransactionResponse(transaction.getId(), transaction.getType(), transaction.getAmount()));
	}
	@PostMapping("{id}/purchase")
	public ResponseEntity<TransactionResponse> purchase(@PathVariable("id") Long id, @RequestBody TransactionRequest transactionRequest) {
		Transaction transaction = transactionService.purchase(id, transactionRequest);
		return ResponseEntity.ok(new TransactionResponse(transaction.getId(), transaction.getType(), transaction.getAmount()));
	}
	@PostMapping("{id}/cancelPurchase/{transactionId}")
	public ResponseEntity<TransactionResponse> cancelPurchase(@PathVariable("id") Long id, @PathVariable("transactionId") Long transactionId) {
		Transaction transaction = transactionService.cancelPurchase(id, transactionId);
		return ResponseEntity.ok(new TransactionResponse(transaction.getId(), transaction.getType(), transaction.getAmount()));
	}
}
