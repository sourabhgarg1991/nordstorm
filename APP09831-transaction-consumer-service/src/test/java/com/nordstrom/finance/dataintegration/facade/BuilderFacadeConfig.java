package com.nordstrom.finance.dataintegration.facade;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;

public class BuilderFacadeConfig {
  public static final Instant TESTING_INSTANT = Instant.parse("2024-05-25T10:15:30.00Z");
  public static final LocalDate TESTING_LOCAL_DATE = Date.valueOf("2024-05-25").toLocalDate();
  public static final String DEPARTMENT_DEFAULT = "0711";
  public static final String ITEM_CLASS_DEFAULT = "";
  public static final String TENDER_TYPE_DEFAULT = "CREDIT_CARD";
  public static final String CARD_TYPE_DEFAULT = "VISA";
  public static final String CARD_SUB_TYPE_DEFAULT = "";
  public static final String STORE_DEFAULT = "0100";
  // the idea around this config file initially was - give a way to generate always unique
  // identifiers etc., in order to spread these facades to integration and load tests
}
