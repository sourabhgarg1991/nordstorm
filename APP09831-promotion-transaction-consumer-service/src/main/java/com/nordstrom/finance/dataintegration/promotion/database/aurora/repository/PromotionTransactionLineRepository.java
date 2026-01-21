package com.nordstrom.finance.dataintegration.promotion.database.aurora.repository;

import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.PromotionTransactionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionTransactionLineRepository
    extends JpaRepository<PromotionTransactionLine, Long> {}
