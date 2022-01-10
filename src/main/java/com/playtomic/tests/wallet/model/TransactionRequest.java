package com.playtomic.tests.wallet.model;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class TransactionRequest {
	private Long transactionId;
	private BigDecimal amount;
	private String creditCardNumber;
	private String paymentId;
}
