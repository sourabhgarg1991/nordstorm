package com.nordstrom.finance.dataintegration.ertm.database.repository;

import com.nordstrom.finance.dataintegration.ertm.database.entity.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

  // duplicate check based on transactionId and transactionlineId
  @Query(
      value =
          "SELECT tl.SOURCE_REFERENCE_LINE_ID FROM transaction t "
              + "JOIN transaction_line tl ON t.transaction_id = tl.transaction_id "
              + "WHERE t.SOURCE_REFERENCE_TRANSACTION_ID = :transactionId "
              + "AND tl.SOURCE_REFERENCE_LINE_ID IN (:lineIds)",
      nativeQuery = true)
  List<String> getExistingLineItemIds(
      @Param("transactionId") String transactionId, @Param("lineIds") List<String> lineIds);

  // Get Transaction with same source_reference_transaction_id
  @Query(
      "SELECT t.sourceReferenceTransactionId FROM Transaction t WHERE t.sourceReferenceTransactionId in (:transactionIds) and t.sourceReferenceSystemType = 'ertm'")
  List<String> getExistingSourceReferenceTransactionIds(
      @Param("transactionIds") List<String> transactionIds);
}
