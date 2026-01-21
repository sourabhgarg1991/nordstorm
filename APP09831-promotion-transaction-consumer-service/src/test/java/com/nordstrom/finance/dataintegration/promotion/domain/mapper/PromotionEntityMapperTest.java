package com.nordstrom.finance.dataintegration.promotion.domain.mapper;

import static org.junit.jupiter.api.Assertions.*;

import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.PromotionTransactionLine;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.Transaction;
import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.TransactionLine;
import com.nordstrom.finance.dataintegration.promotion.domain.constant.PromotionGroupType;
import com.nordstrom.finance.dataintegration.promotion.domain.constant.TransactionActivityCode;
import com.nordstrom.finance.dataintegration.promotion.domain.constant.TransactionCode;
import com.nordstrom.finance.dataintegration.promotion.domain.model.LineItemDetailVO;
import com.nordstrom.finance.dataintegration.promotion.domain.model.TransactionDetailVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PromotionEntityMapperTest {

  @Test
  void testMapToTransactions_withValidInput() {
    LocalDate fixedDate = LocalDate.of(2024, 6, 1);
    LineItemDetailVO lineItem =
        new LineItemDetailVO(
            fixedDate,
            PromotionGroupType.LOYALTY_PROMO,
            "lineId1",
            BigDecimal.valueOf(10.00),
            false,
            TransactionCode.SALE,
            TransactionActivityCode.SALE,
            "1234");
    TransactionDetailVO detail = new TransactionDetailVO(List.of(lineItem), fixedDate, "txnId1");

    List<Transaction> transactions = PromotionEntityMapper.mapToTransactions(List.of(detail));

    assertEquals(1, transactions.size());
    Transaction txn = transactions.getFirst();
    assertNotNull(txn);
    assertEquals("txnId1", txn.getSourceReferenceTransactionId());
    assertEquals(1, txn.getTransactionLines().size());

    TransactionLine txnLine = txn.getTransactionLines().getFirst();
    assertNotNull(txnLine);
    assertEquals("lineId1", txnLine.getSourceReferenceLineId());
    assertEquals(txn, txnLine.getTransaction());

    assertEquals(1, txnLine.getPromotionTransactionLines().size());
    PromotionTransactionLine promoLine = txnLine.getPromotionTransactionLines().getFirst();
    assertNotNull(promoLine);
    assertEquals(BigDecimal.valueOf(10.00), promoLine.getPromoAmount());
    assertEquals(txnLine, promoLine.getTransactionLine());
  }
}
