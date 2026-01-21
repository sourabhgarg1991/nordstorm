package com.nordstrom.finance.dataintegration.common.aws;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

@ExtendWith(MockitoExtension.class)
class S3UtilityTest {

  private S3Utility s3Utility;
  private S3Client mockS3Client;
  private S3ClientBuilder mockS3ClientBuilder;
  private MockedStatic<S3Client> s3ClientMockedStatic;

  @BeforeEach
  void setUp() throws Exception {
    resetSingleton();
    s3Utility = S3Utility.getInstance();
    mockS3Client = mock(S3Client.class);
    mockS3ClientBuilder = mock(S3ClientBuilder.class);

    s3ClientMockedStatic = mockStatic(S3Client.class);
    s3ClientMockedStatic.when(S3Client::builder).thenReturn(mockS3ClientBuilder);
    lenient().when(mockS3ClientBuilder.region(any(Region.class))).thenReturn(mockS3ClientBuilder);
    lenient().when(mockS3ClientBuilder.build()).thenReturn(mockS3Client);
  }

  @AfterEach
  void tearDown() throws Exception {
    if (s3ClientMockedStatic != null) {
      s3ClientMockedStatic.close();
    }
    if (s3Utility != null) {
      s3Utility.closeS3Client();
    }
    resetSingleton();
  }

  /** Helper method to reset the singleton instance using reflection for testing purposes */
  private void resetSingleton() throws Exception {
    Field instanceField = S3Utility.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);

    Field s3ClientField = S3Utility.class.getDeclaredField("s3Client");
    s3ClientField.setAccessible(true);
    if (s3Utility != null) {
      s3ClientField.set(s3Utility, null);
    }
  }

  @Test
  void testGetInstance_Singleton() {
    S3Utility instance1 = S3Utility.getInstance();
    S3Utility instance2 = S3Utility.getInstance();

    assertNotNull(instance1, "getInstance should return a non-null instance");
    assertSame(instance1, instance2, "getInstance should return the same instance");
  }

  @Test
  void testUploadFile_Success_WithVariousContentTypes() {
    String fileContent = "test,data,content\n1,2,3";
    String csvFile = "test.csv";
    String jsonFile = "test.json";
    String unknownFile = "test.unknown";
    String bucketName = "test-bucket";

    PutObjectResponse mockResponse = PutObjectResponse.builder().eTag("test-etag-123").build();
    when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(mockResponse);

    assertTrue(s3Utility.uploadFile(fileContent, csvFile, bucketName));
    ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(mockS3Client, atLeastOnce()).putObject(captor.capture(), any(RequestBody.class));
    assertEquals("text/csv", captor.getAllValues().get(0).contentType());

    assertTrue(s3Utility.uploadFile(fileContent, jsonFile, bucketName));
    verify(mockS3Client, atLeastOnce()).putObject(captor.capture(), any(RequestBody.class));
    assertTrue(
        captor.getAllValues().stream()
            .anyMatch(req -> "application/json".equals(req.contentType())));

    assertTrue(s3Utility.uploadFile(fileContent, unknownFile, bucketName));
    verify(mockS3Client, atLeastOnce()).putObject(captor.capture(), any(RequestBody.class));
    assertTrue(
        captor.getAllValues().stream()
            .anyMatch(req -> "application/octet-stream".equals(req.contentType())));
  }

  @Test
  void testUploadFile_InvalidInputs() {
    String fileContent = "test,data";
    String fileName = "test.csv";
    String bucketName = "test-bucket";

    assertFalse(s3Utility.uploadFile(null, fileName, bucketName));

    assertFalse(s3Utility.uploadFile("", fileName, bucketName));

    assertFalse(s3Utility.uploadFile(fileContent, null, bucketName));

    assertFalse(s3Utility.uploadFile(fileContent, "", bucketName));

    assertFalse(s3Utility.uploadFile(fileContent, "  ", bucketName));

    assertFalse(s3Utility.uploadFile(fileContent, fileName, null));

    assertFalse(s3Utility.uploadFile(fileContent, fileName, ""));

    assertFalse(s3Utility.uploadFile(fileContent, fileName, "  "));
  }

  @Test
  void testUploadFile_S3Exception_And_GenericException() {
    String fileContent = "test,data";
    String fileName = "test.csv";
    String bucketName = "test-bucket";

    AwsErrorDetails errorDetails =
        AwsErrorDetails.builder().errorCode("AccessDenied").errorMessage("Access Denied").build();
    S3Exception s3Exception =
        (S3Exception)
            S3Exception.builder().awsErrorDetails(errorDetails).message("Access Denied").build();
    when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(s3Exception);

    assertFalse(s3Utility.uploadFile(fileContent, fileName, bucketName));

    when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(new RuntimeException("Unexpected error"));

    assertFalse(s3Utility.uploadFile(fileContent, fileName, bucketName));
  }

  @Test
  void testDownloadFileAsStream_Success_And_InvalidInputs() {
    String fileName = "test.csv";
    String bucketName = "test-bucket";

    GetObjectResponse getObjectResponse = GetObjectResponse.builder().build();
    ResponseInputStream<GetObjectResponse> mockStream =
        new ResponseInputStream<>(
            getObjectResponse, new ByteArrayInputStream("test content".getBytes()));

    when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockStream);

    ResponseInputStream<GetObjectResponse> result =
        s3Utility.downloadFileAsStream(fileName, bucketName);
    assertNotNull(result, "Stream should not be null");

    assertNull(
        s3Utility.downloadFileAsStream(null, bucketName), "Should return null for null fileName");
    assertNull(
        s3Utility.downloadFileAsStream("", bucketName), "Should return null for empty fileName");
    assertNull(
        s3Utility.downloadFileAsStream("  ", bucketName),
        "Should return null for whitespace fileName");
    assertNull(
        s3Utility.downloadFileAsStream(fileName, null), "Should return null for null bucketName");
    assertNull(
        s3Utility.downloadFileAsStream(fileName, ""), "Should return null for empty bucketName");

    when(mockS3Client.getObject(any(GetObjectRequest.class)))
        .thenThrow(new RuntimeException("Error"));
    assertNull(
        s3Utility.downloadFileAsStream(fileName, bucketName),
        "Should return null when exception is thrown");
  }

  @Test
  void testListAllObject_Success_And_AllErrors() {
    String bucketName = "test-bucket";

    S3Object obj1 = S3Object.builder().key("file1.txt").size(100L).build();
    S3Object obj2 = S3Object.builder().key("file2.txt").size(200L).build();

    ListObjectsV2Iterable mockIterable = mock(ListObjectsV2Iterable.class);
    when(mockS3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
        .thenReturn(mockIterable);

    @SuppressWarnings("unchecked")
    software.amazon.awssdk.core.pagination.sync.SdkIterable<S3Object> mockContents =
        mock(software.amazon.awssdk.core.pagination.sync.SdkIterable.class);
    when(mockIterable.contents()).thenReturn(mockContents);
    when(mockContents.stream()).thenReturn(List.of(obj1, obj2).stream());

    List<S3Object> result = s3Utility.listAllObject(bucketName);
    assertEquals(2, result.size(), "Should return 2 objects");
    assertEquals("file1.txt", result.get(0).key());
    assertEquals("file2.txt", result.get(1).key());

    when(mockContents.stream()).thenReturn(List.<S3Object>of().stream());
    result = s3Utility.listAllObject(bucketName);
    assertTrue(result.isEmpty(), "Should return empty list when no objects are found");

    assertTrue(
        s3Utility.listAllObject(null).isEmpty(), "Should return empty list for null bucketName");
    assertTrue(
        s3Utility.listAllObject("").isEmpty(), "Should return empty list for empty bucketName");
    assertTrue(
        s3Utility.listAllObject("  ").isEmpty(),
        "Should return empty list for whitespace bucketName");

    AwsErrorDetails errorDetails =
        AwsErrorDetails.builder()
            .errorCode("NoSuchBucket")
            .errorMessage("Bucket not found")
            .build();
    S3Exception s3Exception =
        (S3Exception)
            S3Exception.builder().awsErrorDetails(errorDetails).message("Bucket not found").build();
    when(mockS3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
        .thenThrow(s3Exception);
    assertTrue(
        s3Utility.listAllObject(bucketName).isEmpty(),
        "Should return empty list when S3Exception is thrown");

    when(mockS3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
        .thenThrow(new RuntimeException("Unexpected error"));
    assertTrue(
        s3Utility.listAllObject(bucketName).isEmpty(),
        "Should return empty list when generic exception is thrown");
  }

  @Test
  void testListFileKeys_Success_And_AllErrors() {
    String bucketName = "test-bucket";

    S3Object obj1 = S3Object.builder().key("file1.txt").build();
    S3Object obj2 = S3Object.builder().key("file2.txt").build();

    ListObjectsV2Response response = ListObjectsV2Response.builder().contents(obj1, obj2).build();

    ListObjectsV2Iterable mockIterable = mock(ListObjectsV2Iterable.class);
    when(mockS3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
        .thenReturn(mockIterable);
    when(mockIterable.iterator()).thenReturn(List.of(response).iterator());

    List<String> result = s3Utility.listFileKeys(bucketName);
    assertEquals(2, result.size(), "Should return 2 file keys");
    assertTrue(result.contains("file1.txt"));
    assertTrue(result.contains("file2.txt"));

    assertTrue(
        s3Utility.listFileKeys(null).isEmpty(), "Should return empty list for null bucketName");
    assertTrue(
        s3Utility.listFileKeys("").isEmpty(), "Should return empty list for empty bucketName");
    assertTrue(
        s3Utility.listFileKeys("  ").isEmpty(),
        "Should return empty list for whitespace bucketName");

    AwsErrorDetails errorDetails =
        AwsErrorDetails.builder()
            .errorCode("NoSuchBucket")
            .errorMessage("Bucket not found")
            .build();
    S3Exception s3Exception =
        (S3Exception)
            S3Exception.builder().awsErrorDetails(errorDetails).message("Bucket not found").build();
    when(mockS3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
        .thenThrow(s3Exception);
    assertTrue(
        s3Utility.listFileKeys(bucketName).isEmpty(),
        "Should return empty list when S3Exception is thrown");

    when(mockS3Client.listObjectsV2Paginator(any(ListObjectsV2Request.class)))
        .thenThrow(new RuntimeException("Unexpected error"));
    assertTrue(
        s3Utility.listFileKeys(bucketName).isEmpty(),
        "Should return empty list when generic exception is thrown");
  }

  @Test
  void testMoveFileToAnotherBucket_Success() {
    String fileName = "test.csv";
    String sourceBucket = "source-bucket";
    String destBucket = "dest-bucket";

    CopyObjectResponse copyResponse =
        CopyObjectResponse.builder()
            .copyObjectResult(CopyObjectResult.builder().eTag("etag-123").build())
            .build();

    when(mockS3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(copyResponse);
    when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenReturn(DeleteObjectResponse.builder().build());

    assertTrue(
        s3Utility.moveFileToAnotherBucket(fileName, sourceBucket, destBucket),
        "Move should be successful");
    verify(mockS3Client).copyObject(any(CopyObjectRequest.class));
    verify(mockS3Client).deleteObject(any(DeleteObjectRequest.class));
  }

  @Test
  void testMoveFileToAnotherBucket_InvalidInputs_And_Exceptions() {
    String fileName = "test.csv";
    String sourceBucket = "source-bucket";
    String destBucket = "dest-bucket";

    assertFalse(
        s3Utility.moveFileToAnotherBucket(null, sourceBucket, destBucket),
        "Should fail with null fileName");
    assertFalse(
        s3Utility.moveFileToAnotherBucket("", sourceBucket, destBucket),
        "Should fail with empty fileName");
    assertFalse(
        s3Utility.moveFileToAnotherBucket("  ", sourceBucket, destBucket),
        "Should fail with whitespace fileName");

    assertFalse(
        s3Utility.moveFileToAnotherBucket(fileName, null, destBucket),
        "Should fail with null sourceBucket");
    assertFalse(
        s3Utility.moveFileToAnotherBucket(fileName, "", destBucket),
        "Should fail with empty sourceBucket");
    assertFalse(
        s3Utility.moveFileToAnotherBucket(fileName, "  ", destBucket),
        "Should fail with whitespace sourceBucket");

    assertFalse(
        s3Utility.moveFileToAnotherBucket(fileName, sourceBucket, null),
        "Should fail with null destBucket");
    assertFalse(
        s3Utility.moveFileToAnotherBucket(fileName, sourceBucket, ""),
        "Should fail with empty destBucket");
    assertFalse(
        s3Utility.moveFileToAnotherBucket(fileName, sourceBucket, "  "),
        "Should fail with whitespace destBucket");

    AwsErrorDetails errorDetails =
        AwsErrorDetails.builder().errorCode("AccessDenied").errorMessage("Access denied").build();
    S3Exception s3Exception =
        (S3Exception)
            S3Exception.builder().awsErrorDetails(errorDetails).message("Access denied").build();
    when(mockS3Client.copyObject(any(CopyObjectRequest.class))).thenThrow(s3Exception);
    assertFalse(
        s3Utility.moveFileToAnotherBucket(fileName, sourceBucket, destBucket),
        "Should fail when copy fails");
    verify(mockS3Client, never()).deleteObject(any(DeleteObjectRequest.class));

    when(mockS3Client.copyObject(any(CopyObjectRequest.class)))
        .thenThrow(new RuntimeException("Unexpected error"));
    assertFalse(
        s3Utility.moveFileToAnotherBucket(fileName, sourceBucket, destBucket),
        "Should fail when generic exception is thrown");
  }

  @Test
  void testDeleteFile_Success_And_AllErrors() {
    String fileName = "test.csv";
    String bucketName = "test-bucket";

    when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenReturn(DeleteObjectResponse.builder().build());

    assertTrue(s3Utility.deleteFile(fileName, bucketName), "Delete should be successful");

    assertFalse(s3Utility.deleteFile(null, bucketName), "Should fail with null fileName");
    assertFalse(s3Utility.deleteFile("", bucketName), "Should fail with empty fileName");
    assertFalse(s3Utility.deleteFile("  ", bucketName), "Should fail with whitespace fileName");
    assertFalse(s3Utility.deleteFile(fileName, null), "Should fail with null bucketName");
    assertFalse(s3Utility.deleteFile(fileName, ""), "Should fail with empty bucketName");
    assertFalse(s3Utility.deleteFile(fileName, "  "), "Should fail with whitespace bucketName");

    AwsErrorDetails errorDetails =
        AwsErrorDetails.builder().errorCode("AccessDenied").errorMessage("Access denied").build();
    S3Exception s3Exception =
        (S3Exception)
            S3Exception.builder().awsErrorDetails(errorDetails).message("Access denied").build();
    when(mockS3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(s3Exception);
    assertFalse(
        s3Utility.deleteFile(fileName, bucketName), "Should fail when S3Exception is thrown");

    when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenThrow(new RuntimeException("Unexpected error"));
    assertFalse(
        s3Utility.deleteFile(fileName, bucketName), "Should fail when generic exception is thrown");
  }

  @Test
  void testFileExists_Success_NoSuchKey_And_AllErrors() {
    String fileName = "test.csv";
    String bucketName = "test-bucket";

    when(mockS3Client.headObject(any(HeadObjectRequest.class)))
        .thenReturn(HeadObjectResponse.builder().build());

    assertTrue(s3Utility.fileExists(fileName, bucketName), "File should exist");

    NoSuchKeyException noSuchKeyException =
        NoSuchKeyException.builder().message("Key not found").build();
    when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenThrow(noSuchKeyException);
    assertFalse(s3Utility.fileExists(fileName, bucketName), "File should not exist");

    assertFalse(s3Utility.fileExists(null, bucketName), "Should return false for null fileName");
    assertFalse(s3Utility.fileExists("", bucketName), "Should return false for empty fileName");
    assertFalse(
        s3Utility.fileExists("  ", bucketName), "Should return false for whitespace fileName");
    assertFalse(s3Utility.fileExists(fileName, null), "Should return false for null bucketName");
    assertFalse(s3Utility.fileExists(fileName, ""), "Should return false for empty bucketName");

    AwsErrorDetails errorDetails =
        AwsErrorDetails.builder().errorCode("AccessDenied").errorMessage("Access denied").build();
    S3Exception s3Exception =
        (S3Exception)
            S3Exception.builder().awsErrorDetails(errorDetails).message("Access denied").build();
    when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenThrow(s3Exception);

    assertFalse(
        s3Utility.fileExists(fileName, bucketName),
        "Should return false when S3Exception is thrown");
  }

  @Test
  void testCloseS3Client_Success_And_ExceptionHandling() {
    String fileContent = "test";
    String fileName = "test.csv";
    String bucketName = "bucket";

    PutObjectResponse mockResponse = PutObjectResponse.builder().eTag("etag").build();
    when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(mockResponse);

    s3Utility.uploadFile(fileContent, fileName, bucketName);

    s3Utility.closeS3Client();
    verify(mockS3Client).close();

    s3Utility.closeS3Client();

    s3Utility.uploadFile(fileContent, fileName, bucketName);
    doThrow(new RuntimeException("Close error")).when(mockS3Client).close();
    s3Utility.closeS3Client();
  }

  @Test
  void testS3Client_Initialization_ReusesSameClient() {
    String fileContent = "test";
    String fileName1 = "test1.csv";
    String fileName2 = "test2.csv";
    String bucketName = "bucket";

    PutObjectResponse mockResponse = PutObjectResponse.builder().eTag("etag").build();
    when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(mockResponse);

    s3Utility.uploadFile(fileContent, fileName1, bucketName);
    s3Utility.uploadFile(fileContent, fileName2, bucketName);

    verify(mockS3ClientBuilder, times(1)).build();
    verify(mockS3Client, times(2)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }
}
