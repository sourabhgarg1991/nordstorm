package com.nordstrom.finance.dataintegration.promotion.util;

import static com.nordstrom.finance.dataintegration.promotion.database.gcp.constant.GcpQueryConstants.*;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.nordstrom.finance.dataintegration.promotion.domain.constant.PromotionGroupType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import lombok.experimental.UtilityClass;
import org.mockito.Mockito;

@UtilityClass
public class SourceRowTestDataBuilder {

  private static final String DEFAULT_GLOBAL_TRAN_ID = "TEST_GLOBAL_TRAN_001";
  private static final String DEFAULT_BUSINESS_DATE = "2025-01-15";
  private static final String DEFAULT_LINE_ITEM_ID = "LINE_001";
  private static final String DEFAULT_STORE_NUM = "0100";

  public static class RowBuilder {
    private String globalTranId = DEFAULT_GLOBAL_TRAN_ID;
    private String businessDate = DEFAULT_BUSINESS_DATE;
    private final List<DetailBuilder> details = new ArrayList<>();

    public RowBuilder setGlobalTranId(String globalTranId) {
      this.globalTranId = globalTranId;
      return this;
    }

    public RowBuilder addDetail(DetailBuilder detail) {
      this.details.add(detail);
      return this;
    }

    public FieldValueList build() {
      List<FieldValue> detailFieldValues = new ArrayList<>();
      for (DetailBuilder detail : details) {
        detailFieldValues.add(
            FieldValue.of(FieldValue.Attribute.REPEATED, detail.toFieldValueList()));
      }

      return FieldValueList.of(
          List.of(
              FieldValue.of(FieldValue.Attribute.REPEATED, FieldValueList.of(detailFieldValues)),
              mockStringField(businessDate),
              mockStringField(globalTranId)),
          getSourceSchema().getFields());
    }
  }

  public static class DetailBuilder {
    private String firstReportedTmstp = "2025-01-15T00:51:05.938000";
    private String businessOrigin = PromotionGroupType.LOYALTY_PROMO.name();
    private String itemTransactionLineId = DEFAULT_LINE_ITEM_ID;
    private BigDecimal discount = BigDecimal.valueOf(-5.00);
    private String reversalFlag = "N";
    private String tranTypeCode = "SALE";
    private String lineItemActivityTypeCode = "S";
    private String storeNum = DEFAULT_STORE_NUM;

    public DetailBuilder setBusinessOrigin(String businessOrigin) {
      this.businessOrigin = businessOrigin;
      return this;
    }

    public DetailBuilder setItemTransactionLineId(String itemTransactionLineId) {
      this.itemTransactionLineId = itemTransactionLineId;
      return this;
    }

    public DetailBuilder setDiscount(BigDecimal discount) {
      this.discount = discount;
      return this;
    }

    public DetailBuilder setReversalFlag(String reversalFlag) {
      this.reversalFlag = reversalFlag;
      return this;
    }

    public DetailBuilder setTranTypeCode(String tranTypeCode) {
      this.tranTypeCode = tranTypeCode;
      return this;
    }

    public DetailBuilder setLineItemActivityTypeCode(String lineItemActivityTypeCode) {
      this.lineItemActivityTypeCode = lineItemActivityTypeCode;
      return this;
    }

    public DetailBuilder setStoreNum(String storeNum) {
      this.storeNum = storeNum;
      return this;
    }

    FieldValueList toFieldValueList() {
      // Create properly mocked FieldValue objects for each field
      FieldValue timestampField = mockStringField(firstReportedTmstp);
      FieldValue businessOriginField = mockStringField(businessOrigin);
      FieldValue itemTransactionLineIdField = mockStringField(itemTransactionLineId);
      FieldValue discountField = mockNumericField(discount);
      FieldValue reversalFlagField = mockStringField(reversalFlag);
      FieldValue tranTypeCodeField = mockStringField(tranTypeCode);
      FieldValue lineItemActivityTypeCodeField = mockStringField(lineItemActivityTypeCode);
      FieldValue storeNumField = mockStringField(storeNum);

      List<FieldValue> fieldValues =
          List.of(
              timestampField,
              businessOriginField,
              itemTransactionLineIdField,
              discountField,
              reversalFlagField,
              tranTypeCodeField,
              lineItemActivityTypeCodeField,
              storeNumField);

      return FieldValueList.of(fieldValues, getDetailSchema().getFields());
    }
  }

  // ========== HELPER METHODS FOR MOCKING FIELD VALUES ==========

  /**
   * Creates a mocked FieldValue for string fields. Mocks getStringValue() to return the string
   * value.
   */
  private static FieldValue mockStringField(String value) {
    FieldValue field = Mockito.mock(FieldValue.class);
    Mockito.when(field.getAttribute()).thenReturn(FieldValue.Attribute.PRIMITIVE);
    Mockito.when(field.getStringValue()).thenReturn(value);
    return field;
  }

  /**
   * Creates a mocked FieldValue for numeric fields. Mocks getNumericValue() to return the
   * BigDecimal value.
   */
  private static FieldValue mockNumericField(BigDecimal value) {
    FieldValue field = Mockito.mock(FieldValue.class);
    Mockito.when(field.getAttribute()).thenReturn(FieldValue.Attribute.PRIMITIVE);
    Mockito.when(field.getNumericValue()).thenReturn(value);
    return field;
  }

  // ========== BUILDER METHODS ==========

  // Creates an EMPTY RowBuilder - scenarios add their own details
  private static RowBuilder emptyRow() {
    return new RowBuilder();
  }

  public static DetailBuilder defaultDetail() {
    return new DetailBuilder();
  }

  public static FieldValueList buildRow(UnaryOperator<RowBuilder> modifier) {
    return modifier.apply(emptyRow()).build();
  }

  public static DetailBuilder buildDetail(UnaryOperator<DetailBuilder> modifier) {
    return modifier.apply(defaultDetail());
  }

  // ========== SCHEMA DEFINITIONS ==========

  public static Schema getSourceSchema() {
    return Schema.of(
        Field.newBuilder(
                DETAILS_FIELD_NAME, StandardSQLTypeName.STRUCT, getDetailSchema().getFields())
            .setMode(Field.Mode.REPEATED)
            .build(),
        Field.of(BUSINESS_DATE_FIELD_NAME, StandardSQLTypeName.STRING),
        Field.of(GLOBAL_TRANS_ID_FIELD_NAME, StandardSQLTypeName.STRING));
  }

  private static Schema getDetailSchema() {
    return Schema.of(
        Field.of(FIRST_REPORTED_TMSTP_FIELD, StandardSQLTypeName.STRING),
        Field.of(BUSINESS_ORIGIN_FIELD, StandardSQLTypeName.STRING),
        Field.of(ITEM_TRANSACTION_LINE_ID_FIELD, StandardSQLTypeName.STRING),
        Field.of(DISCOUNT_FIELD, StandardSQLTypeName.NUMERIC),
        Field.of(REVERSAL_FLAG_FIELD, StandardSQLTypeName.STRING),
        Field.of(TRAN_TYPE_CODE_FIELD, StandardSQLTypeName.STRING),
        Field.of(LINE_ITEM_ACTIVITY_TYPE_CODE_FIELD, StandardSQLTypeName.STRING),
        Field.of(STORE_NUM_FIELD, StandardSQLTypeName.STRING));
  }

  // ========== LOYALTY SCENARIOS (11) ==========

  public static FieldValueList loyaltyScenario1() {
    return buildRow(row -> row.setGlobalTranId("TEST_GLOBAL_TRAN_L01").addDetail(defaultDetail()));
  }

  public static FieldValueList loyaltyScenario2() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_L02")
                .addDetail(buildDetail(d -> d.setReversalFlag("Y"))));
  }

  public static FieldValueList loyaltyScenario3() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_L03")
                .addDetail(
                    buildDetail(
                        d ->
                            d.setTranTypeCode("RETN")
                                .setLineItemActivityTypeCode("R")
                                .setDiscount(BigDecimal.valueOf(5.00)))));
  }

  public static FieldValueList loyaltyScenario4() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_L04")
                .addDetail(
                    buildDetail(
                        d ->
                            d.setTranTypeCode("RETN")
                                .setLineItemActivityTypeCode("R")
                                .setReversalFlag("Y")
                                .setDiscount(BigDecimal.valueOf(5.00)))));
  }

  public static FieldValueList loyaltyScenario5() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_L05")
                .addDetail(
                    buildDetail(
                        d -> d.setTranTypeCode("RETN").setDiscount(BigDecimal.valueOf(5.00)))));
  }

  public static FieldValueList loyaltyScenario6() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_L06")
                .addDetail(buildDetail(d -> d.setTranTypeCode("EXCH"))));
  }

  public static FieldValueList loyaltyScenario7() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_L07")
                .addDetail(buildDetail(d -> d.setTranTypeCode("EXCH").setReversalFlag("Y"))));
  }

  public static FieldValueList loyaltyScenario8() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_L08")
                .addDetail(
                    buildDetail(
                        d ->
                            d.setTranTypeCode("EXCH")
                                .setLineItemActivityTypeCode("R")
                                .setDiscount(BigDecimal.valueOf(5.00)))));
  }

  public static FieldValueList loyaltyScenario9() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_L09")
                .addDetail(
                    buildDetail(
                        d ->
                            d.setTranTypeCode("EXCH")
                                .setLineItemActivityTypeCode("R")
                                .setReversalFlag("Y")
                                .setDiscount(BigDecimal.valueOf(5.00)))));
  }

  public static FieldValueList loyaltyScenario10() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_L10")
                .addDetail(buildDetail(d -> d.setTranTypeCode("VOID"))));
  }

  public static FieldValueList loyaltyScenario11() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_L11")
                .addDetail(
                    buildDetail(
                        d ->
                            d.setTranTypeCode("VOID")
                                .setLineItemActivityTypeCode("R")
                                .setDiscount(BigDecimal.valueOf(5.00)))));
  }

  // ========== MARKETING SCENARIOS (11) ==========

  public static FieldValueList marketingScenario1() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_M01")
                .addDetail(
                    buildDetail(
                        d -> d.setBusinessOrigin(PromotionGroupType.MARKETING_PROMO.name()))));
  }

  public static FieldValueList marketingScenario2() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_M02")
                .addDetail(
                    buildDetail(
                        d ->
                            d.setBusinessOrigin(PromotionGroupType.MARKETING_PROMO.name())
                                .setReversalFlag("Y"))));
  }

  public static FieldValueList marketingScenario3() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_M03")
                .addDetail(
                    buildDetail(
                        d ->
                            d.setBusinessOrigin(PromotionGroupType.MARKETING_PROMO.name())
                                .setTranTypeCode("RETN")
                                .setLineItemActivityTypeCode("R")
                                .setDiscount(BigDecimal.valueOf(5.00)))));
  }

  public static FieldValueList marketingScenario4() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_M04")
                .addDetail(
                    buildDetail(
                        d ->
                            d.setBusinessOrigin(PromotionGroupType.MARKETING_PROMO.name())
                                .setTranTypeCode("RETN")
                                .setLineItemActivityTypeCode("R")
                                .setReversalFlag("Y")
                                .setDiscount(BigDecimal.valueOf(5.00)))));
  }

  public static FieldValueList marketingScenario5() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_M05")
                .addDetail(
                    buildDetail(
                        d ->
                            d.setBusinessOrigin(PromotionGroupType.MARKETING_PROMO.name())
                                .setTranTypeCode("RETN")
                                .setDiscount(BigDecimal.valueOf(5.00)))));
  }

  public static FieldValueList marketingScenario6() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_M06")
                .addDetail(
                    buildDetail(
                        d ->
                            d.setBusinessOrigin(PromotionGroupType.MARKETING_PROMO.name())
                                .setTranTypeCode("EXCH"))));
  }

  public static FieldValueList marketingScenario7() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_M07")
                .addDetail(
                    buildDetail(
                        d ->
                            d.setBusinessOrigin(PromotionGroupType.MARKETING_PROMO.name())
                                .setTranTypeCode("EXCH")
                                .setReversalFlag("Y"))));
  }

  public static FieldValueList marketingScenario8() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_M08")
                .addDetail(
                    buildDetail(
                        d ->
                            d.setBusinessOrigin(PromotionGroupType.MARKETING_PROMO.name())
                                .setTranTypeCode("EXCH")
                                .setLineItemActivityTypeCode("R")
                                .setDiscount(BigDecimal.valueOf(5.00)))));
  }

  public static FieldValueList marketingScenario9() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_M09")
                .addDetail(
                    buildDetail(
                        d ->
                            d.setBusinessOrigin(PromotionGroupType.MARKETING_PROMO.name())
                                .setTranTypeCode("EXCH")
                                .setLineItemActivityTypeCode("R")
                                .setReversalFlag("Y")
                                .setDiscount(BigDecimal.valueOf(5.00)))));
  }

  public static FieldValueList marketingScenario10() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_M10")
                .addDetail(
                    buildDetail(
                        d ->
                            d.setBusinessOrigin(PromotionGroupType.MARKETING_PROMO.name())
                                .setTranTypeCode("VOID"))));
  }

  public static FieldValueList marketingScenario11() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_M11")
                .addDetail(
                    buildDetail(
                        d ->
                            d.setBusinessOrigin(PromotionGroupType.MARKETING_PROMO.name())
                                .setTranTypeCode("VOID")
                                .setLineItemActivityTypeCode("R")
                                .setDiscount(BigDecimal.valueOf(5.00)))));
  }

  // ========== MULTI-ITEM SCENARIOS ==========

  public static FieldValueList multipleLineItems() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_MULTI_LINE")
                .addDetail(defaultDetail())
                .addDetail(buildDetail(d -> d.setItemTransactionLineId("LINE_002"))));
  }

  public static FieldValueList multipleStores() {
    return buildRow(
        row ->
            row.setGlobalTranId("TEST_GLOBAL_TRAN_MULTI_STORE")
                .addDetail(defaultDetail())
                .addDetail(
                    buildDetail(d -> d.setItemTransactionLineId("LINE_002").setStoreNum("0101"))));
  }

  public static FieldValueList withCustomGlobalTranId(String globalTranId) {
    return buildRow(row -> row.setGlobalTranId(globalTranId).addDetail(defaultDetail()));
  }
}
