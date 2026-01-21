package com.nordstrom.finance.dataintegration.metric;

public enum MetricErrorCode {
  SDM_RESTAURANT_EVENTS_CONSUMPTION_ERROR("sdmRestaurantEventsConsumptionError"),
  SDM_MARKETPLACE_EVENTS_CONSUMPTION_ERROR("sdmMarketplaceEventsConsumptionError"),
  SAVE_TRANSACTIONS_ERROR_COUNT("saveTransactionToDBError"),
  FIND_DUPLICATE_SDM_ID_DB_ERROR("FindDuplicateSdmIdDbError");

  private final String name;

  public String getErrorValue() {
    return name;
  }

  MetricErrorCode(String name) {
    this.name = name;
  }
}
