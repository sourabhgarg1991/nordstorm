package com.nordstrom.finance.dataintegration.facade.schema.restaurant;

import static com.nordstrom.finance.dataintegration.facade.BuilderFacadeConfig.TESTING_INSTANT;

import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionValidationFailureDetail;
import com.nordstrom.customer.object.operational.FinancialRestaurantTransactionValidationResult;
import java.util.Collections;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class ValidationResultBuilderFacade {

  private ValidationResultBuilderFacade() {}

  private static FinancialRestaurantTransactionValidationResult.Builder getDefaultBuilder() {
    return FinancialRestaurantTransactionValidationResult.newBuilder()
        .setValidationTime(TESTING_INSTANT)
        .setFailureDetails(Collections.emptyList());
  }

  public static FinancialRestaurantTransactionValidationResult build(
      UnaryOperator<FinancialRestaurantTransactionValidationResult.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  @SafeVarargs
  public static FinancialRestaurantTransactionValidationResult build(
      UnaryOperator<FinancialRestaurantTransactionValidationResult.Builder>... modifiers) {
    UnaryOperator<FinancialRestaurantTransactionValidationResult.Builder> combinedModifier =
        Stream.of(modifiers).reduce(UnaryOperator.identity(), (a, b) -> a.andThen(b)::apply);
    return build(combinedModifier);
  }

  public static FinancialRestaurantTransactionValidationResult buildDefault() {
    return build(UnaryOperator.identity());
  }

  // Custom modifiers:
  public static UnaryOperator<FinancialRestaurantTransactionValidationResult.Builder>
      withFailureDetail() {
    FinancialRestaurantTransactionValidationFailureDetail failureDetail =
        FinancialRestaurantTransactionValidationFailureDetail.newBuilder()
            .setRuleName("RN")
            .setDescription("D")
            .build();
    return builder -> builder.setFailureDetails(Collections.singletonList(failureDetail));
  }
}
