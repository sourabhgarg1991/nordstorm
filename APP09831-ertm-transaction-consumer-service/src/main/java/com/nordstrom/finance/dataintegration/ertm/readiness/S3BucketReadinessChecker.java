package com.nordstrom.finance.dataintegration.ertm.readiness;

import com.nordstrom.finance.dataintegration.common.aws.S3Utility;
import com.nordstrom.finance.dataintegration.ertm.config.AwsServiceConfig;
import com.nordstrom.finance.dataintegration.ertm.config.S3Configuration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"!integrationTest & !loadTest"})
public class S3BucketReadinessChecker implements ExternalServiceReadinessChecker {
  @Autowired S3Configuration s3Configuration;
  AwsServiceConfig awsServiceConfig;

  @Override
  public boolean isServiceReady() {
    S3Utility s3Utility = s3Configuration.s3Utility();
    try {
      s3Utility.listFileKeys(awsServiceConfig.getSourceBucket());
      s3Utility.listFileKeys(awsServiceConfig.getProcessedBucket());
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
