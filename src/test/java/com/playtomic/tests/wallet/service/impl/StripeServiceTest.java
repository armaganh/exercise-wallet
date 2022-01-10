package com.playtomic.tests.wallet.service.impl;


import com.playtomic.tests.wallet.exception.StripeAmountTooSmallException;
import com.playtomic.tests.wallet.exception.StripeServiceException;
import com.playtomic.tests.wallet.service.StripeService;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.net.URI;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;


/**
 * This test is failing with the current implementation.
 *
 * How would you test this?
 */
@SpringBootTest
@ActiveProfiles(profiles = "test")
public class StripeServiceTest {

//    URI testUri = URI.create("http://localhost:12111");
//    StripeService s = new StripeService(testUri, testUri, new RestTemplateBuilder());
    @MockBean
	StripeService stripeService;
	
    @Test
    public void test_exception() {
            doThrow(new StripeAmountTooSmallException()).when(stripeService).charge(Mockito.anyString(),
                Mockito.eq(BigDecimal.valueOf(5)));
        Assertions.assertThrows(StripeAmountTooSmallException.class, () -> {
            stripeService.charge("4242 4242 4242 4242", new BigDecimal(5));
        });
    }

	@Test
    public void test_ok() throws StripeServiceException {
        doNothing().when(stripeService).charge(Mockito.anyString(), Mockito.any());
        stripeService.charge("4242 4242 4242 4242", new BigDecimal(15));
    }
}
