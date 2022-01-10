package com.playtomic.tests.wallet.exception;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class PlaytomicException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6700281683021544857L;

	private int errorCode;
	private String errorMessage;
	private HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
	
	public PlaytomicException() {
		super();
	}
	public PlaytomicException(int errorCode, String errorMessage, HttpStatus status) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.status = status == null ?  HttpStatus.INTERNAL_SERVER_ERROR : status;
	}
}
