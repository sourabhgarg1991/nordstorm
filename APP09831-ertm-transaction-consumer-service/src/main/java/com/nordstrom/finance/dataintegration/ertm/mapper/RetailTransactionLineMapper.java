package com.nordstrom.finance.dataintegration.ertm.mapper;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.common.metric.MetricsCommonTag;
import com.nordstrom.finance.dataintegration.common.util.StringFormatUtility;
import com.nordstrom.finance.dataintegration.ertm.consumer.model.RetailTransactionLineDTO;
import com.nordstrom.finance.dataintegration.ertm.database.entity.RetailTransactionLine;
import com.nordstrom.finance.dataintegration.ertm.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.ertm.database.entity.TransactionLine;
import com.nordstrom.finance.dataintegration.ertm.metric.MetricErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetailTransactionLineMapper {

  private final TransactionLineMapper transactionLineMapper;
  private final MetricsClient metricsClient;

  public RetailTransactionLine mapRecordToRetailTransactionLine(
      RetailTransactionLineDTO record, Transaction transaction) {
    RetailTransactionLine retailTransactionLine = new RetailTransactionLine();
    try {
      retailTransactionLine =
          RetailTransactionLine.builder()
              .departmentId(StringFormatUtility.toFourDigitFormat(record.getDepartmentId()))
              .classId(StringFormatUtility.toFourDigitFormat(record.getClassId()))
              .feeCode(StringFormatUtility.toFourDigitFormat(record.getFeeCode()))
              .cashDisbursementLine1(record.getCashDisbursementLine1())
              .cashDisbursementLine2(record.getCashDisbursementLine2())
              .fulfillmentTypeDropshipCode(record.getFulfillmentTypeDropshipCode())
              .waivedReasonCode(record.getWaivedReasonCode())
              .lineItemAmount(record.getLineItemAmount())
              .taxAmount(record.getTaxAmount())
              .employeeDiscountAmount(record.getEmployeeDiscountAmount())
              .waivedAmount(record.getWaivedAmount())
              .tenderType(record.getTenderType())
              .tenderCardTypeCode(record.getTenderCardType())
              .tenderCardSubTypeCode(record.getTenderCardSubType())
              .tenderActivityCode(record.getTenderAdjustmentCode())
              .tenderAmount(record.getTenderAmount())
              .build();

      TransactionLine transactionLine =
          transactionLineMapper.mapRecordToTransactionLine(record, transaction);
      retailTransactionLine.setTransactionLine(transactionLine);
      transactionLine.setRetailTransactionLine(retailTransactionLine);
    } catch (Exception ex) {
      log.error("Error mapping retail transaction line dto to entity. {}", ex.getMessage());
      metricsClient.incrementErrorCount(
          MetricsCommonTag.ERROR_CODE.getTag(
              (MetricErrorCode.ENTITY_MAPPING_ERROR.getErrorValue())));
      throw ex;
    }
    return retailTransactionLine;
  }
}
