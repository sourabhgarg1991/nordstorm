package com.nordstrom.finance.dataintegration.transactionaggregator.database.repository;

import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.TransactionAggregationRelationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing TransactionAggregationRelationEntity entities. This interface
 * extends JpaRepository to provide CRUD operations.
 */
@Repository
public interface TransactionAggregationRelationRepository
    extends JpaRepository<TransactionAggregationRelationEntity, Long> {
  // You can define custom query methods here if needed
}
