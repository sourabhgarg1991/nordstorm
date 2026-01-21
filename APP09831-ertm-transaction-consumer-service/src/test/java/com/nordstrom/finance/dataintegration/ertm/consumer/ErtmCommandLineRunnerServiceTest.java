package com.nordstrom.finance.dataintegration.ertm.consumer;

import static org.mockito.Mockito.*;

import com.nordstrom.finance.dataintegration.ertm.service.FileProcessorService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {ErtmCommandLineRunnerService.class})
@ActiveProfiles("test")
public class ErtmCommandLineRunnerServiceTest {

  @MockitoBean private FileProcessorService fileProcessorService;

  @InjectMocks private ErtmCommandLineRunnerService ertmCommandLineRunnerService;

  @Test
  public void runTest_success() {
    Mockito.doNothing().when(fileProcessorService).processCsvFromS3();

    ertmCommandLineRunnerService.run();

    verify(fileProcessorService, times(1)).processCsvFromS3();
  }
}
