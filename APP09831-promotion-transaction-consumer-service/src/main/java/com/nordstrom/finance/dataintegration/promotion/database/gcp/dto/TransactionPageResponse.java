package com.nordstrom.finance.dataintegration.promotion.database.gcp.dto;

import com.google.cloud.bigquery.Job;
import com.nordstrom.finance.dataintegration.promotion.domain.model.TransactionDetailVO;
import java.util.List;
import org.springframework.util.StringUtils;

/** Represents a response containing a specific page of transaction data */
public record TransactionPageResponse(
    Job job, List<TransactionDetailVO> currentPageTransactions, String nextPageToken) {

  /**
   * Checks if there is a next page of transactions
   *
   * @return true if there is a next page, false otherwise
   */
  public boolean hasNext() {
    return StringUtils.hasText(nextPageToken);
  }
}
