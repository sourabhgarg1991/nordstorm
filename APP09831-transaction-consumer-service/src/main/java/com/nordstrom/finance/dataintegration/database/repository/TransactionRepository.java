package com.nordstrom.finance.dataintegration.database.repository;

import com.nordstrom.finance.dataintegration.database.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  @Query(
      "SELECT COUNT(t) > 0 FROM Transaction t "
          + "WHERE t.sourceReferenceSystemType = :sourceReferenceSystemType "
          + "AND t.sourceReferenceType = :sourceReferenceType "
          + "AND t.sourceReferenceTransactionId = :transactionId")
  boolean existsByTransactionId(
      @Param("sourceReferenceSystemType") String sourceReferenceSystemType,
      @Param("sourceReferenceType") String sourceReferenceType,
      @Param("transactionId") String transactionId);
}
