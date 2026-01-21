package com.nordstrom.finance.dataintegration.ertm.util;// package com.nordstrom.finance.dataintegration.ertm.util;
//
// import java.io.FileNotFoundException;
// import java.util.Collections;
// import java.util.List;
// import java.util.stream.Stream;
//
// import com.opencsv.CSVReader;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.MockedStatic;
// import org.mockito.Mockito;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
// import org.springframework.test.context.ActiveProfiles;
// import software.amazon.awssdk.core.pagination.sync.SdkIterable;
// import software.amazon.awssdk.regions.Region;
// import software.amazon.awssdk.services.s3.S3Client;
// import software.amazon.awssdk.services.s3.S3ClientBuilder;
// import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
// import software.amazon.awssdk.services.s3.model.S3Object;
// import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
//
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.eq;
// import static org.mockito.Mockito.times;
// import static org.mockito.Mockito.when;
//
// @SpringBootTest(classes = {S3Utility.class})
// @ActiveProfiles("test")
// @ExtendWith(MockitoExtension.class)
// public class S3UtilityTest {
//
//    @Mock
//    private CSVReader csvReader;
//
//    @Mock
//    private S3Client s3c;
//    private Region region = Region.US_WEST_2;
//    private static final String INPUT_BUCKET = "app09831-ertm-transaction-source-nonprod";
//    private static final String INPUT_KEY = "aws-key";
//    @Mock
//    private ListObjectsV2Iterable mocklistObject;
//    @Mock
//    private S3ClientBuilder mockS3ClientBuilder;
//    @Mock
//    private SdkIterable<S3Object> contents;
//    @Mock
//    private Stream<S3Object> mockS3Objects;
//
//    @InjectMocks
//    private S3Utility s3Util;
//
//    @BeforeEach
//    void init() {
//        mockS3ClientBuilder = S3Client.builder().region(Region.US_WEST_2);
//    }
//
//    @Test
//    void testListFilesFromS3_success() {
//
//
//        try (MockedStatic<S3Client> mockedS3Client = Mockito.mockStatic(S3Client.class)) {
////            when(S3Client.builder()).thenReturn(mockS3ClientBuilder);
////            when(mockS3ClientBuilder.build()).thenReturn(s3c);
////
// when(s3c.listObjectsV2Paginator(any(ListObjectsV2Request.class))).thenReturn(mocklistObject);
////            when(mocklistObject.contents()).thenReturn(contents);
////            when(contents.stream()).thenReturn(mockS3Objects);
////            when(mockS3Objects.toList()).thenReturn(Collections.emptyList());
//            s3Util.listFilesFromS3(INPUT_BUCKET);
//        }
//
//
//
// }
