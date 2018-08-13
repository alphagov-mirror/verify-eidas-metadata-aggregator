package uk.gov.ida.metadataaggregator;

import org.junit.Before;
import org.junit.Test;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import uk.gov.ida.metadataaggregator.config.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.config.ConfigSource;
import uk.gov.ida.metadataaggregator.config.ConfigSourceException;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataSource;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataSourceException;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStore;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStoreException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
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
    private HashMap<String, URL> urlList;

    private URL testUrl1, testUrl2, testUrl3, unsuccessfulUrl, successfulUrl;
    private String testKey1, testKey2, unsuccessfulKey, successfulKey;
    private EntityDescriptor testMetadata1, testMetadata2, unsuccessfulMetadata, successfulMetadata;

    @Before
    public void before() throws MalformedURLException {
        testAggregator = new MetadataAggregator(testConfigSource, testMetadataSource, testMetadataStore);
        urlList = new HashMap<>();
        testUrl1 = new URL("http://testUrl1");
        testKey1 = "testKey1";
        testMetadata1 = mock(EntityDescriptor.class);
        testUrl2 = new URL("http://testUrl2");
        testKey2 = "testKey2";
        testMetadata2 = mock(EntityDescriptor.class);
        testUrl3 = new URL("http://testUrl3");
        unsuccessfulUrl = new URL("http://www.unsuccessfulUrl.com");
        unsuccessfulKey = "unsuccessfulKey";
        unsuccessfulMetadata = mock(EntityDescriptor.class);
        successfulUrl = new URL("http://www.successfulUrl.com");
        successfulKey = "successfulKey";
        successfulMetadata = mock(EntityDescriptor.class);
    }

    @Test
    public void shouldUploadMetadataDownloadedFromSourceToStore()
            throws MetadataSourceException, ConfigSourceException, MetadataStoreException {

        urlList.put(testKey1, testUrl1);

        when(testConfigSource.downloadConfig())
                .thenReturn(new MetadataSourceConfiguration(urlList));
        when(testMetadataSource.downloadMetadata(testUrl1)).thenReturn(testMetadata1);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl1.toString()), testMetadata1);
    }

    @Test
    public void shouldUploadMultipleMetadataDownloadedFromSourceToStore()
            throws MetadataSourceException, ConfigSourceException, MetadataStoreException {

        urlList.put(testKey1, testUrl1);
        urlList.put(testKey2, testUrl2);

        when(testConfigSource.downloadConfig())
                .thenReturn(new MetadataSourceConfiguration(urlList));
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

        urlList.put(unsuccessfulKey, unsuccessfulUrl);

        when(testConfigSource.downloadConfig())
                .thenReturn(new MetadataSourceConfiguration(urlList));
        when(testMetadataSource.downloadMetadata(unsuccessfulUrl)).thenThrow(new MetadataSourceException("Metadata source exception"));

        testAggregator.aggregateMetadata();

        verify(testMetadataStore, never()).uploadMetadata(anyString(), any(EntityDescriptor.class));
    }

    @Test
    public void shouldUploadValidMetadataWhenExceptionThrowByMetadataSource()
            throws ConfigSourceException, MetadataStoreException, MetadataSourceException {

        urlList.put(unsuccessfulKey, unsuccessfulUrl);
        urlList.put(successfulKey, successfulUrl);

        when(testConfigSource.downloadConfig())
                .thenReturn(new MetadataSourceConfiguration(urlList));
        when(testMetadataSource.downloadMetadata(unsuccessfulUrl)).thenThrow(new MetadataSourceException("Metadata source exception"));
        when(testMetadataSource.downloadMetadata(successfulUrl)).thenReturn(successfulMetadata);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(successfulUrl.toString()), successfulMetadata);
    }

    @Test
    public void shouldUploadValidMetadataWhenPreviousUploadFailed()
            throws ConfigSourceException, MetadataStoreException, MetadataSourceException {

        urlList.put(unsuccessfulKey, unsuccessfulUrl);
        urlList.put(successfulKey, successfulUrl);

        when(testConfigSource.downloadConfig())
                .thenReturn(new MetadataSourceConfiguration(urlList));
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

        urlList.put(unsuccessfulKey, unsuccessfulUrl);

        when(testConfigSource.downloadConfig())
                .thenReturn(new MetadataSourceConfiguration(urlList));
        doThrow(new MetadataSourceException("Metadata source failed"))
                .when(testMetadataSource).downloadMetadata(unsuccessfulUrl);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).deleteMetadata(HexUtils.encodeString(unsuccessfulUrl.toString()));
    }

    @Test
    public void shouldDeleteMetadataWhenUploadIsUnsuccessful()
            throws ConfigSourceException, MetadataSourceException, MetadataStoreException {

        urlList.put(unsuccessfulKey, unsuccessfulUrl);

        EntityDescriptor unsuccessfulMetadata = mock(EntityDescriptor.class);

        when(testConfigSource.downloadConfig())
                .thenReturn(new MetadataSourceConfiguration(urlList));
        when(testMetadataSource.downloadMetadata(unsuccessfulUrl)).thenReturn(unsuccessfulMetadata);

        doThrow(new MetadataStoreException("Metadata store failed"))
                .when(testMetadataStore).uploadMetadata(HexUtils.encodeString(unsuccessfulUrl.toString()), unsuccessfulMetadata);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).deleteMetadata(HexUtils.encodeString(unsuccessfulUrl.toString()));
    }

    @Test
    public void shouldThrowExceptionWhenDeleteMetadataFails()
            throws MetadataSourceException, ConfigSourceException, MetadataStoreException {

        urlList.put(unsuccessfulKey, unsuccessfulUrl);

        when(testConfigSource.downloadConfig())
                .thenReturn(new MetadataSourceConfiguration(urlList));

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

        urlList.put(unsuccessfulKey, unsuccessfulUrl);
        urlList.put(successfulKey, successfulUrl);

        EntityDescriptor successfulMetadata = mock(EntityDescriptor.class);

        when(testConfigSource.downloadConfig())
                .thenReturn(new MetadataSourceConfiguration(urlList));
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

        urlList.put(testKey1, testUrl1);
        urlList.put(testKey2, testUrl2);

        List<String> s3BucketUrls = new ArrayList<>();
        s3BucketUrls.add(HexUtils.encodeString(testUrl1.toString()));
        s3BucketUrls.add(HexUtils.encodeString(testUrl2.toString()));
        s3BucketUrls.add(HexUtils.encodeString(testUrl3.toString()));

        when(testConfigSource.downloadConfig())
                .thenReturn(new MetadataSourceConfiguration(urlList));
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

        urlList.put(testKey1, testUrl1);
        urlList.put(testKey2, testUrl2);

        when(testConfigSource.downloadConfig())
                .thenReturn(new MetadataSourceConfiguration(urlList));
        doThrow(new MetadataStoreException("Unable to retrieve keys from S3 Bucket"))
                .when(testMetadataStore).getAllHexEncodedUrlsFromS3Bucket();
        when(testMetadataSource.downloadMetadata(testUrl1)).thenReturn(testMetadata1);
        when(testMetadataSource.downloadMetadata(testUrl2)).thenReturn(testMetadata2);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl1.toString()), testMetadata1);
        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl2.toString()), testMetadata2);
    }
}
