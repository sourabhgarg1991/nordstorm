package com.nordstrom.finance.dataintegration.transactionaggregator.config;

import com.nordstrom.finance.dataintegration.transactionaggregator.database.entity.AggregationConfigurationEntity;
import com.nordstrom.finance.dataintegration.transactionaggregator.database.repository.AggregationConfigurationRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Integration test configuration that provides a filtered repository to ensure integration tests
 * only process configurations with '_Integration_Test' suffix. This prevents interference with load
 * test configurations in the database.
 */
@Slf4j
@TestConfiguration
@RequiredArgsConstructor
public class IntegrationTestRepositoryConfig {

  /**
   * Creates a wrapper around AggregationConfigurationRepository that automatically filters
   * configurations to only those with '_Integration_Test' suffix. This ensures complete isolation
   * between integration tests and load test data.
   */
  @Bean
  @Primary
  public AggregationConfigurationRepository integrationTestAggregationConfigurationRepository(
      AggregationConfigurationRepository originalRepository) {

    return new AggregationConfigurationRepository() {

      @Override
      public List<AggregationConfigurationEntity> getCurrentConfigurations(LocalDate currentDate) {
        // Get all configurations from the original repository
        List<AggregationConfigurationEntity> allConfigs =
            originalRepository.getCurrentConfigurations(currentDate);

        // Filter to only those with _Integration_Test suffix
        List<AggregationConfigurationEntity> filteredConfigs =
            allConfigs.stream()
                .filter(config -> config.getFileNamePrefix().endsWith("_Integration_Test"))
                .collect(Collectors.toList());

        log.debug(
            "Integration test: filtered {} configs to {} with _Integration_Test suffix",
            allConfigs.size(),
            filteredConfigs.size());

        return filteredConfigs;
      }

      // Delegate all other methods to the original repository
      @Override
      public <S extends AggregationConfigurationEntity> S save(S entity) {
        return originalRepository.save(entity);
      }

      @Override
      public <S extends AggregationConfigurationEntity> List<S> saveAll(Iterable<S> entities) {
        return originalRepository.saveAll(entities);
      }

      @Override
      public java.util.Optional<AggregationConfigurationEntity> findById(Long id) {
        return originalRepository.findById(id);
      }

      @Override
      public boolean existsById(Long id) {
        return originalRepository.existsById(id);
      }

      @Override
      public List<AggregationConfigurationEntity> findAll() {
        return originalRepository.findAll();
      }

      @Override
      public List<AggregationConfigurationEntity> findAllById(Iterable<Long> ids) {
        return originalRepository.findAllById(ids);
      }

      @Override
      public long count() {
        return originalRepository.count();
      }

      @Override
      public void deleteById(Long id) {
        originalRepository.deleteById(id);
      }

      @Override
      public void delete(AggregationConfigurationEntity entity) {
        originalRepository.delete(entity);
      }

      @Override
      public void deleteAllById(Iterable<? extends Long> ids) {
        originalRepository.deleteAllById(ids);
      }

      @Override
      public void deleteAll(Iterable<? extends AggregationConfigurationEntity> entities) {
        originalRepository.deleteAll(entities);
      }

      @Override
      public void deleteAll() {
        originalRepository.deleteAll();
      }

      @Override
      public void flush() {
        originalRepository.flush();
      }

      @Override
      public <S extends AggregationConfigurationEntity> S saveAndFlush(S entity) {
        return originalRepository.saveAndFlush(entity);
      }

      @Override
      public <S extends AggregationConfigurationEntity> List<S> saveAllAndFlush(
          Iterable<S> entities) {
        return originalRepository.saveAllAndFlush(entities);
      }

      @Override
      public void deleteAllInBatch(Iterable<AggregationConfigurationEntity> entities) {
        originalRepository.deleteAllInBatch(entities);
      }

      @Override
      public void deleteAllByIdInBatch(Iterable<Long> ids) {
        originalRepository.deleteAllByIdInBatch(ids);
      }

      @Override
      public void deleteAllInBatch() {
        originalRepository.deleteAllInBatch();
      }

      @Override
      public AggregationConfigurationEntity getOne(Long id) {
        return originalRepository.getOne(id);
      }

      @Override
      public AggregationConfigurationEntity getById(Long id) {
        return originalRepository.getById(id);
      }

      @Override
      public AggregationConfigurationEntity getReferenceById(Long id) {
        return originalRepository.getReferenceById(id);
      }

      @Override
      public <S extends AggregationConfigurationEntity> java.util.Optional<S> findOne(
          org.springframework.data.domain.Example<S> example) {
        return originalRepository.findOne(example);
      }

      @Override
      public <S extends AggregationConfigurationEntity> List<S> findAll(
          org.springframework.data.domain.Example<S> example) {
        return originalRepository.findAll(example);
      }

      @Override
      public <S extends AggregationConfigurationEntity> List<S> findAll(
          org.springframework.data.domain.Example<S> example,
          org.springframework.data.domain.Sort sort) {
        return originalRepository.findAll(example, sort);
      }

      @Override
      public <S extends AggregationConfigurationEntity>
          org.springframework.data.domain.Page<S> findAll(
              org.springframework.data.domain.Example<S> example,
              org.springframework.data.domain.Pageable pageable) {
        return originalRepository.findAll(example, pageable);
      }

      @Override
      public <S extends AggregationConfigurationEntity> long count(
          org.springframework.data.domain.Example<S> example) {
        return originalRepository.count(example);
      }

      @Override
      public <S extends AggregationConfigurationEntity> boolean exists(
          org.springframework.data.domain.Example<S> example) {
        return originalRepository.exists(example);
      }

      @Override
      public <S extends AggregationConfigurationEntity, R> R findBy(
          org.springframework.data.domain.Example<S> example,
          java.util.function.Function<
                  org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R>
              queryFunction) {
        return originalRepository.findBy(example, queryFunction);
      }

      @Override
      public List<AggregationConfigurationEntity> findAll(
          org.springframework.data.domain.Sort sort) {
        return originalRepository.findAll(sort);
      }

      @Override
      public org.springframework.data.domain.Page<AggregationConfigurationEntity> findAll(
          org.springframework.data.domain.Pageable pageable) {
        return originalRepository.findAll(pageable);
      }
    };
  }
}
