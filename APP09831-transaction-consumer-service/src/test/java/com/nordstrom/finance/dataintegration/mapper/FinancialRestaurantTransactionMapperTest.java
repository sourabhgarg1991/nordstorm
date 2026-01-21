package com.nordstrom.finance.dataintegration.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.nordstrom.customer.object.operational.FinancialRestaurantTransaction;
import com.nordstrom.finance.dataintegration.database.entity.Transaction;
import com.nordstrom.finance.dataintegration.exception.ObjectMappingException;
import com.nordstrom.finance.dataintegration.facade.entity.TransactionFacade;
import com.nordstrom.finance.dataintegration.facade.schema.restaurant.FinancialRestaurantTransactionBuilderFacade;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {FinancialRestaurantTransactionMapper.class})
public class FinancialRestaurantTransactionMapperTest {
  @Autowired FinancialRestaurantTransactionMapper financialRestaurantTransactionMapper;

  static Stream<Arguments> transactionScenarios() {
    return Stream.of(
        // #1 Sale default
        Arguments.arguments(
            FinancialRestaurantTransactionBuilderFacade.saleWithSingleItemAndSingleTender(),
            TransactionFacade.saleWithSingleItemAndSingleTender()),
        Arguments.arguments(
            FinancialRestaurantTransactionBuilderFacade.returnWithSingleItemAndSingleTender(),
            TransactionFacade.returnWithSingleItemAndSingleTender()),
        Arguments.arguments(
            FinancialRestaurantTransactionBuilderFacade.reversedSaleWithSingleItemAndSingleTender(),
            TransactionFacade.reversedSaleWithSingleItemAndSingleTender()),
        Arguments.arguments(
            FinancialRestaurantTransactionBuilderFacade
                .reversedReturnWithSingleItemAndSingleTender(),
            TransactionFacade.reversedReturnWithSingleItemAndSingleTender()));
  }

  @ParameterizedTest(
      name = "SDM Retail Schema object to Transaction entity object mapping test :: {index}")
  @MethodSource("transactionScenarios")
  public void mapToRetailTransactionSuccessTest(
      FinancialRestaurantTransaction sdmRestaurantTransaction, Transaction expectedTransaction)
      throws ObjectMappingException {
    Transaction transaction =
        financialRestaurantTransactionMapper.mapRestaurantSchemaToTransactionEntity(
            sdmRestaurantTransaction);
    assertEquals(expectedTransaction, transaction);
  }
}
