package com.playtomic.tests.wallet.entity;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.playtomic.tests.wallet.enums.TransactionStatus;
import com.playtomic.tests.wallet.enums.TransactionType;

import lombok.Data;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Transaction {
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;
	
    @CreatedDate
    @Column(name = "created_date")
    private Date createdTime;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Date lastModifiedTime;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TransactionType type;
	
	@Column(nullable = false)
	private BigDecimal amount;

	@Column(name = "payment_id")
	private String paymentId;
	
	@Column(name="canceled_transaction_id")
	private Long canceledTransactionId;
	
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TransactionStatus status = TransactionStatus.ACTIVE;
}
