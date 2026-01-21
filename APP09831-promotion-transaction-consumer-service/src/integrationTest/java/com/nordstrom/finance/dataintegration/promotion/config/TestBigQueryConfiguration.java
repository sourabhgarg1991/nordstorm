package com.nordstrom.finance.dataintegration.promotion.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.JobStatus;
import com.google.cloud.bigquery.TableResult;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Test configuration that provides a pre-configured mocked BigQuery bean for integration tests.
 * This ensures BigQuery is available without requiring GCP credentials and won't fail if
 * accidentally invoked during context initialization.
 */
@TestConfiguration
@Profile("integration-test")
public class TestBigQueryConfiguration {

  @Bean
  @Primary
  public BigQuery mockBigQuery() throws InterruptedException {
    BigQuery bigQuery = Mockito.mock(BigQuery.class);
    Job mockJob = Mockito.mock(Job.class);
    JobStatus mockStatus = Mockito.mock(JobStatus.class);
    TableResult emptyResult = Mockito.mock(TableResult.class);

    // Pre-configure basic mock behavior to prevent failures during context load
    when(mockStatus.getError()).thenReturn(null);
    when(mockJob.getStatus()).thenReturn(mockStatus);
    when(mockJob.waitFor()).thenReturn(mockJob);
    when(mockJob.getQueryResults(any())).thenReturn(emptyResult);
    when(bigQuery.create(any(JobInfo.class))).thenReturn(mockJob);

    // Configure empty result
    when(emptyResult.getValues()).thenReturn(java.util.Collections.emptyList());
    when(emptyResult.getNextPageToken()).thenReturn(null);

    return bigQuery;
  }
}
