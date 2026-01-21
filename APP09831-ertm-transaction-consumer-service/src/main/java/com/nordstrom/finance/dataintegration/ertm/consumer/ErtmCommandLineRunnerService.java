package com.nordstrom.finance.dataintegration.ertm.consumer;

import com.nordstrom.finance.dataintegration.ertm.service.FileProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

/**
 * CommandLineRunnerService is called based on K8 cronjob schedule. Service is responsible for
 * calling FileProcessorService and DataLoadService to process data based on the configuration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ErtmCommandLineRunnerService implements CommandLineRunner {

  private final FileProcessorService fileProcessorService;

  /**
   * Method is responsible for checking the file extraction schedule and extract file and upload
   * data to RDS.
   *
   * @param args incoming main method arguments (No incoming arguments)
   */
  @Override
  public void run(String... args) {

    boolean isExtractSuccessful = true;
    try {
      fileProcessorService.processCsvFromS3();
    } catch (Exception e) {
      isExtractSuccessful = false;
      log.error("Error while extracting data source. Exception message: " + e.getMessage());
    } finally {
      if (isExtractSuccessful) {
        log.info("Successfully extracted all data.");
      } else {
        log.error("Failed to extract all data.");
      }
      // force exit after short delay to ensure all logs/metrics are flushed
      log.info("Cronjob completed. Application exit.");
      System.exit(0);
    }
  }
}
