package com.playtomic.tests.wallet.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.playtomic.tests.wallet.entity.Wallet;

public interface WalletRepository extends JpaRepository<Wallet, Long>{

}
