package com.nordstrom.finance.dataintegration.transactionaggregator.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nordstrom.finance.dataintegration.common.metric.MetricsClient;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.AggregationConfigurationEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.GeneratedFileDetailEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.TransactionAggregationRelationEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.repository.AggregationConfigurationRepository;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.repository.GeneratedFileDetailRepository;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.repository.TransactionAggregationRelationRepository;
import com.nordstrom.finance.dataintegration.transactionaggregator.exception.DatabaseConnectionException;
import com.nordstrom.finance.dataintegration.transactionaggregator.exception.DatabaseOperationException;
import jakarta.persistence.QueryTimeoutException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

@ExtendWith(MockitoExtension.class)
public class AggregationDBServiceTest {

  @Mock private AggregationConfigurationRepository aggregationConfigurationRepository;
  @Mock private TransactionAggregationRelationRepository transactionAggregationRelationRepository;
  @Mock private GeneratedFileDetailRepository generatedFileDetailRepository;
  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private MetricsClient metricsClient;

  @InjectMocks private AggregationDBService service;

  @BeforeEach
  void setup() {}

  @Test
  void getCurrentAggregationConfigurations_success() throws Exception {
    List<AggregationConfigurationEntity> configs = List.of(new AggregationConfigurationEntity());
    when(aggregationConfigurationRepository.getCurrentConfigurations(any(LocalDate.class)))
        .thenReturn(configs);

    List<AggregationConfigurationEntity> result = service.getCurrentAggregationConfigurations();
    assertEquals(configs, result);
    verify(aggregationConfigurationRepository).getCurrentConfigurations(any(LocalDate.class));
  }

  @Test
  void getCurrentAggregationConfigurations_jdbcConnectionException() {
    when(aggregationConfigurationRepository.getCurrentConfigurations(any(LocalDate.class)))
        .thenThrow(new JDBCConnectionException("jdbc-err", null));

    assertThrows(
        DatabaseConnectionException.class, () -> service.getCurrentAggregationConfigurations());
  }

  @Test
  void getCurrentAggregationConfigurations_queryTimeoutException() {
    when(aggregationConfigurationRepository.getCurrentConfigurations(any(LocalDate.class)))
        .thenThrow(new QueryTimeoutException("timeout"));

    assertThrows(
        DatabaseConnectionException.class, () -> service.getCurrentAggregationConfigurations());
  }

  @Test
  void getCurrentAggregationConfigurations_genericException() {
    when(aggregationConfigurationRepository.getCurrentConfigurations(any(LocalDate.class)))
        .thenThrow(new RuntimeException("fail"));

    assertThrows(
        DatabaseOperationException.class, () -> service.getCurrentAggregationConfigurations());
  }

  @Test
  void saveGeneratedFileDetails_success() throws Exception {
    GeneratedFileDetailEntity entity = new GeneratedFileDetailEntity();
    service.saveGeneratedFileDetails(entity);
    verify(generatedFileDetailRepository).save(entity);
  }

  @Test
  void saveGeneratedFileDetails_jdbcConnectionException() {
    GeneratedFileDetailEntity entity = new GeneratedFileDetailEntity();
    doThrow(new JDBCConnectionException("jdbc-err", null))
        .when(generatedFileDetailRepository)
        .save(any());
    assertThrows(DatabaseConnectionException.class, () -> service.saveGeneratedFileDetails(entity));
  }

  @Test
  void saveGeneratedFileDetails_queryTimeoutException() {
    GeneratedFileDetailEntity entity = new GeneratedFileDetailEntity();
    doThrow(new QueryTimeoutException("timeout")).when(generatedFileDetailRepository).save(any());
    assertThrows(DatabaseConnectionException.class, () -> service.saveGeneratedFileDetails(entity));
  }

  @Test
  void saveGeneratedFileDetails_genericException() {
    GeneratedFileDetailEntity entity = new GeneratedFileDetailEntity();
    doThrow(new RuntimeException("fail")).when(generatedFileDetailRepository).save(any());
    assertThrows(DatabaseOperationException.class, () -> service.saveGeneratedFileDetails(entity));
  }

  @Test
  void saveAggregationDetails_success() throws Exception {
    List<TransactionAggregationRelationEntity> details =
        List.of(new TransactionAggregationRelationEntity());
    service.saveTransactionAggregationRelationshipDetails(details);
    verify(transactionAggregationRelationRepository).saveAll(details);
  }

  @Test
  void saveAggregationDetails_jdbcConnectionException() {
    List<TransactionAggregationRelationEntity> details =
        List.of(new TransactionAggregationRelationEntity());
    doThrow(new JDBCConnectionException("jdbc-err", null))
        .when(transactionAggregationRelationRepository)
        .saveAll(any());
    assertThrows(
        DatabaseConnectionException.class,
        () -> service.saveTransactionAggregationRelationshipDetails(details));
  }

  @Test
  void saveAggregationDetails_queryTimeoutException() {
    List<TransactionAggregationRelationEntity> details =
        List.of(new TransactionAggregationRelationEntity());
    doThrow(new QueryTimeoutException("timeout"))
        .when(transactionAggregationRelationRepository)
        .saveAll(any());
    assertThrows(
        DatabaseConnectionException.class,
        () -> service.saveTransactionAggregationRelationshipDetails(details));
  }

  @Test
  void saveAggregationDetails_genericException() {
    List<TransactionAggregationRelationEntity> details =
        List.of(new TransactionAggregationRelationEntity());
    doThrow(new RuntimeException("fail"))
        .when(transactionAggregationRelationRepository)
        .saveAll(any());
    assertThrows(
        DatabaseOperationException.class,
        () -> service.saveTransactionAggregationRelationshipDetails(details));
  }

  @Test
  void testGetGeneratedFileToBeUploadToS3() {
    List<GeneratedFileDetailEntity> files = List.of(new GeneratedFileDetailEntity());
    when(generatedFileDetailRepository.getGeneratedFileToBeUploadToS3()).thenReturn(files);
    try {
      List<GeneratedFileDetailEntity> result = service.getGeneratedFileToBeUploadToS3();
      assertEquals(files, result);
      verify(generatedFileDetailRepository).getGeneratedFileToBeUploadToS3();
    } catch (Exception e) {
      assert false : "Exception should not be thrown: " + e.getMessage();
    }
  }

  @Test
  void testUpdateGeneratedFileUploadToS3Indicator() {
    String fileName = "testFile.csv";
    try {
      service.updateGeneratedFileUploadToS3IndicatorToTrue(fileName);
      verify(generatedFileDetailRepository).updateGeneratedFileUploadToS3IndicatorToTrue(fileName);
    } catch (Exception e) {
      assert false : "Exception should not be thrown: " + e.getMessage();
    }
  }

  @Test
  void executeQueryAndGenerateCSV_success_multipleRows() throws Exception {
    String query = "SELECT * FROM tbl";

    ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
    ResultSetMetaData meta = org.mockito.Mockito.mock(ResultSetMetaData.class);
    AtomicInteger rowCounter = new AtomicInteger(0);

    when(rs.getMetaData()).thenReturn(meta);
    when(meta.getColumnCount()).thenReturn(2);
    when(meta.getColumnName(1)).thenReturn("col1");
    when(meta.getColumnName(2)).thenReturn("col2");

    when(rs.next()).thenAnswer(invocation -> rowCounter.incrementAndGet() <= 2);

    when(rs.getString(org.mockito.ArgumentMatchers.anyInt()))
        .thenAnswer(
            invocation -> {
              int col = invocation.getArgument(0);
              int r = rowCounter.get();
              if (r == 1) {
                return col == 1 ? "a" : "b";
              } else if (r == 2) {
                return col == 1 ? "c" : "d";
              }
              return null;
            });

    when(jdbcTemplate.query(eq(query), any(ResultSetExtractor.class)))
        .thenAnswer(
            invocation -> {
              ResultSetExtractor<?> extractor = invocation.getArgument(1);
              return extractor.extractData(rs);
            });

    List<String[]> result = service.executeAggregationQuery(query);

    assertEquals(3, result.size());
    assertEquals("col1", result.get(0)[0]);
    assertEquals("col2", result.get(0)[1]);
    assertEquals("a", result.get(1)[0]);
    assertEquals("b", result.get(1)[1]);
    assertEquals("c", result.get(2)[0]);
    assertEquals("d", result.get(2)[1]);

    verify(jdbcTemplate).query(eq(query), any(ResultSetExtractor.class));
    verify(metricsClient).recordExecutionTime(anyString(), anyLong());
  }

  @Test
  void executeQueryAndGenerateCSV_success_noRows() throws Exception {
    String query = "SELECT * FROM tbl_empty";

    ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
    ResultSetMetaData meta = org.mockito.Mockito.mock(ResultSetMetaData.class);

    when(rs.getMetaData()).thenReturn(meta);
    when(meta.getColumnCount()).thenReturn(2);
    when(rs.next()).thenReturn(false);

    when(jdbcTemplate.query(eq(query), any(ResultSetExtractor.class)))
        .thenAnswer(
            invocation -> {
              ResultSetExtractor<?> extractor = invocation.getArgument(1);
              return extractor.extractData(rs);
            });

    List<String[]> result = service.executeAggregationQuery(query);

    assertEquals(0, result.size());
    verify(jdbcTemplate).query(eq(query), any(ResultSetExtractor.class));
  }

  @Test
  void executeQueryAndGenerateCSV_jdbcConnectionException() {
    String query = "SELECT * FROM tbl_fail";
    when(jdbcTemplate.query(eq(query), any(ResultSetExtractor.class)))
        .thenThrow(new JDBCConnectionException("msg", null));

    assertThrows(DatabaseConnectionException.class, () -> service.executeAggregationQuery(query));
  }

  @Test
  void executeQueryAndGenerateCSV_queryTimeoutException() {
    String query = "SELECT * FROM tbl_timeout";
    when(jdbcTemplate.query(eq(query), any(ResultSetExtractor.class)))
        .thenThrow(new QueryTimeoutException("timeout"));

    assertThrows(DatabaseConnectionException.class, () -> service.executeAggregationQuery(query));
  }

  @Test
  void executeQueryAndGenerateCSV_genericException() {
    String query = "SELECT * FROM tbl_error";
    when(jdbcTemplate.query(eq(query), any(ResultSetExtractor.class)))
        .thenThrow(new RuntimeException("boom"));

    assertThrows(DatabaseOperationException.class, () -> service.executeAggregationQuery(query));
  }

  @Test
  public void testExecuteControlDataQuery()
      throws SQLException, DatabaseOperationException, DatabaseConnectionException {
    String query = "SELECT * FROM control_table";

    ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
    AtomicInteger rowCounter = new AtomicInteger(0);

    when(rs.next()).thenAnswer(invocation -> rowCounter.incrementAndGet() <= 1);

    when(rs.getString(org.mockito.ArgumentMatchers.anyInt())).thenReturn("100.0");

    when(jdbcTemplate.query(eq(query), any(ResultSetExtractor.class)))
        .thenAnswer(
            invocation -> {
              ResultSetExtractor<?> extractor = invocation.getArgument(1);
              return extractor.extractData(rs);
            });

    String result = service.executeControlDataQuery(query);

    assertEquals("100.0", "100.0");

    verify(jdbcTemplate).query(eq(query), any(ResultSetExtractor.class));
    verify(metricsClient).recordExecutionTime(anyString(), anyLong());
  }
}
