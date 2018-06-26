package uk.gov.ida.metadataaggregator;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.metadataaggregator.config.ConfigSource;
import uk.gov.ida.metadataaggregator.config.ConfigSourceException;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataSource;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataSourceException;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStore;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStoreException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MetadataAggregatorTest {

    private final ConfigSource testConfigSource = mock(ConfigSource.class);
    private final CountryMetadataSource testMetadataSource = mock(CountryMetadataSource.class);
    private final MetadataStore testMetadataStore = mock(MetadataStore.class);
    private MetadataAggregator testAggregator;

    URL testUrl1, testUrl2, testUrl3, unsuccessfulUrl, successfulUrl;
    EntityDescriptor testMetadata1, testMetadata2, unsuccessfulMetadata, successfulMetadata;

    @Before
    public void before() throws MalformedURLException {
        testAggregator = new MetadataAggregator(testConfigSource, testMetadataSource, testMetadataStore);

        testUrl1 = new URL("http://testUrl1");
        testMetadata1 = mock(EntityDescriptor.class);
        testUrl2 = new URL("http://testUrl2");
        testMetadata2 = mock(EntityDescriptor.class);
        testUrl3 = new URL("http://testUrl3");
        unsuccessfulUrl = new URL("http://www.unsuccessfulUrl.com");
        unsuccessfulMetadata = mock(EntityDescriptor.class);
        successfulUrl= new URL("http://www.successfulUrl.com");
        successfulMetadata = mock(EntityDescriptor.class);
    }

    @Test
    public void shouldUploadMetadataDownloadedFromSourceToStore()
            throws MetadataSourceException, ConfigSourceException, MetadataStoreException {


        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(testUrl1), null));
        when(testMetadataSource.downloadMetadata(testUrl1)).thenReturn(testMetadata1);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl1.toString()), testMetadata1);
    }

    @Test
    public void shouldUploadMultipleMetadataDownloadedFromSourceToStore()
            throws MetadataSourceException, ConfigSourceException, MetadataStoreException {

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(testUrl1, testUrl2), null));
        when(testMetadataSource.downloadMetadata(testUrl1)).thenReturn(testMetadata1);
        when(testMetadataSource.downloadMetadata(testUrl2)).thenReturn(testMetadata2);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl1.toString()), testMetadata1);
        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl2.toString()), testMetadata2);
    }

    @Test
    public void shouldNotUploadAnyMetadataWhenExceptionThrowByConfigSource()
            throws ConfigSourceException, MetadataStoreException, MetadataSourceException {

        when(testConfigSource.downloadConfig()).thenThrow(new ConfigSourceException("Unable to obtain config"));

        testAggregator.aggregateMetadata();

        verify(testMetadataSource, never()).downloadMetadata(any(URL.class));
        verify(testMetadataStore, never()).uploadMetadata(anyString(), any(EntityDescriptor.class));
    }

    @Test
    public void shouldNotUploadMetadataWhenExceptionThrowByMetadataSource()
            throws ConfigSourceException, MetadataStoreException, MetadataSourceException {

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(unsuccessfulUrl), null));
        when(testMetadataSource.downloadMetadata(unsuccessfulUrl)).thenThrow(new MetadataSourceException("Metadata source exception"));

        testAggregator.aggregateMetadata();

        verify(testMetadataStore, never()).uploadMetadata(anyString(), any(EntityDescriptor.class));
    }

    @Test
    public void shouldUploadValidMetadataWhenExceptionThrowByMetadataSource()
            throws ConfigSourceException, MetadataStoreException, MetadataSourceException {

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(unsuccessfulUrl, successfulUrl), null));
        when(testMetadataSource.downloadMetadata(unsuccessfulUrl)).thenThrow(new MetadataSourceException("Metadata source exception"));
        when(testMetadataSource.downloadMetadata(successfulUrl)).thenReturn(successfulMetadata);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(successfulUrl.toString()), successfulMetadata);
    }

    @Test
    public void shouldUploadValidMetadataWhenPreviousUploadFailed()
            throws ConfigSourceException, MetadataStoreException, MetadataSourceException {

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(unsuccessfulUrl, successfulUrl), null));
        when(testMetadataSource.downloadMetadata(unsuccessfulUrl)).thenReturn(unsuccessfulMetadata);
        when(testMetadataSource.downloadMetadata(successfulUrl)).thenReturn(successfulMetadata);

        doThrow(new MetadataStoreException("Metadata store failed"))
                .when(testMetadataStore).uploadMetadata(HexUtils.encodeString(unsuccessfulUrl.toString()), unsuccessfulMetadata);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(successfulUrl.toString()), successfulMetadata);
    }

    @Test
    public void shouldDeleteMetadataWhenDownloadIsUnsuccessful()
            throws ConfigSourceException, MetadataSourceException, MetadataStoreException {

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(unsuccessfulUrl),null));
        doThrow(new MetadataSourceException("Metadata source failed"))
                .when(testMetadataSource).downloadMetadata(unsuccessfulUrl);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).deleteMetadata(HexUtils.encodeString(unsuccessfulUrl.toString()));
    }

    @Test
    public void shouldDeleteMetadataWhenUploadIsUnsuccessful()
            throws ConfigSourceException, MetadataSourceException, MetadataStoreException {

        EntityDescriptor unsuccessfulMetadata = mock(EntityDescriptor.class);

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(unsuccessfulUrl), null));
        when(testMetadataSource.downloadMetadata(unsuccessfulUrl)).thenReturn(unsuccessfulMetadata);

        doThrow(new MetadataStoreException("Metadata store failed"))
                .when(testMetadataStore).uploadMetadata(HexUtils.encodeString(unsuccessfulUrl.toString()),unsuccessfulMetadata);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).deleteMetadata(HexUtils.encodeString(unsuccessfulUrl.toString()));
    }

    @Test
    public void shouldThrowExceptionWhenDeleteMetadataFails()
            throws MetadataSourceException, ConfigSourceException, MetadataStoreException {

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(unsuccessfulUrl), null));

        doThrow(new MetadataSourceException("Download metadata has failed"))
                .when(testMetadataSource).downloadMetadata(unsuccessfulUrl);
        doThrow(new MetadataStoreException("Delete metadata has failed"))
                .when(testMetadataStore).deleteMetadata(HexUtils.encodeString(unsuccessfulUrl.toString()));

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).deleteMetadata(HexUtils.encodeString(unsuccessfulUrl.toString()));
    }

    @Test
    public void shouldUploadValidMetadataWhenDeleteOfPreviousMetadataFails()
            throws MetadataStoreException, MetadataSourceException, ConfigSourceException {

        EntityDescriptor successfulMetadata = mock(EntityDescriptor.class);

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(unsuccessfulUrl, successfulUrl), null));
        when(testMetadataSource.downloadMetadata(successfulUrl)).thenReturn(successfulMetadata);

        doThrow(new MetadataSourceException("Download metadata has failed"))
                .when(testMetadataSource).downloadMetadata(unsuccessfulUrl);
        doThrow(new MetadataStoreException("Delete metadata has failed"))
                .when(testMetadataStore).deleteMetadata(HexUtils.encodeString(unsuccessfulUrl.toString()));

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(successfulUrl.toString()), successfulMetadata);
    }

    @Test
    public void shouldDeleteMetadataFromS3BucketWhenNotInConfig()
            throws ConfigSourceException, MetadataSourceException, MetadataStoreException {

        List<String> s3BucketUrls = new ArrayList<>();
        s3BucketUrls.add(HexUtils.encodeString(testUrl1.toString()));
        s3BucketUrls.add(HexUtils.encodeString(testUrl2.toString()));
        s3BucketUrls.add(HexUtils.encodeString(testUrl3.toString()));

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(testUrl1, testUrl2), null));
        when(testMetadataSource.downloadMetadata(testUrl1)).thenReturn(testMetadata1);
        when(testMetadataSource.downloadMetadata(testUrl2)).thenReturn(testMetadata2);
        when(testMetadataStore.getAllHexEncodedUrlsFromS3Bucket()).thenReturn(s3BucketUrls);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).deleteMetadata(HexUtils.encodeString(testUrl3.toString()));
        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl1.toString()), testMetadata1);
        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl2.toString()), testMetadata2);
    }

    @Test
    public void shouldUploadMetadataWhenRetrievingKeysFromS3BucketFails() throws ConfigSourceException, MetadataSourceException, MetadataStoreException {

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(testUrl1, testUrl2), null));
        doThrow(new MetadataStoreException("Unable to retrieve keys from S3 Bucket"))
                .when(testMetadataStore).getAllHexEncodedUrlsFromS3Bucket();
        when(testMetadataSource.downloadMetadata(testUrl1)).thenReturn(testMetadata1);
        when(testMetadataSource.downloadMetadata(testUrl2)).thenReturn(testMetadata2);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl1.toString()), testMetadata1);
        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl2.toString()), testMetadata2);
    }
}
