package com.nordstrom.finance.dataintegration.ertm.mapper;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.common.util.StringFormatUtility;
import com.nordstrom.finance.dataintegration.ertm.consumer.model.RetailTransactionLineDTO;
import com.nordstrom.finance.dataintegration.ertm.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.ertm.database.entity.TransactionLine;
import com.nordstrom.finance.dataintegration.ertm.metric.MetricErrorCode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionLineMapper {
  private final TransactionMapper transactionMapper;
  private final MetricsClient metricsClient;

  public TransactionLine mapRecordToTransactionLine(
      RetailTransactionLineDTO record, Transaction transaction) {
    List<TransactionLine> transactionLines = new ArrayList<>();
    TransactionLine transactionLine = new TransactionLine();
    try {
      transactionLine =
          TransactionLine.builder()
              .ringingStore(StringFormatUtility.toFourDigitFormat(record.getRingingStore()))
              .sourceReferenceLineType(record.getSourceReferenceLineType())
              .sourceReferenceLineId(record.getSourceReferenceLineId())
              .transactionLineType(transactionLineTypeTransform(record.getTransactionLineType()))
              .storeOfIntent(StringFormatUtility.toFourDigitFormat(record.getStoreOfIntent()))
              .build();

      if (transaction == null || transaction.getSourceReferenceTransactionId() == null) {
        transaction = transactionMapper.mapRecordToTransaction(record);
      } else if (transaction.getTransactionLines() != null) {
        transactionLines = transaction.getTransactionLines();
      }
      transactionLine.setTransaction(transaction);
      transactionLines.add(transactionLine);
      transaction.setTransactionLines(transactionLines);
    } catch (Exception ex) {
      log.error("Error mapping transaction line dto to entity. {}", ex.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(
              (MetricErrorCode.ENTITY_MAPPING_ERROR.getErrorValue())));
      throw ex;
    }
    return transactionLine;
  }

  private static String transactionLineTypeTransform(String lineType) {
    if (lineType == null || lineType.isBlank()) return null;

    String normalizedLineType = lineType.trim().toUpperCase();
    return switch (normalizedLineType) {
      case "S" -> "SALE";
      case "R", "RETURN" -> "RETN";
      default -> normalizedLineType;
    };
  }
}
