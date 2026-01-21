package com.nordstrom.finance.dataintegration.promotion.domain.model;

import java.time.LocalDate;
import java.util.List;

public record TransactionDetailVO(
    List<LineItemDetailVO> lineItems, LocalDate businessDate, String globalTransactionId) {}
