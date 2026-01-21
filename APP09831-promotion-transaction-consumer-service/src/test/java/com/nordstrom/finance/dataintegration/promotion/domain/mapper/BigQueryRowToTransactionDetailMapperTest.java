package com.nordstrom.finance.dataintegration.promotion.domain.mapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.nordstrom.finance.dataintegration.promotion.database.gcp.constant.GcpQueryConstants;
import com.nordstrom.finance.dataintegration.promotion.domain.constant.PromotionGroupType;
import com.nordstrom.finance.dataintegration.promotion.domain.constant.TransactionActivityCode;
import com.nordstrom.finance.dataintegration.promotion.domain.constant.TransactionCode;
import com.nordstrom.finance.dataintegration.promotion.domain.model.LineItemDetailVO;
import com.nordstrom.finance.dataintegration.promotion.domain.model.TransactionDetailVO;
import com.nordstrom.finance.dataintegration.promotion.util.TestSchemaHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BigQueryRowToTransactionDetailMapperTest {

  @Test
  void testMapLineItem() {
    FieldValueList details = mock(FieldValueList.class);

    FieldValue firstReportedTmstp = mock(FieldValue.class);
    when(firstReportedTmstp.getStringValue()).thenReturn("2025-10-21T00:51:05.938000");
    when(details.get(GcpQueryConstants.FIRST_REPORTED_TMSTP_FIELD)).thenReturn(firstReportedTmstp);

    FieldValue businessOrigin = mock(FieldValue.class);
    when(businessOrigin.getStringValue()).thenReturn("LOYALTY_PROMO");
    when(details.get(GcpQueryConstants.BUSINESS_ORIGIN_FIELD)).thenReturn(businessOrigin);

    FieldValue lineItemId = mock(FieldValue.class);
    when(lineItemId.getStringValue()).thenReturn("lineId1");
    when(details.get(GcpQueryConstants.ITEM_TRANSACTION_LINE_ID_FIELD)).thenReturn(lineItemId);

    FieldValue discount = mock(FieldValue.class);
    when(discount.getNumericValue()).thenReturn(BigDecimal.valueOf(12.34));
    when(details.get(GcpQueryConstants.DISCOUNT_FIELD)).thenReturn(discount);

    FieldValue reversalFlag = mock(FieldValue.class);
    when(reversalFlag.getStringValue()).thenReturn("N");
    when(details.get(GcpQueryConstants.REVERSAL_FLAG_FIELD)).thenReturn(reversalFlag);

    FieldValue tranTypeCode = mock(FieldValue.class);
    when(tranTypeCode.getStringValue()).thenReturn("SALE");
    when(details.get(GcpQueryConstants.TRAN_TYPE_CODE_FIELD)).thenReturn(tranTypeCode);

    FieldValue activityTypeCode = mock(FieldValue.class);
    when(activityTypeCode.getStringValue()).thenReturn("S");
    when(details.get(GcpQueryConstants.LINE_ITEM_ACTIVITY_TYPE_CODE_FIELD))
        .thenReturn(activityTypeCode);

    FieldValue storeNum = mock(FieldValue.class);
    when(storeNum.getStringValue()).thenReturn("1234");
    when(details.get(GcpQueryConstants.STORE_NUM_FIELD)).thenReturn(storeNum);

    LineItemDetailVO vo = BigQueryRowToTransactionDetailMapper.mapLineItem(details);

    assertEquals(LocalDate.of(2025, 10, 21), vo.firstReportedTmstp());
    assertEquals(PromotionGroupType.LOYALTY_PROMO, vo.businessOrigin());
    assertEquals("lineId1", vo.lineItemId());
    assertEquals(BigDecimal.valueOf(12.34), vo.discountAmount());
    assertFalse(vo.isReversed());
    assertEquals(TransactionCode.SALE, vo.transactionCode());
    assertEquals(TransactionActivityCode.SALE, vo.transactionActivityCode());
    assertEquals("1234", vo.store());
  }

  @ParameterizedTest
  @CsvSource({
    "S, SALE, SALE",
    "R, RETURN, RETN",
    "RETURN, RETURN, RETN",
    "s, SALE, SALE",
    "r, RETURN, RETN",
    "return, RETURN, RETN"
  })
  void testActivityCodeMapping_VariousSourceFormats(
      String sourceActivityCode, String expectedEnumName, String expectedOutputCode) {
    FieldValueList details = mock(FieldValueList.class);

    FieldValue firstReportedTmstp = mock(FieldValue.class);
    when(firstReportedTmstp.getStringValue()).thenReturn("2025-10-21T00:51:05.938000");
    when(details.get(GcpQueryConstants.FIRST_REPORTED_TMSTP_FIELD)).thenReturn(firstReportedTmstp);

    FieldValue businessOrigin = mock(FieldValue.class);
    when(businessOrigin.getStringValue()).thenReturn("LOYALTY_PROMO");
    when(details.get(GcpQueryConstants.BUSINESS_ORIGIN_FIELD)).thenReturn(businessOrigin);

    FieldValue lineItemId = mock(FieldValue.class);
    when(lineItemId.getStringValue()).thenReturn("lineId1");
    when(details.get(GcpQueryConstants.ITEM_TRANSACTION_LINE_ID_FIELD)).thenReturn(lineItemId);

    FieldValue discount = mock(FieldValue.class);
    when(discount.getNumericValue()).thenReturn(BigDecimal.valueOf(12.34));
    when(details.get(GcpQueryConstants.DISCOUNT_FIELD)).thenReturn(discount);

    FieldValue reversalFlag = mock(FieldValue.class);
    when(reversalFlag.getStringValue()).thenReturn("N");
    when(details.get(GcpQueryConstants.REVERSAL_FLAG_FIELD)).thenReturn(reversalFlag);

    FieldValue tranTypeCode = mock(FieldValue.class);
    when(tranTypeCode.getStringValue()).thenReturn("SALE");
    when(details.get(GcpQueryConstants.TRAN_TYPE_CODE_FIELD)).thenReturn(tranTypeCode);

    FieldValue activityTypeCode = mock(FieldValue.class);
    when(activityTypeCode.getStringValue()).thenReturn(sourceActivityCode);
    when(details.get(GcpQueryConstants.LINE_ITEM_ACTIVITY_TYPE_CODE_FIELD))
        .thenReturn(activityTypeCode);

    FieldValue storeNum = mock(FieldValue.class);
    when(storeNum.getStringValue()).thenReturn("1234");
    when(details.get(GcpQueryConstants.STORE_NUM_FIELD)).thenReturn(storeNum);

    LineItemDetailVO vo = BigQueryRowToTransactionDetailMapper.mapLineItem(details);

    // Verify the enum mapping
    assertEquals(
        TransactionActivityCode.valueOf(expectedEnumName),
        vo.transactionActivityCode(),
        String.format("Source '%s' should map to enum %s", sourceActivityCode, expectedEnumName));

    // Verify the actual output code
    assertEquals(
        expectedOutputCode,
        vo.transactionActivityCode().getActivityCode(),
        String.format(
            "Source '%s' should output '%s' not '%s'",
            sourceActivityCode, expectedOutputCode, sourceActivityCode));
  }

  @Test
  void testMapTransactionDetailVO() {
    FieldValue firstReportedTmstp = mock(FieldValue.class);
    when(firstReportedTmstp.getStringValue()).thenReturn("2025-10-21T00:51:05.938000");

    FieldValue businessOrigin = mock(FieldValue.class);
    when(businessOrigin.getStringValue()).thenReturn("LOYALTY_PROMO");

    FieldValue lineItemId = mock(FieldValue.class);
    when(lineItemId.getStringValue()).thenReturn("lineId1");

    FieldValue discount = mock(FieldValue.class);
    when(discount.getNumericValue()).thenReturn(BigDecimal.valueOf(12.34));

    FieldValue reversalFlag = mock(FieldValue.class);
    when(reversalFlag.getStringValue()).thenReturn("N");

    FieldValue tranTypeCode = mock(FieldValue.class);
    when(tranTypeCode.getStringValue()).thenReturn("SALE");

    FieldValue activityTypeCode = mock(FieldValue.class);
    when(activityTypeCode.getStringValue()).thenReturn("S");

    FieldValue storeNum = mock(FieldValue.class);
    when(storeNum.getStringValue()).thenReturn("1234");

    // Build the line item struct as a real FieldValueList
    List<FieldValue> lineItemFields =
        List.of(
            firstReportedTmstp,
            businessOrigin,
            lineItemId,
            discount,
            reversalFlag,
            tranTypeCode,
            activityTypeCode,
            storeNum);
    FieldValueList lineItemStruct =
        FieldValueList.of(lineItemFields, TestSchemaHelper.getDetailStructFields());
    FieldValue lineItemFieldValue = mock(FieldValue.class);
    when(lineItemFieldValue.getAttribute()).thenReturn(FieldValue.Attribute.RECORD);
    when(lineItemFieldValue.getRecordValue()).thenReturn(lineItemStruct);

    // Build the details array (repeated field)
    FieldValue detailsField = mock(FieldValue.class);
    when(detailsField.getRepeatedValue()).thenReturn(List.of(lineItemFieldValue));

    // business_date and global_tran_id fields
    FieldValue businessDateField = mock(FieldValue.class);
    when(businessDateField.getStringValue()).thenReturn("2024-06-01");
    FieldValue globalTranIdField = mock(FieldValue.class);
    when(globalTranIdField.getStringValue()).thenReturn("txnId1");

    // Build the top-level row as a real FieldValueList
    List<FieldValue> rowFields = List.of(detailsField, businessDateField, globalTranIdField);
    Schema schema = TestSchemaHelper.getRowSchema();
    FieldValueList row = FieldValueList.of(rowFields, schema.getFields());

    TransactionDetailVO vo = BigQueryRowToTransactionDetailMapper.map(row, schema);

    assertEquals("txnId1", vo.globalTransactionId());
    assertEquals(LocalDate.of(2024, 6, 1), vo.businessDate());
    assertEquals(1, vo.lineItems().size());

    LineItemDetailVO lineItem = vo.lineItems().getFirst();
    assertEquals("lineId1", lineItem.lineItemId());
    assertEquals(BigDecimal.valueOf(12.34), lineItem.discountAmount());
  }
}
