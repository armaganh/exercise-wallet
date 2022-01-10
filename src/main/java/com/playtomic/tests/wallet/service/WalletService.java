package com.playtomic.tests.wallet.service;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.playtomic.tests.wallet.entity.Wallet;
import com.playtomic.tests.wallet.exception.PlaytomicException;
import com.playtomic.tests.wallet.repository.WalletRepository;

/**
 * This is Wallet service and the wallet CRUD operations class. Now just implemented Create Wallet, get Wallet and Update balance services are
 * implemented.
 * */
@Service
public class WalletService {
	private final Logger log = LoggerFactory.getLogger(WalletService.class);

	@Autowired
	WalletRepository walletRepository;

	public Wallet createWallet(Wallet wallet) {
		return walletRepository.save(wallet);
	}

	public Wallet getWalletById(Long id) {
		return walletRepository.findById(id)
				.orElseThrow(() -> new PlaytomicException(1001, "No data found!", HttpStatus.OK));
	}

	public void updateWalletBalance(Long walletId, BigDecimal value) {
		log.info("{} added for {} wallet", value, walletId);
		walletRepository.findById(walletId).ifPresentOrElse(o -> {
			if(BigDecimal.ZERO.compareTo(o.getBalance().add(value)) > 0) {
				throw new PlaytomicException(1002, "Insufficient Fund", HttpStatus.OK);
			}
			o.setBalance(o.getBalance().add(value));
			walletRepository.save(o);
		}, () -> new PlaytomicException(1001, "No data found!", HttpStatus.OK));
	}
}
