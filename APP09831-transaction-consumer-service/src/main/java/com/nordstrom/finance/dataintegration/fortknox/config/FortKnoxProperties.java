package com.nordstrom.finance.dataintegration.fortknox.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.context.annotation.Configuration;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "fortknox.redemption")
public class FortKnoxProperties {

  private String url;
  private String keyStore;
  private String keyStorePassword;
  private Integer maxAttempts;

  @DurationUnit(ChronoUnit.SECONDS)
  private Duration minBackoffInSeconds;
}
