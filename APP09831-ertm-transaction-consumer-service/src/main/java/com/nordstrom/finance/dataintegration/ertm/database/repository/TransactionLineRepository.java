package com.nordstrom.finance.dataintegration.ertm.database.repository;

import com.nordstrom.finance.dataintegration.ertm.database.entity.TransactionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionLineRepository extends JpaRepository<TransactionLine, Long> {}
