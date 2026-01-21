package com.nordstrom.finance.dataintegration.fortknox.config;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.time.Duration;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.ResourceUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

@Slf4j
@Configuration
public class FortKnoxWebClientConfig {
  @Autowired private FortKnoxProperties fortknoxProperties;

  @Bean
  public RetryBackoffSpec fortknoxRetryBackoffSpec() {
    return Retry.backoff(
        fortknoxProperties.getMaxAttempts(), fortknoxProperties.getMinBackoffInSeconds());
  }

  @Bean("fortknoxWebClient")
  @ConditionalOnProperty(
      value = "fortknox.tls.enabled",
      havingValue = "true",
      matchIfMissing = true)
  public WebClient fortknoxTLSWebClient() {
    log.info("--------- Initializing FortKnox TLS WebClient ---------");
    return buildWebClient();
  }

  @Bean("fortknoxWebClient")
  @ConditionalOnProperty(value = "fortknox.tls.enabled", havingValue = "false")
  public WebClient fortknoxWebClient() {
    log.info("--------- Initializing FortKnox Non-TLS DEFAULT WebClient ---------");
    return WebClient.builder().build();
  }

  private WebClient buildWebClient() {
    SslContext sslContext = getSSLContext(fortknoxProperties);
    ConnectionProvider connectionProvider =
        ConnectionProvider.builder("custom")
            .maxIdleTime(Duration.ofSeconds(30)) // Idle timeout
            .maxLifeTime(Duration.ofMinutes(5)) // Max lifetime for a connection
            .evictInBackground(Duration.ofSeconds(60)) // How often to evict
            .build();
    HttpClient httpClient =
        HttpClient.create(connectionProvider)
            .responseTimeout(Duration.ofSeconds(60))
            .secure(sslSpec -> sslSpec.sslContext(sslContext));
    return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
  }

  private static SslContext getSSLContext(FortKnoxProperties fortknoxProperties) {
    try {
      KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
      keyStore.load(
          new FileInputStream(ResourceUtils.getFile(fortknoxProperties.getKeyStore())),
          fortknoxProperties.getKeyStorePassword().toCharArray());

      // initialize trust manager
      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(keyStore);

      KeyManagerFactory keyManagerFactory =
          KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keyStore, fortknoxProperties.getKeyStorePassword().toCharArray());
      log.info(
          "--------- initialize key manager. keyManagerFactory.init(...) called successfully ---------\r\n");

      return SslContextBuilder.forClient()
          .keyManager(keyManagerFactory)
          .trustManager(trustManagerFactory)
          .protocols("TLSv1.2")
          .build();
    } catch (Exception e) {
      log.error("Error creating TLS WebClient. Check key-store and/or key-store-password.", e);
    }
    return null;
  }
}
