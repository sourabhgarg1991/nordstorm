package com.nordstrom.finance.dataintegration.fortknox;

import com.nordstrom.finance.dataintegration.common.util.MoneyUtility;
import com.nordstrom.finance.dataintegration.fortknox.config.FortKnoxProperties;
import com.nordstrom.finance.dataintegration.fortknox.exception.FortKnoxRetryableException;
import com.nordstrom.finance.dataintegration.fortknox.exception.FortknoxException;
import com.nordstrom.finance.dataintegration.fortknox.model.FortknoxRedemptionRequest;
import com.nordstrom.finance.dataintegration.fortknox.model.FortknoxRedemptionResponse;
import com.nordstrom.standard.TokenizedMoneyV2;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.RetryBackoffSpec;

/**
 * Service class responsible for handling the redemption of tokens. This class interacts with the
 * FortKnox Redemption API to convert tokens back to their original values.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FortKnoxRedemptionService {

  private final FortKnoxProperties fortKnoxProperties;
  private final WebClient fortknoxWebClient;
  private final RetryBackoffSpec fortknoxRetryBackoffSpec;

  @Value("${applicationId}")
  String APP_ID;

  private static final String HEADER_REQUEST_ID = "Nord-Request-Id";
  private static final String HEADER_CLIENT_ID = "Nord-Client-Id";

  /**
   * Method to redeem tokenized value
   *
   * @param tokenizedValue tokenized String
   * @param authority authority used for tokenization
   * @return redeemed String
   * @throws FortknoxException e
   */
  public String getRedeemedString(@NonNull String tokenizedValue, @NonNull String authority)
      throws FortknoxException {
    FortknoxRedemptionRequest fortknoxRedemptionRequest =
        buildFortknoxRedemptionRequest(tokenizedValue, authority);
    return redeem(fortknoxRedemptionRequest).getSde();
  }

  /**
   * Method redeems units and nanos separately. These redeemed units & nanos are concatenates with
   * dot to get full amount.
   *
   * @param money TokenizedMoney
   * @return full amount as BigDecimal
   * @throws FortknoxException e
   */
  public BigDecimal redeemAndGetFullAmount(TokenizedMoneyV2 money) throws FortknoxException {
    return MoneyUtility.getAmount(
        getRedeemedString(
            String.valueOf(money.getUnits().getValue()),
            String.valueOf(money.getUnits().getAuthority())),
        getRedeemedString(
            String.valueOf(money.getNanos().getValue()),
            String.valueOf(money.getNanos().getAuthority())));
  }

  private FortknoxRedemptionRequest buildFortknoxRedemptionRequest(
      String tokenizedValue, String authority) {
    FortknoxRedemptionRequest fortknoxTokenizerRequest = new FortknoxRedemptionRequest();
    fortknoxTokenizerRequest.setRequestId(UUID.randomUUID().toString());
    fortknoxTokenizerRequest.setAuthority(authority);
    fortknoxTokenizerRequest.setToken(tokenizedValue);
    return fortknoxTokenizerRequest;
  }

  /**
   * Method to redeem a token using the Redemption Rest API.
   *
   * @param fortknoxRedemptionRequest FortknoxRedemptionRequest object with all information needed
   *     for de-tokenization.
   * @return FortknoxRedemptionResponse with redeemed value.
   */
  private FortknoxRedemptionResponse redeem(FortknoxRedemptionRequest fortknoxRedemptionRequest)
      throws FortknoxException {
    try {
      long startTime = System.currentTimeMillis();

      FortknoxRedemptionResponse fortknoxRedemptionResponse =
          fortknoxWebClient
              .post()
              .uri(URI.create(fortKnoxProperties.getUrl()))
              .contentType(MediaType.APPLICATION_JSON)
              .header(HEADER_REQUEST_ID, fortknoxRedemptionRequest.getRequestId())
              .header(HEADER_CLIENT_ID, StringUtils.hasText(APP_ID) ? APP_ID : "APP09831")
              .body(BodyInserters.fromValue(fortknoxRedemptionRequest))
              .retrieve()
              .onStatus(
                  status ->
                      status.is5xxServerError()
                          || status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)
                          || status.isSameCodeAs(HttpStatus.NOT_FOUND),
                  response -> {
                    int statusCode = response.statusCode().value();
                    String reasonPhrase =
                        HttpStatus.valueOf(response.statusCode().value()).getReasonPhrase();
                    log.error(
                        "Received error response from Redemption Service: HTTP Status Code {} ({})",
                        statusCode,
                        reasonPhrase);
                    return Mono.error(
                        new FortKnoxRetryableException(
                            String.format(
                                "Redemption Service API response: HTTP Status Code %d (%s).",
                                statusCode, reasonPhrase)));
                  })
              .bodyToMono(FortknoxRedemptionResponse.class)
              .retryWhen(
                  fortknoxRetryBackoffSpec
                      .filter(
                          throwable -> {
                            boolean shouldRetry =
                                throwable instanceof FortKnoxRetryableException
                                    || throwable instanceof IOException
                                    || throwable instanceof TimeoutException;

                            if (shouldRetry) {
                              log.info(
                                  "API call to Redemption Service failed due to retryable exception: {}",
                                  throwable.toString());
                            } else {
                              log.warn(
                                  "API call to Redemption Service failed due to non-retryable exception: {}",
                                  throwable.toString());
                            }

                            return shouldRetry;
                          })
                      .doBeforeRetry(
                          retrySignal -> {
                            log.info(
                                "Retrying API call to Redemption Service, attempt #{} out of {}. Reason: {}",
                                (retrySignal.totalRetries() + 1),
                                fortknoxRetryBackoffSpec.maxAttempts,
                                retrySignal.failure().getMessage());
                          })
                      .onRetryExhaustedThrow(
                          (retryBackoffSpec, retrySignal) -> {
                            log.error(
                                "API call to Redemption Service failed after {} retry attempts: {}",
                                fortknoxRetryBackoffSpec.maxAttempts,
                                retrySignal.failure().getMessage());
                            throw new RuntimeException(
                                String.format(
                                    "API call to Redemption Service failed after %d retry attempts: %s",
                                    fortknoxRetryBackoffSpec.maxAttempts,
                                    retrySignal.failure().getMessage()));
                          }))
              .block();

      long endTime = System.currentTimeMillis();
      log.trace("Received response from Redemption Service in {} ms", (endTime - startTime));

      return fortknoxRedemptionResponse;
    } catch (Exception e) {
      log.error("API call to Redemption Service failed", e);
      throw new FortknoxException("API call to Redemption Service failed.");
    }
  }
}
