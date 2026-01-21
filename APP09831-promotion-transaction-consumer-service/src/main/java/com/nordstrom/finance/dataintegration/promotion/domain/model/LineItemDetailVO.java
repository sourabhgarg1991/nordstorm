package com.nordstrom.finance.dataintegration.promotion.domain.model;

import com.nordstrom.finance.dataintegration.promotion.domain.constant.PromotionGroupType;
import com.nordstrom.finance.dataintegration.promotion.domain.constant.TransactionActivityCode;
import com.nordstrom.finance.dataintegration.promotion.domain.constant.TransactionCode;
import java.math.BigDecimal;
import java.time.LocalDate;

public record LineItemDetailVO(
    LocalDate firstReportedTmstp, // first_reported_tmstp
    PromotionGroupType businessOrigin, // business_origin
    String lineItemId, // item_transaction_line_id
    BigDecimal discountAmount, // discount
    boolean isReversed, // reversal_flag
    TransactionCode transactionCode, // tran_type_code
    TransactionActivityCode transactionActivityCode, // line_item_activity_type_code
    String store // store_num
    ) {}
