package com.nordstrom.finance.dataintegration.ertm.readiness;

import com.nordstrom.finance.dataintegration.ertm.database.service.TransactionDBService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"!integrationTest & !loadTest"})
public class AuroraDBReadinessChecker implements ExternalServiceReadinessChecker {
  @Autowired TransactionDBService transactionDBService;

  @Override
  public boolean isServiceReady() {
    return transactionDBService.isReady();
  }
}
