package com.playtomic.tests.wallet.model;

import com.playtomic.tests.wallet.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@AllArgsConstructor
@Data
public class TransactionResponse {
	private Long id;
	private TransactionType type;
	private BigDecimal amount;
}
