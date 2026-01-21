package com.nordstrom.finance.dataintegration.promotion.database.aurora.repository;

import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.TransactionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionLineRepository extends JpaRepository<TransactionLine, Long> {}
