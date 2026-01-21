package com.nordstrom.finance.dataintegration.fortknox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.nordstrom.event.secure.DataClassification;
import com.nordstrom.event.secure.TokenizedValue;
import com.nordstrom.finance.dataintegration.common.util.MoneyUtility;
import com.nordstrom.finance.dataintegration.fortknox.config.FortKnoxProperties;
import com.nordstrom.finance.dataintegration.fortknox.exception.FortKnoxRetryableException;
import com.nordstrom.finance.dataintegration.fortknox.exception.FortknoxException;
import com.nordstrom.finance.dataintegration.fortknox.model.FortknoxRedemptionResponse;
import com.nordstrom.standard.CurrencyCodeV2;
import com.nordstrom.standard.TokenizedMoneyV2;
import java.math.BigDecimal;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

class FortKnoxRedemptionServiceTest {

  private final FortKnoxProperties fortKnoxProperties = mock(FortKnoxProperties.class);
  private final WebClient webClientMock = mock(WebClient.class);
  private final RetryBackoffSpec retryBackoffSpec =
      Retry.backoff(3, java.time.Duration.ofMillis(10));

  private final FortKnoxRedemptionService fortKnoxRedemptionService =
      new FortKnoxRedemptionService(fortKnoxProperties, webClientMock, retryBackoffSpec);

  private final WebClient.RequestBodyUriSpec requestBodyUriSpecMock =
      mock(WebClient.RequestBodyUriSpec.class);
  private final WebClient.RequestBodySpec requestBodySpecMock =
      mock(WebClient.RequestBodySpec.class);
  private final WebClient.RequestHeadersSpec requestHeadersSpecMock =
      mock(WebClient.RequestHeadersSpec.class);
  private final WebClient.ResponseSpec responseSpecMock = mock(WebClient.ResponseSpec.class);
  FortknoxRedemptionResponse fortknoxRedemptionResponse =
      new FortknoxRedemptionResponse("requestId", "authority", "redeemedValue");

  @BeforeEach
  void setUp() {
    when(webClientMock.post()).thenReturn(requestBodyUriSpecMock);
    when(requestBodyUriSpecMock.uri((URI) any())).thenReturn(requestBodySpecMock);
    when(requestBodySpecMock.contentType(any())).thenReturn(requestBodySpecMock);
    when(requestBodySpecMock.body(any())).thenReturn(requestHeadersSpecMock);
    when(requestBodySpecMock.header(anyString(), anyString())).thenReturn(requestBodySpecMock);
    when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpecMock);
    when(responseSpecMock.onStatus(any(), any())).thenReturn(responseSpecMock);
  }

  @Test
  void testGetRedeemedString_Success() throws FortknoxException {
    when(fortKnoxProperties.getUrl()).thenReturn("http://mock-url");
    when(responseSpecMock.bodyToMono(FortknoxRedemptionResponse.class))
        .thenReturn(Mono.just(fortknoxRedemptionResponse));
    String result = fortKnoxRedemptionService.getRedeemedString("token", "authority");

    assertEquals("redeemedValue", result);
    verify(webClientMock, times(1)).post();
  }

  @Test
  void testRedeemAndGetFullAmount_Success() throws FortknoxException {
    when(fortKnoxProperties.getUrl()).thenReturn("http://mock-url");
    AtomicInteger counter = new AtomicInteger();
    when(responseSpecMock.bodyToMono(FortknoxRedemptionResponse.class))
        .thenAnswer(
            inv -> {
              int n = counter.incrementAndGet();
              if (n == 1)
                return (Mono.just(new FortknoxRedemptionResponse("requestId1", "authority", "10")));
              if (n == 2)
                return (Mono.just(
                    new FortknoxRedemptionResponse("requestId2", "authority", "500000000")));
              throw new IllegalStateException("too many calls");
            });
    BigDecimal result = fortKnoxRedemptionService.redeemAndGetFullAmount(getTokenizedMoney());
    assertEquals(MoneyUtility.getAmount("10", "500000000"), result);
  }

  @Test
  void testGetRedeemedString_Failure() {
    when(responseSpecMock.bodyToMono(FortknoxRedemptionResponse.class))
        .thenReturn(Mono.error(new FortKnoxRetryableException("Retryable error")));
    when(fortKnoxProperties.getUrl()).thenReturn("http://mock-url");

    assertThrows(
        FortknoxException.class,
        () -> fortKnoxRedemptionService.getRedeemedString("token", "authority"));
  }

  private TokenizedMoneyV2 getTokenizedMoney() {
    return TokenizedMoneyV2.newBuilder()
        .setUnits(
            TokenizedValue.newBuilder()
                .setValue("amountUnits")
                .setAuthority("GL")
                .setDataClassification(DataClassification.RESTRICTED)
                .build())
        .setNanos(
            TokenizedValue.newBuilder()
                .setValue("amountNanos")
                .setAuthority("GL")
                .setDataClassification(DataClassification.RESTRICTED)
                .build())
        .setCurrencyCode(CurrencyCodeV2.USD)
        .build();
  }
}
