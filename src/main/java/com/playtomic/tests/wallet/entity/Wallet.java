package com.playtomic.tests.wallet.entity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Version;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Data;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
public class Wallet {
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
	
    @CreatedDate
    @Column(name = "created_date")
    private Date createdTime;

    @LastModifiedDate
    @Column(name = "last_modified_date")
    private Date lastModifiedTime;
	
    @Column(nullable = false)
    private String name;
    
	private BigDecimal balance = BigDecimal.ZERO;

	private transient List<Transaction> transactions;
	
	@Version
	private Integer version;
}
