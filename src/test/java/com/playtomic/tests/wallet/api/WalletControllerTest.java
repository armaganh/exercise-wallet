package com.playtomic.tests.wallet.api;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import com.playtomic.tests.wallet.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playtomic.tests.wallet.entity.Wallet;
import com.playtomic.tests.wallet.exception.StripeAmountTooSmallException;
import com.playtomic.tests.wallet.model.TransactionRequest;
import com.playtomic.tests.wallet.model.WalletRequest;
import com.playtomic.tests.wallet.service.StripeService;
import com.playtomic.tests.wallet.service.TransactionService;
import com.playtomic.tests.wallet.service.WalletService;

@SpringBootTest
@EnableJpaAuditing
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "test")
public class WalletControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

	@Autowired
	TransactionService transactionService;

	@Autowired
	WalletService walletService;

	@MockBean
	StripeService stripeService;

	Wallet wallet = new Wallet();

	@BeforeEach
	public void setUp() {
		doNothing().when(stripeService).charge(Mockito.anyString(), Mockito.any());
		doNothing().when(stripeService).refund(Mockito.anyString());
		doThrow(new StripeAmountTooSmallException()).when(stripeService).charge(Mockito.anyString(),
				Mockito.eq(BigDecimal.valueOf(5)));
		
		wallet.setName("Test");
		wallet = walletService.createWallet(wallet);
	}

  @Test
  public void postingNewWalletCreateANewWallet() throws Exception {

	  WalletRequest walletRequestCaptor = new WalletRequest();
	  walletRequestCaptor.setName("Test2");


    this.mockMvc
      .perform(post("/api/wallets")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(walletRequestCaptor)))
      .andExpect(status().isCreated());

  }
  
  @Test
  public void getBalance() throws Exception {

	    this.mockMvc
	      .perform(get("/api/wallets/"+wallet.getId() +"/balance"))
	      .andExpect(status().isOk())
	      .andExpect(content().contentType("application/json"))
	      .andExpect(jsonPath("$.id", is(wallet.getId().intValue())))
	      .andExpect(jsonPath("$.balance", is(0.00)));

  }

	@Test
	public void getBalanceNoFoundWallet() throws Exception {

		this.mockMvc
				.perform(get("/api/wallets/100212/balance"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.errorCode", is(1001)))
				.andExpect(jsonPath("$.message", is("No data found!")));

	}

	@Test
	public void getTransactionHistory() throws Exception {
	    TransactionRequest transactionRequest = new TransactionRequest();
		transactionRequest.setAmount(BigDecimal.valueOf(150));
		transactionService.topUp(wallet.getId(), transactionRequest);
		transactionRequest.setAmount(BigDecimal.valueOf(50));
		transactionService.purchase(wallet.getId(), transactionRequest);
		this.mockMvc
				.perform(get("/api/wallets/"+wallet.getId() +"/transactions"))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.transactions", hasSize(2)))
				.andExpect(jsonPath("$.currentPage", is(0)))
				.andExpect(jsonPath("$.totalItems", is(2)))
				.andExpect(jsonPath("$.totalPages", is(1)));
	}
	@Test
	public void getTransactionHistoryNoContent() throws Exception {
		this.mockMvc
				.perform(get("/api/wallets/"+wallet.getId() +"/transactions"))
				.andExpect(status().isNoContent());
	}

	@Test
	public void topUpSuccessfully() throws Exception {
		TransactionRequest transactionRequest = new TransactionRequest();
		transactionRequest.setAmount(BigDecimal.valueOf(100));
		transactionRequest.setCreditCardNumber("424242424242424");

		this.mockMvc
				.perform(post("/api/wallets/"+wallet.getId()+"/charge")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(transactionRequest)))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.type", is("CHARGE")))
				.andExpect(jsonPath("$.amount",  is(100)));
	}

	@Test
	public void refundSuccessfully() throws Exception {
		TransactionRequest transactionRequest = new TransactionRequest();
		transactionRequest.setAmount(BigDecimal.valueOf(150));
		Transaction transaction = transactionService.topUp(wallet.getId(), transactionRequest);


		this.mockMvc
				.perform(post("/api/wallets/"+wallet.getId()+"/refund/" + transaction.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(transactionRequest)))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.type", is("REFUND")))
				.andExpect(jsonPath("$.amount",  is(150.0)));
	}
	@Test
	public void purchaseSuccessfully() throws Exception {
		TransactionRequest transactionRequest = new TransactionRequest();
		transactionRequest.setAmount(BigDecimal.valueOf(150));
		transactionService.topUp(wallet.getId(), transactionRequest);

		transactionRequest = new TransactionRequest();
		transactionRequest.setAmount(BigDecimal.valueOf(100));

		this.mockMvc
				.perform(post("/api/wallets/"+wallet.getId()+"/purchase")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(transactionRequest)))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.type", is("PURCHASE")))
				.andExpect(jsonPath("$.amount",  is(100)));
	}

	@Test
	public void cancelPurchaseSuccessfully() throws Exception {
		TransactionRequest transactionRequest = new TransactionRequest();
		transactionRequest.setAmount(BigDecimal.valueOf(150));
		transactionService.topUp(wallet.getId(), transactionRequest);
		transactionRequest.setAmount(BigDecimal.valueOf(50));
	    Transaction transaction =  transactionService.purchase(wallet.getId(), transactionRequest);

		transactionRequest = new TransactionRequest();
		transactionRequest.setAmount(BigDecimal.valueOf(100));

		this.mockMvc
				.perform(post("/api/wallets/"+wallet.getId()+"/cancelPurchase/" + transaction.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(transactionRequest)))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.id", notNullValue()))
				.andExpect(jsonPath("$.type", is("CANCEL_PURCHASE")))
				.andExpect(jsonPath("$.amount",  is(50.0)));
	}

	@Test
	public void cancelPurchaseAlreadyCancelPurchaseError() throws Exception {
		TransactionRequest transactionRequest = new TransactionRequest();
		transactionRequest.setAmount(BigDecimal.valueOf(150));
		transactionService.topUp(wallet.getId(), transactionRequest);
		transactionRequest.setAmount(BigDecimal.valueOf(50));
		Transaction transaction =  transactionService.purchase(wallet.getId(), transactionRequest);

		transactionService.cancelPurchase(wallet.getId(), transaction.getId());

		transactionRequest = new TransactionRequest();
		transactionRequest.setAmount(BigDecimal.valueOf(100));

		this.mockMvc
				.perform(post("/api/wallets/"+wallet.getId()+"/cancelPurchase/" + transaction.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(transactionRequest)))
				.andExpect(status().isOk())
				.andExpect(content().contentType("application/json"))
				.andExpect(jsonPath("$.errorCode", is(1003)))
				.andExpect(jsonPath("$.message", is("Transaction already has been cancelled")));
	}
}
