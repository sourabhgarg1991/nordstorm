package com.nordstrom.finance.dataintegration.transactionaggregator.database.repository;

import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.GeneratedFileDetailEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing GeneratedFileDetailEntity entities. This interface extends
 * JpaRepository to provide CRUD operations.
 */
@Repository
public interface GeneratedFileDetailRepository
    extends JpaRepository<GeneratedFileDetailEntity, Long> {

  @Modifying
  @Query(
      "update GeneratedFileDetailEntity a set a.isUploadedToS3 = true, "
          + " a.lastUpdatedDatetime = CURRENT_TIMESTAMP where a.generatedFileName = :fileName ")
  void updateGeneratedFileUploadToS3IndicatorToTrue(String fileName);

  @Modifying
  @Query(
      "select fileDetails from GeneratedFileDetailEntity fileDetails where fileDetails.isUploadedToS3 = false ")
  List<GeneratedFileDetailEntity> getGeneratedFileToBeUploadToS3();
  // You can define custom query methods here if needed
}
