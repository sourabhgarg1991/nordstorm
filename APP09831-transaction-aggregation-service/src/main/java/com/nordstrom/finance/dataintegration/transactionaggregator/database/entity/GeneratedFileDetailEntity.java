package com.nordstrom.finance.dataintegration.transactionaggregator.database.entity;

import jakarta.persistence.*;
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
@Table(name = "Generated_File_Detail")
public class GeneratedFileDetailEntity {

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @Id
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "generated_file_detail_seq_generator")
  @SequenceGenerator(
      name = "generated_file_detail_seq_generator",
      sequenceName = "generated_file_detail_generated_file_detail_id_seq",
      allocationSize = 1)
  @Column(name = "GENERATED_FILE_DETAIL_ID")
  private Long generatedFileDetailId;

  @Column(name = "AGGREGATION_CONFIGURATION_ID")
  private Long aggregationConfigurationId;

  @Column(name = "GENERATED_FILE_NAME")
  private String generatedFileName;

  @Column(name = "FILE_CONTENT", columnDefinition = "TEXT")
  private String fileContent;

  @Column(name = "IS_UPLOADED_TO_S3")
  private Boolean isUploadedToS3;

  @Column(name = "IS_PUBLISHED_TO_DATA_PLATFORM")
  private Boolean isPublishedToDataPlatform;

  @EqualsAndHashCode.Exclude
  @CreationTimestamp
  @Column(name = "CREATED_DATETIME")
  private LocalDateTime createdDatetime;

  @EqualsAndHashCode.Exclude
  @Column(name = "LAST_UPDATED_DATETIME")
  @UpdateTimestamp
  private LocalDateTime lastUpdatedDatetime;
}
