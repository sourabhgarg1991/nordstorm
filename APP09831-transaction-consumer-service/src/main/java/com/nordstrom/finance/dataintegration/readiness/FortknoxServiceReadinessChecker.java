package com.nordstrom.finance.dataintegration.readiness;

import com.nordstrom.finance.dataintegration.fortknox.FortKnoxRedemptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FortknoxServiceReadinessChecker implements ExternalServiceReadinessChecker {
  static final String AUTHORITY_GENERAL_LEDGER = "GeneralLedger";

  @Value("${app.fortknox.redeem}")
  boolean fortknoxRedeemEnabled;

  @Value("${fortknox.redemption.readiness.check.token}")
  String SAMPLE_TOKEN;

  @Autowired FortKnoxRedemptionService fortKnoxRedemptionService;

  @Override
  public boolean isServiceReady() {
    try {
      if (fortknoxRedeemEnabled) {
        fortKnoxRedemptionService.getRedeemedString(SAMPLE_TOKEN, AUTHORITY_GENERAL_LEDGER);
        return true;
      } else {
        log.info("Fortknox redemption is disabled, skipping readiness check.");
        return true;
      }
    } catch (Exception e) {
      log.error("Fortknox service is not available. Error : {}", e.getMessage());
    }
    return false;
  }
}
