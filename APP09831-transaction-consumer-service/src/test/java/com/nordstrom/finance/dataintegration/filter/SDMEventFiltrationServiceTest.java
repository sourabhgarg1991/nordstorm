package com.nordstrom.finance.dataintegration.filter;

import static com.nordstrom.customer.object.operational.FinancialRetailTransactionItemRefundAdjustmentReason.OUTSIDE_OF_RETURN_POLICY;
import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_SYSTEM_TYPE_SDM;
import static com.nordstrom.finance.dataintegration.constant.TransactionMappingConstants.SOURCE_REFERENCE_TYPE_MARKETPLACE;
import static com.nordstrom.finance.dataintegration.facade.schema.retail.FinancialRetailTransactionBuilderFacade.withCleanDeletedAuditActivity;
import static com.nordstrom.finance.dataintegration.facade.schema.retail.FinancialRetailTransactionBuilderFacade.withFailedValidation;
import static com.nordstrom.finance.dataintegration.facade.schema.retail.FinancialRetailTransactionBuilderFacade.withMerchandiseLineItems;
import static com.nordstrom.finance.dataintegration.facade.schema.retail.FinancialRetailTransactionBuilderFacade.withPartnerRelationship;
import static com.nordstrom.finance.dataintegration.facade.schema.retail.FinancialRetailTransactionBuilderFacade.withTransactionAdjustment;
import static com.nordstrom.finance.dataintegration.facade.schema.retail.FinancialRetailTransactionBuilderFacade.withZeroTotal;
import static com.nordstrom.finance.dataintegration.facade.schema.retail.FinancialRetailTransactionBuilderFacade.withoutItemsRevenueRecognizedDetail;
import static com.nordstrom.finance.dataintegration.facade.schema.retail.MerchandiseLineItemBuilderFacade.buildWithItemsReturnDetails;
import static com.nordstrom.finance.dataintegration.metric.Metric.SDM_EVENT_FILTER_COUNT;
import static com.nordstrom.finance.dataintegration.metric.MetricTag.FILTER_REASON;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nordstrom.customer.object.operational.FinancialRetailTransaction;
import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.database.service.TransactionDBService;
import com.nordstrom.finance.dataintegration.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.exception.DatabaseOperationException;
import com.nordstrom.finance.dataintegration.facade.schema.retail.FinancialRetailTransactionBuilderFacade;
import com.nordstrom.finance.dataintegration.facade.schema.standard.MoneyBuilderFacade;
import com.nordstrom.standard.PartnerRelationshipType;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
    classes = {
      SDMEventFiltrationService.class,
    })
@ActiveProfiles("test")
@DirtiesContext
@EmbeddedKafka
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
public class SDMEventFiltrationServiceTest {

  @Value("${spring.kafka.producer.topic.noAccountingApplied.name}")
  private String topicName;

  @Autowired SDMEventFiltrationService sdmEventFiltrationService;
  @MockitoBean TransactionDBService transactionDBService;
  @MockitoBean private MetricsClient metricsClient;

  static Stream<Arguments> isAcceptableScenarios() {
    return Stream.of(
        // #1 Acceptable Default
        Arguments.arguments(FinancialRetailTransactionBuilderFacade.buildDefault(), true, null),
        // #2 Rejected Return
        Arguments.arguments(
            FinancialRetailTransactionBuilderFacade.build(
                FinancialRetailTransactionBuilderFacade.withReturnTypeDefault(),
                withZeroTotal(),
                withTransactionAdjustment(),
                withMerchandiseLineItems(buildWithItemsReturnDetails(2, OUTSIDE_OF_RETURN_POLICY))),
            false),
        // #3 Sale with unknown Partner Relationship Type
        Arguments.arguments(
            FinancialRetailTransactionBuilderFacade.build(
                withPartnerRelationship(PartnerRelationshipType.UNKNOWN)),
            false),
        // #4 Sale with null PartnerRelationshipType
        Arguments.arguments(
            FinancialRetailTransactionBuilderFacade.build(withPartnerRelationship(null)), false),
        // #5 Sale with Failed Validation
        Arguments.arguments(
            FinancialRetailTransactionBuilderFacade.build(withFailedValidation()), false),
        // #6 Sale without Revenue Recognized Detail
        Arguments.arguments(
            FinancialRetailTransactionBuilderFacade.build(withoutItemsRevenueRecognizedDetail(3)),
            false),
        // #7 Audit Activity as CLEAN_DELETED
        Arguments.arguments(
            FinancialRetailTransactionBuilderFacade.build(withCleanDeletedAuditActivity()), false));
  }

  @ParameterizedTest(name = "Is Acceptable Scenario #{index}")
  @MethodSource("isAcceptableScenarios")
  public void isSdmTransactionAcceptable_returns_correctValue(
      FinancialRetailTransaction financialRetailTransaction, boolean isAcceptableExpected)
      throws DatabaseOperationException, DatabaseConnectionException {

    boolean isAcceptableActual =
        sdmEventFiltrationService.isSDMTransactionAcceptable(financialRetailTransaction);
    assertEquals(isAcceptableExpected, isAcceptableActual);

    verify(transactionDBService, times(isAcceptableExpected ? 0 : 1))
        .saveFilteredTransaction(any());
  }

  @Test
  public void isSdmTransactionAcceptable_duplicateFound()
      throws DatabaseOperationException, DatabaseConnectionException {
    FinancialRetailTransaction sdmFinancialRetailTransaction =
        FinancialRetailTransactionBuilderFacade.buildDefault();
    String sdmId =
        String.valueOf(sdmFinancialRetailTransaction.getFinancialRetailTransactionRecordId());

    when(transactionDBService.existsByTransactionId(
            SOURCE_REFERENCE_SYSTEM_TYPE_SDM, SOURCE_REFERENCE_TYPE_MARKETPLACE, sdmId))
        .thenReturn(true);

    boolean isAcceptable =
        sdmEventFiltrationService.isSDMTransactionAcceptable(sdmFinancialRetailTransaction);
    assertFalse(isAcceptable);

    verify(metricsClient, times(1))
        .incrementCounter(
            SDM_EVENT_FILTER_COUNT.getMetricName(), FILTER_REASON.getTag("Duplicate SDM Event"));
  }

  static Stream<Arguments> allowableAmounts() {
    return Stream.of(
        Arguments.arguments(0L, 1),
        Arguments.arguments(0L, -1),
        Arguments.arguments(1L, 1),
        Arguments.arguments(-1L, 1),
        Arguments.arguments(-1L, -1));
  }

  @ParameterizedTest(name = "Allowable amounts #{index}")
  @MethodSource("allowableAmounts")
  public void allowsNonZeroAmounts(long units, int nanos)
      throws DatabaseOperationException, DatabaseConnectionException {
    FinancialRetailTransaction financialRetailTransaction =
        FinancialRetailTransactionBuilderFacade.build(
            transaction -> transaction.setTotal(MoneyBuilderFacade.build(units, nanos)));

    boolean isAcceptableActual =
        sdmEventFiltrationService.isSDMTransactionAcceptable(financialRetailTransaction);
    assertTrue(isAcceptableActual);
  }
}
