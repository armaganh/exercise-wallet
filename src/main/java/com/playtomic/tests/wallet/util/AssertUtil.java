package com.playtomic.tests.wallet.util;

import org.springframework.http.HttpStatus;

import com.playtomic.tests.wallet.exception.PlaytomicException;

public class AssertUtil {
	public static void isTrue(boolean expression, Integer errorCode, String message, HttpStatus status) {
		if (!expression) {
			throw new PlaytomicException(errorCode, message, status == null ? HttpStatus.OK : status);
		}
	}
}
