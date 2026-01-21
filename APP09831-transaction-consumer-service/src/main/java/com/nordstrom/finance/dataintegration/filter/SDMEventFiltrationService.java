package com.nordstrom.finance.dataintegration.filter;

import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_SYSTEM_TYPE_SDM;
import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_TYPE_MARKETPLACE;
import static com.nordstrom.finance.dataintegration.metric.Metric.SDM_EVENT_FILTER_COUNT;
import static com.nordstrom.finance.dataintegration.metric.MetricTag.FILTER_REASON;
import static com.nordstrom.finance.dataintegration.utility.MoneyUtility.getAmount;
import static java.math.BigDecimal.ZERO;

import com.nordstrom.customer.object.operational.FinancialRetailTransaction;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionAdjustmentReason;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionAuditActivityType;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionMerchandiseLineItem;
import com.nordstrom.customer.shared.AccountingNotAppliedReason;
import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.database.entity.FilteredTransaction;
import com.nordstrom.finance.dataintegration.database.service.TransactionDBService;
import com.nordstrom.finance.dataintegration.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.exception.DatabaseOperationException;
import com.nordstrom.standard.PartnerRelationshipType;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/** Service class for filtering SDM Kafka messages to determine its acceptability for accounting. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SDMEventFiltrationService {
  private final TransactionDBService transactionDBService;
  private final MetricsClient metricsClient;

  /**
   * Checks whether a Kafka message is acceptable for accounting.
   *
   * @param sdmFinancialRetailTransaction The Kafka message to be evaluated.
   * @return True if the message meets the criteria and should be processed, False otherwise (skips
   *     the message).
   */
  public boolean isSDMTransactionAcceptable(
      FinancialRetailTransaction sdmFinancialRetailTransaction)
      throws DatabaseOperationException, DatabaseConnectionException {
    if (!CollectionUtils.isEmpty(
        sdmFinancialRetailTransaction.getValidationResult().getFailureDetails())) {
      log.warn("SDM Event has validation results failure details, can not be consumed.");
      metricsClient.incrementCounter(
          SDM_EVENT_FILTER_COUNT.getMetricName(), FILTER_REASON.getTag("Validation Failure"));
      saveFilteredTransaction(
          sdmFinancialRetailTransaction.getFinancialRetailTransactionRecordId().toString(),
          AccountingNotAppliedReason.VALIDATION_FAILURE);
      return false;
    }

    if (sdmFinancialRetailTransaction.getPartnerRelationship().isEmpty()) {
      log.warn(
          "SDM Event has no validation results failure details, but has no PartnerRelationship to verify of type 'ECONCESSION', can not be consumed.");
      metricsClient.incrementCounter(
          SDM_EVENT_FILTER_COUNT.getMetricName(), FILTER_REASON.getTag("PR Empty"));
      saveFilteredTransaction(
          sdmFinancialRetailTransaction.getFinancialRetailTransactionRecordId().toString(),
          AccountingNotAppliedReason.INVALID_PARTNER_RELATIONSHIP_TYPE);
      return false;
    }

    if (!sdmFinancialRetailTransaction
        .getPartnerRelationship()
        .get()
        .getType()
        .equals(PartnerRelationshipType.ECONCESSION)) {
      log.warn(
          "SDM Event has no validation results failure details, but PartnerRelationship is not of type 'ECONCESSION', can not be consumed.");
      metricsClient.incrementCounter(
          SDM_EVENT_FILTER_COUNT.getMetricName(), FILTER_REASON.getTag("PR Not ECONCESSION"));
      saveFilteredTransaction(
          sdmFinancialRetailTransaction.getFinancialRetailTransactionRecordId().toString(),
          AccountingNotAppliedReason.INVALID_PARTNER_RELATIONSHIP_TYPE);
      return false;
    }

    if (getAmount(sdmFinancialRetailTransaction.getTotal()).compareTo(ZERO) == 0
        && sdmFinancialRetailTransaction.getTransactionAdjustment().isPresent()
        && sdmFinancialRetailTransaction
            .getTransactionAdjustment()
            .get()
            .getReason()
            .equals(FinancialRetailTransactionAdjustmentReason.OUTSIDE_OF_RETURN_POLICY)) {
      log.warn("Rejected Return SDM Event. Should not be processed");
      metricsClient.incrementCounter(
          SDM_EVENT_FILTER_COUNT.getMetricName(), FILTER_REASON.getTag("Rejected Return"));
      saveFilteredTransaction(
          sdmFinancialRetailTransaction.getFinancialRetailTransactionRecordId().toString(),
          AccountingNotAppliedReason.REJECTED_RETURN);
      return false;
    }

    if (sdmFinancialRetailTransaction.getAuditActivityDetail().isPresent()
        && sdmFinancialRetailTransaction
            .getAuditActivityDetail()
            .get()
            .getAuditActivityType()
            .equals(FinancialRetailTransactionAuditActivityType.CLEAN_DELETED)) {
      log.warn("SDM Event's AuditActivity is CLEAN_DELETED. Should not be processed");
      metricsClient.incrementCounter(
          SDM_EVENT_FILTER_COUNT.getMetricName(), FILTER_REASON.getTag("Clean Deleted"));
      saveFilteredTransaction(
          sdmFinancialRetailTransaction.getFinancialRetailTransactionRecordId().toString(),
          AccountingNotAppliedReason.CLEAN_DELETE);
      return false;
    }

    Optional<FinancialRetailTransactionMerchandiseLineItem>
        merchandiseLineItemRevenueRecognizedDetailAsValue =
            sdmFinancialRetailTransaction.getMerchandiseLineItems().stream()
                .filter(item -> item.getRevenueRecognizedDetail().isPresent())
                .findFirst();
    if (merchandiseLineItemRevenueRecognizedDetailAsValue.isEmpty()) {
      log.warn(
          "Revenue recognized details is null for all merchandise line items. Should not be processed");
      metricsClient.incrementCounter(
          SDM_EVENT_FILTER_COUNT.getMetricName(),
          FILTER_REASON.getTag("Null Revenue details for all merchandise line items"));
      saveFilteredTransaction(
          sdmFinancialRetailTransaction.getFinancialRetailTransactionRecordId().toString(),
          AccountingNotAppliedReason.NO_REVENUE_RECOGNIZED);
      return false;
    }

    if (transactionDBService.existsByTransactionId(
        SOURCE_REFERENCE_SYSTEM_TYPE_SDM,
        SOURCE_REFERENCE_TYPE_MARKETPLACE,
        getSdmId(sdmFinancialRetailTransaction))) {
      log.warn("Duplicate SDM Event. It has already been processed");
      metricsClient.incrementCounter(
          SDM_EVENT_FILTER_COUNT.getMetricName(), FILTER_REASON.getTag("Duplicate SDM Event"));
      saveFilteredTransaction(
          sdmFinancialRetailTransaction.getFinancialRetailTransactionRecordId().toString(),
          AccountingNotAppliedReason.DUPLICATE_TRANSACTION);
      return false;
    }

    return true;
  }

  private static String getSdmId(FinancialRetailTransaction sdmFinancialRetailTransaction) {
    return String.valueOf(sdmFinancialRetailTransaction.getFinancialRetailTransactionRecordId());
  }

  private void saveFilteredTransaction(String sdmTransactionId, AccountingNotAppliedReason reason)
      throws DatabaseConnectionException, DatabaseOperationException {
    FilteredTransaction filteredTransaction = new FilteredTransaction();
    filteredTransaction.setSourceReferenceTransactionId(sdmTransactionId);
    filteredTransaction.setFilteredReason(reason.name());
    filteredTransaction.setSourceReferenceSystemType(SOURCE_REFERENCE_SYSTEM_TYPE_SDM);
    filteredTransaction.setSourceReferenceType(SOURCE_REFERENCE_TYPE_MARKETPLACE);
    transactionDBService.saveFilteredTransaction(filteredTransaction);
  }
}
