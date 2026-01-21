package com.nordstrom.finance.dataintegration.promotion.database.gcp.dto;

import com.google.cloud.bigquery.Job;

/** Represents a request for a specific page of transaction data */
public record TransactionPageRequest(Job job, String currentPageToken) {

  /**
   * Creates a TransactionPageRequest for the first page without existing job or token to reference
   *
   * @return a TransactionPageRequest for the first page
   */
  public static TransactionPageRequest firstPage() {
    return new TransactionPageRequest(null, null);
  }

  /**
   * Creates a TransactionPageRequest for the next page based on the previous response
   *
   * @param previousResponse the previous transaction page response
   * @return a TransactionPageRequest for the next page
   */
  public static TransactionPageRequest nextPage(TransactionPageResponse previousResponse) {
    return new TransactionPageRequest(previousResponse.job(), previousResponse.nextPageToken());
  }
}
