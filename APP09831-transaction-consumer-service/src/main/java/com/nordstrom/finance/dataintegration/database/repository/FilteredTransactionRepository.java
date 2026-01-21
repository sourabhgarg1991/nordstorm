package com.nordstrom.finance.dataintegration.database.repository;

import com.nordstrom.finance.dataintegration.database.entity.FilteredTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FilteredTransactionRepository extends JpaRepository<FilteredTransaction, Long> {}
