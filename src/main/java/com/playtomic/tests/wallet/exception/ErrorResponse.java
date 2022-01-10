package com.playtomic.tests.wallet.exception;

import lombok.Data;

@Data
public class ErrorResponse {
	private int errorCode;
	private String message;
}
