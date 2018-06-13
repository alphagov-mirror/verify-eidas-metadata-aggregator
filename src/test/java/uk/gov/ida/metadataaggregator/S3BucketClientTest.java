package uk.gov.ida.metadataaggregator;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.apache.commons.codec.binary.Hex;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.metadataaggregator.config.ConfigSourceException;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStoreException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.ida.metadataaggregator.LambdaConstants.CONFIG_BUCKET_KEY;

public class S3BucketClientTest {

    private static final String TEST_BUCKET_NAME = "testBucketName";
    private static final String TEST_METADATA_URL = "test.url.com";
    private static final String HEX_ENCODED_METADATA_URL = String.valueOf(Hex.encodeHex(TEST_METADATA_URL.getBytes()));
    private static final String TEST_METADATA = "testMetadataString";

    @Test
    public void shouldMapDownloadedConfigIntoObject() throws ConfigSourceException {

        AmazonS3Client testClient = mock(AmazonS3Client.class);
        String testJson = JsonAggregatorConfigBuilder.newConfig().withMetadataUrl(TEST_METADATA_URL).toJson();
        S3Object mockS3Object = mockS3ObjectReturning(testJson);
        when(testClient.getObject(TEST_BUCKET_NAME, CONFIG_BUCKET_KEY)).thenReturn(mockS3Object);

        AggregatorConfig aggregatorConfig = new S3BucketClient(TEST_BUCKET_NAME, testClient).downloadConfig();

        Collection<String> metadataUrls = aggregatorConfig.getMetadataUrls();
        assertThat(metadataUrls).hasSize(1);
    }

    @Test
    public void shouldPutObjectIntoS3BucketUnderHexEncodedKey() throws MetadataStoreException {
        AmazonS3Client amazonS3Client = mock(AmazonS3Client.class);

        S3BucketClient s3BucketClient = new S3BucketClient(TEST_BUCKET_NAME, amazonS3Client);
        s3BucketClient.uploadMetadata(TEST_METADATA_URL, TEST_METADATA);

        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(amazonS3Client).putObject(putObjectRequestArgumentCaptor.capture());

        PutObjectRequest value = putObjectRequestArgumentCaptor.getValue();
        assertThat(value.getKey()).isEqualTo(HEX_ENCODED_METADATA_URL);

        String uploadedMetadata = new BufferedReader(new InputStreamReader(value.getInputStream()))
                .lines().collect(Collectors.joining("\n"));

        assertThat(uploadedMetadata).isEqualTo(TEST_METADATA);
    }

    @Test
    public void shouldConvertExceptionIntoDomainTypeWhenDownloadMappingFails(){
        AmazonS3Client amazonS3Client = mock(AmazonS3Client.class);
        S3BucketClient s3BucketClient = new S3BucketClient(TEST_BUCKET_NAME, amazonS3Client);
        when(amazonS3Client.getObject(anyString(), anyString())).thenThrow(new AmazonClientException(""));

        assertThatExceptionOfType(ConfigSourceException.class).isThrownBy(s3BucketClient::downloadConfig);
    }

    @Test
    public void shouldConvertExceptionIntoDomainTypeWhenDownloadFails(){
        AmazonS3Client amazonS3Client = mock(AmazonS3Client.class);
        S3BucketClient s3BucketClient = new S3BucketClient(TEST_BUCKET_NAME, amazonS3Client);
        String testJson = JsonAggregatorConfigBuilder.newConfig().withMetadataUrl(TEST_METADATA_URL).toInvalidJson();
        S3Object mockS3Object = mockS3ObjectReturning(testJson);
        when(amazonS3Client.getObject(TEST_BUCKET_NAME, CONFIG_BUCKET_KEY)).thenReturn(mockS3Object);

        assertThatExceptionOfType(ConfigSourceException.class).isThrownBy(s3BucketClient::downloadConfig);
    }

    @Test
    public void shouldConvertExceptionIntoDomainTypeWhenUploadFails(){
        AmazonS3Client amazonS3Client = mock(AmazonS3Client.class);
        S3BucketClient s3BucketClient = new S3BucketClient(TEST_BUCKET_NAME, amazonS3Client);

        when(amazonS3Client.putObject(any())).thenThrow(new RuntimeException());

        assertThatExceptionOfType(MetadataStoreException.class)
                .isThrownBy(() -> s3BucketClient.uploadMetadata(TEST_METADATA_URL, TEST_METADATA));
    }

    private S3Object mockS3ObjectReturning(String testJson) {
        S3Object mockS3Object = mock(S3Object.class);
        when(mockS3Object.getObjectContent())
                .thenReturn(new S3ObjectInputStream(new ByteArrayInputStream(testJson.getBytes()), null));
        return mockS3Object;
    }
}
