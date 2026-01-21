package com.nordstrom.finance.dataintegration.facade.schema.standard;

import com.nordstrom.standard.MoneyV2;
import com.nordstrom.standard.PartnerRelationship;
import com.nordstrom.standard.PartnerRelationshipType;
import java.util.function.UnaryOperator;

public class PartnerRelationshipBuilderFacade {

  private PartnerRelationshipBuilderFacade() {}

  private static PartnerRelationship.Builder getDefaultBuilder() {
    MoneyV2 defaultMoney = MoneyBuilderFacade.buildDefault();

    return PartnerRelationship.newBuilder()
        .setId("Id-1")
        .setType(PartnerRelationshipType.ECONCESSION);
  }

  public static PartnerRelationship build(UnaryOperator<PartnerRelationship.Builder> modifier) {
    return modifier.apply(getDefaultBuilder()).build();
  }

  public static PartnerRelationship buildDefault() {
    return build(UnaryOperator.identity());
  }
}
