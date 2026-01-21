package com.nordstrom.finance.dataintegration.promotion.database.aurora.repository;

import com.nordstrom.finance.dataintegration.promotion.database.aurora.entity.Transaction;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
  /**
   * Finds all existing source reference transaction IDs from the given list. This method is
   * optimized for batch duplicate checking.
   *
   * @param sourceReferenceTransactionIds list of source reference transaction IDs to check
   * @return set of source reference transaction IDs that already exist in the database
   */
  @Query(
      "SELECT t.sourceReferenceTransactionId FROM Transaction t "
          + "WHERE t.sourceReferenceTransactionId IN :ids")
  Set<String> findExistingSourceReferenceTransactionIds(
      @Param("ids") List<String> sourceReferenceTransactionIds);

  List<Transaction> findBySourceReferenceSystemType(String sourceReferenceSystemType);
}
