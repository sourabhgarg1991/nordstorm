package com.nordstrom.finance.dataintegration.ertm.mapper;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.ertm.consumer.model.RetailTransactionLineDTO;
import com.nordstrom.finance.dataintegration.ertm.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.ertm.metric.MetricErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionMapper {
  private static final String SOURCE_REFERENCE_SYSTEM_ERTM = "ertm";
  private static final String SOURCE_REFERENCE_TYPE_RETAIL = "retail";

  private final MetricsClient metricsClient;

  public Transaction mapRecordToTransaction(RetailTransactionLineDTO record) {
    Transaction transaction = new Transaction();
    try {
      transaction =
          Transaction.builder()
              .sourceReferenceTransactionId(record.getSourceReferenceTransactionId())
              .transactionDate(record.getTransactionDate())
              .businessDate(record.getBusinessDate())
              .sourceProcessedDate(record.getSourceProcessedDate())
              .transactionType(record.getTransactionType())
              .transactionReversalCode(record.getTransactionReversalCode())
              .sourceReferenceSystemType(SOURCE_REFERENCE_SYSTEM_ERTM)
              .sourceReferenceType(SOURCE_REFERENCE_TYPE_RETAIL)
              .build();
    } catch (Exception ex) {
      log.error("Error mapping transaction line dto to entity. {}", ex.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(
              (MetricErrorCode.ENTITY_MAPPING_ERROR.getErrorValue())));
      throw ex;
    }
    return transaction;
  }
}
