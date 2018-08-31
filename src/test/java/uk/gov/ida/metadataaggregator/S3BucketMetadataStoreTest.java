package uk.gov.ida.metadataaggregator;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.commons.codec.binary.Hex;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import uk.gov.ida.metadataaggregator.exceptions.MetadataStoreException;
import uk.gov.ida.metadataaggregator.util.HexUtils;
import uk.gov.ida.saml.core.test.TestEntityIds;
import uk.gov.ida.saml.metadata.test.factories.metadata.EntityDescriptorFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class S3BucketMetadataStoreTest {

    private static final String TEST_BUCKET_NAME = "testBucketName";
    private static final String STUB_COUNTRY_ENTITY_ID = TestEntityIds.STUB_COUNTRY_ONE;
    private static final String HEX_ENCODED_METADATA_URL = String.valueOf(Hex.encodeHex(STUB_COUNTRY_ENTITY_ID.getBytes()));
    private static final EntityDescriptor STUB_COUNTRY_METADATA = new EntityDescriptorFactory().idpEntityDescriptor(STUB_COUNTRY_ENTITY_ID);


    private AmazonS3 amazonS3Client;
    private S3BucketMetadataStore s3BucketMetadataStore;

    @Before
    public void setUp() throws InitializationException {
        InitializationService.initialize();
        amazonS3Client = mock(AmazonS3.class);
        s3BucketMetadataStore = new S3BucketMetadataStore(TEST_BUCKET_NAME, amazonS3Client);
    }

    @Test
    public void shouldPutObjectIntoS3BucketUnderHexEncodedKey() throws MetadataStoreException {
        s3BucketMetadataStore.uploadMetadata(HexUtils.encodeString(STUB_COUNTRY_ENTITY_ID), STUB_COUNTRY_METADATA);

        ArgumentCaptor<PutObjectRequest> putObjectRequestArgumentCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(amazonS3Client).putObject(putObjectRequestArgumentCaptor.capture());
    }

    @Test
    public void shouldConvertExceptionIntoDomainTypeWhenUploadFails() {
        when(amazonS3Client.putObject(any())).thenThrow(new RuntimeException());

        assertThatExceptionOfType(MetadataStoreException.class)
                .isThrownBy(() -> s3BucketMetadataStore.uploadMetadata(STUB_COUNTRY_ENTITY_ID, STUB_COUNTRY_METADATA));
    }

    @Test
    public void shouldDeleteObjectFromS3Bucket() throws MetadataStoreException {
        s3BucketMetadataStore.deleteMetadata(HexUtils.encodeString(STUB_COUNTRY_ENTITY_ID));

        ArgumentCaptor<DeleteObjectRequest> deleteObjectRequestArgumentCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(amazonS3Client).deleteObject(deleteObjectRequestArgumentCaptor.capture());
    }

    @Test
    public void shouldThrowWhenDeleteObjectFromS3BucketFails() {
        doThrow(new RuntimeException()).when(amazonS3Client).deleteObject(any());

        assertThatExceptionOfType(MetadataStoreException.class)
                .isThrownBy(() -> s3BucketMetadataStore.deleteMetadata(HEX_ENCODED_METADATA_URL));
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

        List<String> s3BucketKeys = s3BucketMetadataStore.getAllHexEncodedUrlsFromS3Bucket();

        assertThat(s3BucketKeys.get(0).contains(kid1));
        assertThat(s3BucketKeys.get(1).contains(kid2));
    }

    @Test
    public void shouldThrowWhenS3ObjectListCantBeRetrievedFromBucket() {
        doThrow(new RuntimeException()).when(amazonS3Client).listObjects(TEST_BUCKET_NAME);

        assertThatExceptionOfType(MetadataStoreException.class)
                .isThrownBy(() -> s3BucketMetadataStore.getAllHexEncodedUrlsFromS3Bucket());
    }

    private S3ObjectSummary createS3ObjectSummary(String key) {
        S3ObjectSummary s3ObjectSummary = new S3ObjectSummary();
        s3ObjectSummary.setBucketName(TEST_BUCKET_NAME);
        s3ObjectSummary.setKey(key);

        return s3ObjectSummary;
    }
}
