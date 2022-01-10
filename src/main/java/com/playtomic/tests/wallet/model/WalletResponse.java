package com.playtomic.tests.wallet.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class WalletResponse {
	private Long id;
	private String name;
	private BigDecimal balance;
}
