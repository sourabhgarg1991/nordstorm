package com.nordstrom.finance.dataintegration.ertm.database.repository;

import com.nordstrom.finance.dataintegration.ertm.database.entity.RetailTransactionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetailTransactionLineRepository
    extends JpaRepository<RetailTransactionLine, Long> {}
