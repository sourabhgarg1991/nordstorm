package com.nordstrom.finance.dataintegration.transactionaggregator.database.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Builder
@ToString
@EqualsAndHashCode
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Aggregation_Configuration")
public class AggregationConfigurationEntity {

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "aggregation_configuration_seq_generator")
  @SequenceGenerator(
      name = "aggregation_configuration_seq_generator",
      sequenceName = "aggregation_configuration_aggregation_configuration_id_seq",
      allocationSize = 1)
  @Column(name = "AGGREGATION_CONFIGURATION_ID")
  private Long aggregationConfigurationId;

  @Column(name = "FILE_NAME_PREFIX")
  private String fileNamePrefix;

  @Column(name = "FILE_DELIMITER")
  private Character fileDelimiter;

  @Column(name = "IS_DATA_QUOTES_SURROUNDED")
  private Boolean isDataQuotesSurrounded;

  @Column(name = "AGGREGATION_QUERY", columnDefinition = "TEXT")
  private String aggregationQuery;

  @Column(name = "DATA_CONTROL_QUERY", columnDefinition = "TEXT")
  private String dataControlQuery;

  @Column(name = "START_DATE")
  private LocalDate startDate;

  @Column(name = "END_DATE")
  private LocalDate endDate;

  @EqualsAndHashCode.Exclude
  @CreationTimestamp
  @Column(name = "CREATED_DATETIME")
  private LocalDateTime createdDatetime;

  @EqualsAndHashCode.Exclude
  @Column(name = "LAST_UPDATED_DATETIME")
  @UpdateTimestamp
  private LocalDateTime lastUpdatedDatetime;
}
