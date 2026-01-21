package com.nordstrom.finance.dataintegration.facade.schema.retail;

import static com.nordstrom.finance.dataintegration.facade.BuilderFacadeConfig.TESTING_INSTANT;

import com.nordstrom.customer.object.operational.FinancialRetailTransactionValidationFailureDetail;
import com.nordstrom.customer.object.operational.FinancialRetailTransactionValidationResult;
import java.util.Collections;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class ValidationResultBuilderFacade {

  private ValidationResultBuilderFacade() {}

  private static FinancialRetailTransactionValidationResult.Builder getDefaultBuilder() {
    return FinancialRetailTransactionValidationResult.newBuilder()
        .setValidationTime(TESTING_INSTANT)
        .setFailureDetails(Collections.emptyList());
  }

  public static FinancialRetailTransactionValidationResult build(
      UnaryOperator<FinancialRetailTransactionValidationResult.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  @SafeVarargs
  public static FinancialRetailTransactionValidationResult build(
      UnaryOperator<FinancialRetailTransactionValidationResult.Builder>... modifiers) {
    UnaryOperator<FinancialRetailTransactionValidationResult.Builder> combinedModifier =
        Stream.of(modifiers).reduce(UnaryOperator.identity(), (a, b) -> a.andThen(b)::apply);
    return build(combinedModifier);
  }

  public static FinancialRetailTransactionValidationResult buildDefault() {
    return build(UnaryOperator.identity());
  }

  // Custom modifiers:
  public static UnaryOperator<FinancialRetailTransactionValidationResult.Builder>
      withFailureDetail() {
    FinancialRetailTransactionValidationFailureDetail failureDetail =
        FinancialRetailTransactionValidationFailureDetail.newBuilder()
            .setRuleName("RN")
            .setDescription("D")
            .build();
    return builder -> builder.setFailureDetails(Collections.singletonList(failureDetail));
  }
}
