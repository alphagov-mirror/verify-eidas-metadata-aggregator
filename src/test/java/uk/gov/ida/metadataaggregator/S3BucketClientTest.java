package uk.gov.ida.metadataaggregator;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.metadataaggregator.config.ConfigSourceException;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStoreException;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.metadataaggregator.LambdaConstants.CONFIG_BUCKET_KEY;

public class S3BucketClientTest {

    private static final String TEST_BUCKET_NAME = "testBucketName";
    private static final String TEST_METADATA_URL = "test.url.com";
    private static final String HEX_ENCODED_METADATA_URL = String.valueOf(Hex.encodeHex(TEST_METADATA_URL.getBytes()));
    private static final String TEST_METADATA = "testMetadataString";

    private AmazonS3Client amazonS3Client;
    private S3BucketClient s3BucketClient;

    @Before
    public void setUp() {
        amazonS3Client = mock(AmazonS3Client.class);
        s3BucketClient = new S3BucketClient(TEST_BUCKET_NAME, amazonS3Client);
    }

    @Test
    public void shouldMapDownloadedConfigIntoObject() throws ConfigSourceException {
        String testJson = JsonAggregatorConfigBuilder.newConfig().withMetadataUrl(TEST_METADATA_URL).toJson();
        S3Object mockS3Object = mockS3ObjectReturning(testJson);
        when(amazonS3Client.getObject(TEST_BUCKET_NAME, CONFIG_BUCKET_KEY)).thenReturn(mockS3Object);

        AggregatorConfig aggregatorConfig = new S3BucketClient(TEST_BUCKET_NAME, amazonS3Client).downloadConfig();

        Collection<String> metadataUrls = aggregatorConfig.getMetadataUrls();
        assertThat(metadataUrls).hasSize(1);
    }

    @Test
    public void shouldPutObjectIntoS3BucketUnderHexEncodedKey() throws MetadataStoreException {
        s3BucketClient.uploadMetadata(HexUtils.encodeString(TEST_METADATA_URL), TEST_METADATA);

        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(amazonS3Client).putObject(putObjectRequestArgumentCaptor.capture());
    }

    @Test
    public void shouldConvertExceptionIntoDomainTypeWhenDownloadMappingFails() {
        when(amazonS3Client.getObject(anyString(), anyString())).thenThrow(new AmazonClientException(""));

        assertThatExceptionOfType(ConfigSourceException.class).isThrownBy(s3BucketClient::downloadConfig);
    }

    @Test
    public void shouldConvertExceptionIntoDomainTypeWhenDownloadFails() {
        String testJson = JsonAggregatorConfigBuilder.newConfig().withMetadataUrl(TEST_METADATA_URL).toInvalidJson();
        S3Object mockS3Object = mockS3ObjectReturning(testJson);
        when(amazonS3Client.getObject(TEST_BUCKET_NAME, CONFIG_BUCKET_KEY)).thenReturn(mockS3Object);

        assertThatExceptionOfType(ConfigSourceException.class).isThrownBy(s3BucketClient::downloadConfig);
    }

    @Test
    public void shouldConvertExceptionIntoDomainTypeWhenUploadFails() {
        when(amazonS3Client.putObject(any())).thenThrow(new RuntimeException());

        assertThatExceptionOfType(MetadataStoreException.class)
                .isThrownBy(() -> s3BucketClient.uploadMetadata(TEST_METADATA_URL, TEST_METADATA));
    }

    @Test
    public void shouldDeleteObjectFromS3Bucket() throws MetadataStoreException {
        s3BucketClient.deleteMetadata(HexUtils.encodeString(TEST_METADATA_URL));

        ArgumentCaptor<DeleteObjectRequest> deleteObjectRequestArgumentCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(amazonS3Client).deleteObject(deleteObjectRequestArgumentCaptor.capture());
    }

    @Test
    public void shouldThrowWhenDeleteObjectFromS3BucketFails() {
        doThrow(new RuntimeException()).when(amazonS3Client).deleteObject(any());

        assertThatExceptionOfType(MetadataStoreException.class)
                .isThrownBy(() -> s3BucketClient.deleteMetadata(HEX_ENCODED_METADATA_URL));
    }

    @Test
    public void shouldRetrieveAllKeysFromS3Bucket() throws MetadataStoreException {
        String kid1 = "http://example1.com";
        String kid2 = "http://example2.com";

        S3ObjectSummary s3Object1 = createS3ObjectSummary(kid1);
        S3ObjectSummary s3Object2 = createS3ObjectSummary(kid2);

        List<S3ObjectSummary> s3ObjectSummaries = new ArrayList<>();
        s3ObjectSummaries.add(s3Object1);
        s3ObjectSummaries.add(s3Object2);

        ObjectListing objectListing = mock(ObjectListing.class);
        when(amazonS3Client.listObjects(TEST_BUCKET_NAME)).thenReturn(objectListing);
        when(objectListing.getObjectSummaries()).thenReturn(s3ObjectSummaries);

        List<String> s3BucketKeys = s3BucketClient.getAllHexEncodedUrlsFromS3Bucket();

        assertThat(s3BucketKeys.get(0).contains(kid1));
        assertThat(s3BucketKeys.get(1).contains(kid2));
    }

    @Test
    public void shouldThrowWhenS3ObjectListCantBeRetrievedFromBucket() {
        doThrow(new RuntimeException()).when(amazonS3Client).listObjects(TEST_BUCKET_NAME);

        assertThatExceptionOfType(MetadataStoreException.class)
                .isThrownBy(() -> s3BucketClient.getAllHexEncodedUrlsFromS3Bucket());
    }

    private S3ObjectSummary createS3ObjectSummary(String key) {
        S3ObjectSummary s3ObjectSummary = new S3ObjectSummary();
        s3ObjectSummary.setBucketName(TEST_BUCKET_NAME);
        s3ObjectSummary.setKey(key);

        return s3ObjectSummary;
    }

    private S3Object mockS3ObjectReturning(String testJson) {
        S3Object mockS3Object = mock(S3Object.class);
        when(mockS3Object.getObjectContent())
                .thenReturn(new S3ObjectInputStream(new ByteArrayInputStream(testJson.getBytes()), null));
        return mockS3Object;
    }
}
