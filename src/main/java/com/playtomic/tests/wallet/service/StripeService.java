package com.playtomic.tests.wallet.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.playtomic.tests.wallet.exception.StripeRestTemplateResponseErrorHandler;
import com.playtomic.tests.wallet.exception.StripeServiceException;
import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;


/**
 * Handles the communication with Stripe.
 *
 * A real implementation would call to String using their API/SDK.
 * This dummy implementation throws an error when trying to charge less than 10€.
 */
@Service
public class StripeService {

    private final Logger log = LoggerFactory.getLogger(StripeService.class);

    @NonNull
    private final URI chargesUri;

    @NonNull
    private final URI refundsUri;

    @NonNull
    private final RestTemplate restTemplate;

    public StripeService(@Value("stripe.simulator.charges-uri") @NonNull URI chargesUri,
                         @Value("stripe.simulator.refunds-uri") @NonNull URI refundsUri,
                         @NotNull RestTemplateBuilder restTemplateBuilder) {
        this.chargesUri = chargesUri;
        this.refundsUri = refundsUri;
        this.restTemplate =
                restTemplateBuilder
                .errorHandler(new StripeRestTemplateResponseErrorHandler())
                .build();
    }

    /**
     * Charges money in the credit card.
     *
     * Ignore the fact that no CVC or expiration date are provided.
     *
     * @param creditCardNumber The number of the credit card
     * @param amount The amount that will be charged.
     *
     * @throws StripeServiceException
     */
    public void charge(@NonNull String creditCardNumber, @NonNull BigDecimal amount) throws StripeServiceException {
        log.debug("charge started");
        ChargeRequest body = new ChargeRequest(creditCardNumber, amount);
        // Object.class because we don't read the body here.
        restTemplate.postForObject(chargesUri, body, Object.class);
        log.debug("charge finished");
    }

    /**
     * Refunds the specified payment.
     */
    public void refund(@NonNull String paymentId) throws StripeServiceException {
        log.debug("refund started");
        log.info("Refund successful for %s paymentId", paymentId);
        // Object.class because we don't read the body here.
        restTemplate.postForEntity(chargesUri.toString(), null, Object.class, paymentId);
        log.debug("refund finished");
    }

    @AllArgsConstructor
    private static class ChargeRequest {

        @NonNull
        @JsonProperty("credit_card")
        String creditCardNumber;

        @NonNull
        @JsonProperty("amount")
        BigDecimal amount;
    }
}
