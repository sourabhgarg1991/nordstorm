package com.nordstrom.finance.dataintegration.common.aws;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

/**
 * Instance-based S3 Service for uploading files to AWS S3. Thread-safe singleton pattern. This
 * utility is configured to work exclusively with the us-west-2 region.
 *
 * <p>Available Methods:
 *
 * <ul>
 *   <li>uploadFile - Upload file to S3
 *   <li>downloadFileAsStream - Get file as InputStream
 *   <li>listAllObject - Get all S3Objects with metadata
 *   <li>moveFileToAnotherBucket - Move file between buckets
 *   <li>deleteFile - Delete file from S3
 *   <li>listFileKeys - Get all file keys from bucket
 *   <li>fileExists - Check if file exists in bucket
 * </ul>
 */
@Slf4j
public class S3Utility {

  private static final Region AWS_REGION = Region.US_WEST_2;

  private static volatile S3Utility instance;
  private volatile S3Client s3Client;

  private static final Map<String, String> EXTENSION_TO_CONTENT_TYPE =
      Map.ofEntries(
          Map.entry("csv", "text/csv"),
          Map.entry("json", "application/json"),
          Map.entry("xml", "application/xml"),
          Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
          Map.entry("xls", "application/vnd.ms-excel"),
          Map.entry("txt", "text/plain"),
          Map.entry("pdf", "application/pdf"),
          Map.entry("jpg", "image/jpeg"),
          Map.entry("jpeg", "image/jpeg"),
          Map.entry("png", "image/png"),
          Map.entry("gif", "image/gif"));

  private S3Utility() {}

  /**
   * Returns the singleton instance of S3Utility. Thread-safe implementation using double-checked
   * locking pattern.
   *
   * @return the singleton instance of S3Utility
   */
  public static S3Utility getInstance() {
    if (instance == null) {
      synchronized (S3Utility.class) {
        if (instance == null) {
          instance = new S3Utility();
        }
      }
    }
    return instance;
  }

  private S3Client getS3Client() {
    if (s3Client == null) {
      synchronized (this) {
        if (s3Client == null) {
          try {
            log.debug("Creating S3 Client for region: {}", AWS_REGION);
            s3Client = S3Client.builder().region(AWS_REGION).build();
          } catch (Exception e) {
            log.error("Error initiating S3Client for region {}: {}", AWS_REGION, e.getMessage(), e);
            throw new RuntimeException("Failed to create S3Client for region: " + AWS_REGION, e);
          }
        }
      }
    }
    return s3Client;
  }

  /**
   * Closes the S3 client and releases resources. This method is idempotent and can be called
   * multiple times safely. After calling this method, a new S3 client will be created on the next
   * operation.
   */
  public void closeS3Client() {
    if (s3Client != null) {
      synchronized (this) {
        if (s3Client != null) {
          try {
            log.debug("Closing S3 Client");
            s3Client.close();
          } catch (Exception e) {
            log.warn("Error closing S3 client: {}", e.getMessage(), e);
          } finally {
            s3Client = null;
          }
        }
      }
    }
  }

  /**
   * Uploads a file to the specified S3 bucket with server-side encryption (AES256). The content
   * type is automatically determined based on the file extension.
   *
   * @param fileContent the content of the file to upload as a String
   * @param fileName the name/key of the file in S3 (including path if applicable)
   * @param bucketName the name of the S3 bucket
   * @return true if the upload was successful, false otherwise
   */
  public boolean uploadFile(String fileContent, String fileName, String bucketName) {
    log.info(
        "Attempting to upload file {} to S3 bucket {} in region {}",
        fileName,
        bucketName,
        AWS_REGION);

    try {
      validateInputs(fileName, bucketName);

      if (fileContent == null || fileContent.isEmpty()) {
        throw new IllegalArgumentException("fileContent cannot be null or empty");
      }

      S3Client client = getS3Client();

      byte[] fileBytes = fileContent.getBytes(StandardCharsets.UTF_8);

      String contentType = getContentType(fileName);

      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(bucketName)
              .key(fileName)
              .contentType(contentType)
              .serverSideEncryption(ServerSideEncryption.AES256)
              .build();

      PutObjectResponse response =
          client.putObject(
              putObjectRequest,
              RequestBody.fromInputStream(new ByteArrayInputStream(fileBytes), fileBytes.length));

      log.info(
          "File {} uploaded successfully to S3 bucket {}. ETag: {}",
          fileName,
          bucketName,
          response.eTag());
      return true;

    } catch (IllegalArgumentException e) {
      log.error(
          "Invalid input for uploading file {} to bucket {}: {}",
          fileName,
          bucketName,
          e.getMessage());
      return false;
    } catch (S3Exception e) {
      log.error(
          "S3 error uploading file {} to bucket {}: {}",
          fileName,
          bucketName,
          e.awsErrorDetails().errorMessage(),
          e);
      return false;
    } catch (Exception e) {
      log.error(
          "Error uploading file {} to S3 bucket {}: {}", fileName, bucketName, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Downloads a file from S3 and returns it as a ResponseInputStream. The caller is responsible for
   * closing the returned stream.
   *
   * @param fileName the name/key of the file to download from S3
   * @param bucketName the name of the S3 bucket
   * @return ResponseInputStream containing the file data, or null if an error occurs
   */
  public ResponseInputStream<GetObjectResponse> downloadFileAsStream(
      final String fileName, final String bucketName) {
    log.info("Attempting to read file {} from S3 bucket {}", fileName, bucketName);

    try {
      validateInputs(fileName, bucketName);

      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(bucketName).key(fileName).build();
      S3Client client = getS3Client();
      ResponseInputStream<GetObjectResponse> file = client.getObject(getObjectRequest);
      log.info("Successfully retrieved file {} from S3", fileName);
      return file;
    } catch (Exception e) {
      log.error("Unable to read file {} from bucket: {}", fileName, e.getMessage(), e);
      return null;
    }
  }

  /**
   * Lists all S3 objects in the specified bucket with their metadata. This method uses pagination
   * to handle buckets with large numbers of objects.
   *
   * @param bucketName the name of the S3 bucket
   * @return a list of S3Object instances containing object metadata, or an empty list if the bucket
   *     is empty or an error occurs
   */
  public List<S3Object> listAllObject(final String bucketName) {
    log.info(
        "Attempting to get all S3 objects from bucket {} in region {}", bucketName, AWS_REGION);

    try {
      if (bucketName == null || bucketName.trim().isEmpty()) {
        throw new IllegalArgumentException("bucketName cannot be null or empty");
      }

      S3Client client = getS3Client();

      ListObjectsV2Request listObjectsRequest =
          ListObjectsV2Request.builder().bucket(bucketName).build();

      List<S3Object> filteredObjects =
          client.listObjectsV2Paginator(listObjectsRequest).contents().stream().toList();

      if (filteredObjects.isEmpty()) {
        log.info("No files found in bucket: {}", bucketName);
      } else {
        log.info("Files available in bucket: {}", bucketName);
      }
      return filteredObjects;

    } catch (IllegalArgumentException e) {
      log.error("Invalid input for getting objects from bucket {}: {}", bucketName, e.getMessage());
      return Collections.emptyList();
    } catch (S3Exception e) {
      log.error(
          "S3 error getting objects from bucket {}: {}",
          bucketName,
          e.awsErrorDetails().errorMessage(),
          e);
      return Collections.emptyList();
    } catch (Exception e) {
      log.error("Error listing S3 objects from bucket {}: {}", bucketName, e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Moves a file from one S3 bucket to another by copying and then deleting the original. The
   * destination file will have server-side encryption (AES256) applied. If the copy succeeds but
   * delete fails, the file will exist in both locations.
   *
   * @param fileName the name/key of the file to move
   * @param sourceBucketName the name of the source S3 bucket
   * @param destinationBucketName the name of the destination S3 bucket
   * @return true if the move (copy + delete) was successful, false otherwise
   */
  public boolean moveFileToAnotherBucket(
      String fileName, String sourceBucketName, String destinationBucketName) {
    log.info(
        "Attempting to move file {} from bucket {} to bucket {} in region {}",
        fileName,
        sourceBucketName,
        destinationBucketName,
        AWS_REGION);

    try {
      validateInputs(fileName, sourceBucketName);
      if (destinationBucketName == null || destinationBucketName.trim().isEmpty()) {
        throw new IllegalArgumentException("destinationBucketName cannot be null or empty");
      }

      S3Client client = getS3Client();

      CopyObjectRequest copyObjectRequest =
          CopyObjectRequest.builder()
              .sourceBucket(sourceBucketName)
              .sourceKey(fileName)
              .destinationBucket(destinationBucketName)
              .destinationKey(fileName)
              .serverSideEncryption(ServerSideEncryption.AES256)
              .build();

      CopyObjectResponse copyResponse = client.copyObject(copyObjectRequest);
      log.info(
          "File {} copied to bucket {} with ETag: {}",
          fileName,
          destinationBucketName,
          copyResponse.copyObjectResult().eTag());

      DeleteObjectRequest deleteObjectRequest =
          DeleteObjectRequest.builder().bucket(sourceBucketName).key(fileName).build();

      client.deleteObject(deleteObjectRequest);
      log.info("File {} deleted from source bucket {}", fileName, sourceBucketName);

      log.info(
          "File {} moved successfully from bucket {} to bucket {}",
          fileName,
          sourceBucketName,
          destinationBucketName);
      return true;

    } catch (IllegalArgumentException e) {
      log.error("Invalid input for moving file {}: {}", fileName, e.getMessage());
      return false;
    } catch (S3Exception e) {
      log.error(
          "S3 error moving file {} from bucket {} to bucket {}: {}",
          fileName,
          sourceBucketName,
          destinationBucketName,
          e.awsErrorDetails().errorMessage(),
          e);
      // If error occurred during copy, file is only in source (safe)
      // If error occurred during delete, file is in BOTH locations (logged above)
      return false;
    } catch (Exception e) {
      log.error(
          "Error moving file {} from bucket {} to bucket {}: {}",
          fileName,
          sourceBucketName,
          destinationBucketName,
          e.getMessage(),
          e);
      return false;
    }
  }

  /**
   * Deletes a file from the specified S3 bucket.
   *
   * @param fileName the name/key of the file to delete
   * @param bucketName the name of the S3 bucket
   * @return true if the deletion was successful, false otherwise
   */
  public boolean deleteFile(String fileName, String bucketName) {
    log.info(
        "Attempting to delete file {} from S3 bucket {} in region {}",
        fileName,
        bucketName,
        AWS_REGION);

    try {
      validateInputs(fileName, bucketName);

      S3Client client = getS3Client();

      DeleteObjectRequest deleteObjectRequest =
          DeleteObjectRequest.builder().bucket(bucketName).key(fileName).build();

      client.deleteObject(deleteObjectRequest);

      log.info("File {} deleted successfully from bucket {}", fileName, bucketName);
      return true;

    } catch (IllegalArgumentException e) {
      log.error(
          "Invalid input for deleting file {} from bucket {}: {}",
          fileName,
          bucketName,
          e.getMessage());
      return false;
    } catch (S3Exception e) {
      log.error(
          "S3 error deleting file {} from bucket {}: {}",
          fileName,
          bucketName,
          e.awsErrorDetails().errorMessage(),
          e);
      return false;
    } catch (Exception e) {
      log.error(
          "Error deleting file {} from S3 bucket {}: {}", fileName, bucketName, e.getMessage(), e);
      return false;
    }
  }

  /**
   * Lists all file keys (object names) in the specified S3 bucket. This method uses pagination to
   * handle buckets with large numbers of objects.
   *
   * @param bucketName the name of the S3 bucket
   * @return a list of file keys (object names), or an empty list if the bucket is empty or an error
   *     occurs
   */
  public List<String> listFileKeys(String bucketName) {
    log.info("Attempting to get all files from S3 bucket {} in region {}", bucketName, AWS_REGION);
    List<String> fileNames = new ArrayList<>();

    try {
      if (bucketName == null || bucketName.trim().isEmpty()) {
        throw new IllegalArgumentException("bucketName cannot be null or empty");
      }

      S3Client client = getS3Client();

      ListObjectsV2Request listObjectsRequest =
          ListObjectsV2Request.builder().bucket(bucketName).build();

      ListObjectsV2Iterable listObjectsResponse = client.listObjectsV2Paginator(listObjectsRequest);

      for (ListObjectsV2Response page : listObjectsResponse) {
        for (S3Object s3Object : page.contents()) {
          fileNames.add(s3Object.key());
        }
      }

      log.info("Retrieved {} files from bucket {}", fileNames.size(), bucketName);
      return fileNames;

    } catch (IllegalArgumentException e) {
      log.error("Invalid input for getting files from bucket {}: {}", bucketName, e.getMessage());
      return fileNames;
    } catch (S3Exception e) {
      log.error(
          "S3 error getting files from bucket {}: {}",
          bucketName,
          e.awsErrorDetails().errorMessage(),
          e);
      return fileNames;
    } catch (Exception e) {
      log.error("Error getting files from S3 bucket {}: {}", bucketName, e.getMessage(), e);
      return fileNames;
    }
  }

  /**
   * Checks if a file exists in the specified S3 bucket.
   *
   * @param fileName the name/key of the file to check
   * @param bucketName the name of the S3 bucket
   * @return true if the file exists, false otherwise (including error cases)
   */
  public boolean fileExists(String fileName, String bucketName) {
    log.info(
        "Checking if file {} exists in S3 bucket {} in region {}",
        fileName,
        bucketName,
        AWS_REGION);

    try {
      validateInputs(fileName, bucketName);

      S3Client client = getS3Client();

      HeadObjectRequest headObjectRequest =
          HeadObjectRequest.builder().bucket(bucketName).key(fileName).build();

      client.headObject(headObjectRequest);

      log.info("File {} exists in bucket {}", fileName, bucketName);
      return true;

    } catch (NoSuchKeyException e) {
      log.info("File {} does not exist in bucket {}", fileName, bucketName);
      return false;
    } catch (IllegalArgumentException e) {
      log.error(
          "Invalid input for checking file {} in bucket {}: {}",
          fileName,
          bucketName,
          e.getMessage());
      return false;
    } catch (S3Exception e) {
      log.error(
          "S3 error checking if file {} exists in bucket {}: {}",
          fileName,
          bucketName,
          e.awsErrorDetails().errorMessage(),
          e);
      return false;
    } catch (Exception e) {
      log.error(
          "Error checking if file {} exists in S3 bucket {}: {}",
          fileName,
          bucketName,
          e.getMessage(),
          e);
      return false;
    }
  }

  /** Validates input parameters */
  private void validateInputs(String fileName, String bucketName) {
    if (fileName == null || fileName.trim().isEmpty()) {
      throw new IllegalArgumentException("fileName cannot be null or empty");
    }
    if (bucketName == null || bucketName.trim().isEmpty()) {
      throw new IllegalArgumentException("bucketName cannot be null or empty");
    }
  }

  /** Determines content type based on file extension */
  private String getContentType(String fileName) {
    if (fileName == null || fileName.trim().isEmpty()) return "application/octet-stream";
    String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    return EXTENSION_TO_CONTENT_TYPE.getOrDefault(ext, "application/octet-stream");
  }
}
