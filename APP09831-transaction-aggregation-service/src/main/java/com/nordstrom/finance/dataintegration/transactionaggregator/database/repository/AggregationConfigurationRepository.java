package com.nordstrom.finance.dataintegration.transactionaggregator.database.repository;

import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.AggregationConfigurationEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing AggregationConfigurationEntity entities. This interface extends
 * JpaRepository to provide CRUD operations.
 */
@Repository
public interface AggregationConfigurationRepository
    extends JpaRepository<AggregationConfigurationEntity, Long> {
  @Query(
      """
           SELECT a FROM AggregationConfigurationEntity a
           WHERE a.startDate <= :currentDate AND (a.endDate IS NULL OR a.endDate >= :currentDate)
        """)
  List<AggregationConfigurationEntity> getCurrentConfigurations(LocalDate currentDate);
}
