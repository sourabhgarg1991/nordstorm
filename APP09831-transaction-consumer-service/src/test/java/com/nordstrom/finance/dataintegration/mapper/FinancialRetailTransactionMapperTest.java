package com.nordstrom.finance.dataintegration.mapper;

import static com.nordstrom.finance.dataintegration.facade.schema.retail.FinancialRetailTransactionBuilderFacade.withReturnTypeDefault;
import static com.nordstrom.finance.dataintegration.facade.schema.retail.FinancialRetailTransactionBuilderFacade.withoutItemsRevenueRecognizedDetail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.nordstrom.customer.object.operational.FinancialRetailTransaction;
import com.nordstrom.finance.dataintegration.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.exception.ObjectMappingException;
import com.nordstrom.finance.dataintegration.facade.entity.TransactionFacade;
import com.nordstrom.finance.dataintegration.facade.schema.retail.FinancialRetailTransactionBuilderFacade;
import com.nordstrom.finance.dataintegration.fortknox.FortKnoxRedemptionService;
import com.nordstrom.finance.dataintegration.fortknox.exception.FortknoxException;
import com.nordstrom.standard.TokenizedMoneyV2;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {FinancialRetailTransactionMapper.class})
@ActiveProfiles("test")
public class FinancialRetailTransactionMapperTest {
  @Autowired FinancialRetailTransactionMapper financialRetailTransactionMapper;
  @MockitoBean FortKnoxRedemptionService fortKnoxRedemptionService;

  static Stream<Arguments> transactionScenarios() {
    return Stream.of(
        // #1 Sale default
        Arguments.arguments(
            FinancialRetailTransactionBuilderFacade.buildDefault(),
            TransactionFacade.marketplaceSale()),
        Arguments.arguments(
            FinancialRetailTransactionBuilderFacade.build(withReturnTypeDefault()),
            TransactionFacade.marketplaceReturn()),
        Arguments.arguments(
            FinancialRetailTransactionBuilderFacade.build(
                transaction -> transaction.setIsReversed(true)),
            TransactionFacade.marketplaceSaleReversed()),
        Arguments.arguments(
            FinancialRetailTransactionBuilderFacade.build(
                withReturnTypeDefault(), transaction -> transaction.setIsReversed(true)),
            TransactionFacade.marketplaceReturnReversed()),
        Arguments.arguments(
            FinancialRetailTransactionBuilderFacade.build(withoutItemsRevenueRecognizedDetail(3)),
            TransactionFacade.marketplaceWithoutCommission()));
  }

  @ParameterizedTest(
      name = "SDM Retail Schema object to Transaction entity object mapping test :: {index}")
  @MethodSource("transactionScenarios")
  public void mapToRetailTransactionSuccessTest(
      FinancialRetailTransaction sdmRetailTransaction, Transaction expectedTransaction)
      throws ObjectMappingException, FortknoxException {
    when(fortKnoxRedemptionService.redeemAndGetFullAmount(any(TokenizedMoneyV2.class)))
        .thenReturn(new BigDecimal("1"));
    Transaction transaction = financialRetailTransactionMapper.toTransaction(sdmRetailTransaction);
    assertEquals(expectedTransaction, transaction);
  }
}
