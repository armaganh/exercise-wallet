package com.playtomic.tests.wallet.repository.impl;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import com.playtomic.tests.wallet.entity.Wallet;
import com.playtomic.tests.wallet.repository.WalletRepository;

@SpringBootTest
@EnableJpaAuditing
@ActiveProfiles(profiles = "test")
public class WalletRepositoryTest {
	@Autowired
	WalletRepository walletRepository;

	@Test
	public void createWallet() {
		Wallet wallet = new Wallet();
		wallet.setName("Test");
		walletRepository.save(wallet);
		System.out.println(wallet);

		Assertions.assertEquals(wallet.getName(), "Test");
		Assertions.assertEquals(wallet.getBalance(), BigDecimal.ZERO);
		Assertions.assertNotNull(wallet.getId());
	}

	@Test
	public void throwExpetionforWalletNameEmpty() {
		Wallet wallet = new Wallet();
		Assertions.assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
			walletRepository.save(wallet);
		});
	}

	@Test
	public void optimisticLockException() {
		Wallet wallet = new Wallet();
		wallet.setName("Test");
		walletRepository.save(wallet);
		Wallet walletUpdated = walletRepository.findById(wallet.getId()).get();
		walletUpdated.setBalance(BigDecimal.TEN);
		walletRepository.save(wallet);
		walletUpdated.setBalance(walletUpdated.getBalance().add(BigDecimal.TEN));
		walletRepository.save(walletUpdated);
		wallet.setBalance(BigDecimal.ONE);
		Assertions.assertThrows(org.springframework.orm.ObjectOptimisticLockingFailureException.class, () -> {
			walletRepository.save(wallet);
		});
	}

	@Test
	public void updateBalance() {
		Wallet wallet = new Wallet();
		wallet.setName("Test");
		wallet = walletRepository.save(wallet);
		wallet.setBalance(BigDecimal.TEN);
		wallet = walletRepository.save(wallet);
		wallet.setBalance(wallet.getBalance().add(BigDecimal.TEN));
		walletRepository.save(wallet);
		Assertions.assertEquals(wallet.getBalance(), BigDecimal.valueOf(20));
	}
}
