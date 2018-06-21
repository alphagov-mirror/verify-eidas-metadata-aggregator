package uk.gov.ida.metadataaggregator;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.metadataaggregator.config.ConfigSource;
import uk.gov.ida.metadataaggregator.config.ConfigSourceException;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataSource;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataSourceException;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStore;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStoreException;

import java.util.ArrayList;
import java.util.List;

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

    @Before
    public void before() {
        testAggregator = new MetadataAggregator(testConfigSource, testMetadataSource, testMetadataStore);
    }

    @Test
    public void shouldUploadMetadataDownloadedFromSourceToStore()
            throws MetadataSourceException, ConfigSourceException, MetadataStoreException {

        String testUrl1 = "testUrl1";
        String testMetadata1 = "testMetadata1";

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(testUrl1), null));
        when(testMetadataSource.downloadMetadata(testUrl1)).thenReturn(testMetadata1);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl1), testMetadata1);
    }

    @Test
    public void shouldUploadMultipleMetadataDownloadedFromSourceToStore()
            throws MetadataSourceException, ConfigSourceException, MetadataStoreException {

        String testUrl1 = "testUrl1";
        String testMetadata1 = "testMetadata1";
        String testUrl2= "testUrl2";
        String testMetadata2 = "testMetadata2";

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(testUrl1, testUrl2), null));
        when(testMetadataSource.downloadMetadata(testUrl1)).thenReturn(testMetadata1);
        when(testMetadataSource.downloadMetadata(testUrl2)).thenReturn(testMetadata2);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl1), testMetadata1);
        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl2), testMetadata2);
    }

    @Test
    public void shouldNotUploadAnyMetadataWhenExceptionThrowByConfigSource()
            throws ConfigSourceException, MetadataStoreException, MetadataSourceException {

        when(testConfigSource.downloadConfig()).thenThrow(new ConfigSourceException("Unable to obtain config"));

        testAggregator.aggregateMetadata();

        verify(testMetadataSource, never()).downloadMetadata(anyString());
        verify(testMetadataStore, never()).uploadMetadata(anyString(), anyString());
    }

    @Test
    public void shouldNotUploadMetadataWhenExceptionThrowByMetadataSource()
            throws ConfigSourceException, MetadataStoreException, MetadataSourceException {

        String invalidMetadata = "invalidMetadata";

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(invalidMetadata), null));
        when(testMetadataSource.downloadMetadata(invalidMetadata)).thenThrow(new MetadataSourceException("Metadata source exception"));

        testAggregator.aggregateMetadata();

        verify(testMetadataStore, never()).uploadMetadata(anyString(), anyString());
    }

    @Test
    public void shouldUploadValidMetadataWhenExceptionThrowByMetadataSource()
            throws ConfigSourceException, MetadataStoreException, MetadataSourceException {

        String invalidUrl = "http://www.invalidUrl.com";
        String validUrl= "http://www.validUrl.com";
        String validMetadata = "validMetadata";

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(invalidUrl, validUrl), null));
        when(testMetadataSource.downloadMetadata(invalidUrl)).thenThrow(new MetadataSourceException("Metadata source exception"));
        when(testMetadataSource.downloadMetadata(validUrl)).thenReturn(validMetadata);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(validUrl), validMetadata);
    }

    @Test
    public void shouldUploadValidMetadataWhenPreviousUploadFailed()
            throws ConfigSourceException, MetadataStoreException, MetadataSourceException {

        String unsuccessfulUrl = "http://www.unsuccessfulUrl.com";
        String unsuccessfulMetadata = "unsuccessfulMetadata";
        String successfulUrl= "http://www.successfulUrl.com";
        String successfulMetadata = "successfulMetadata";

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(unsuccessfulUrl, successfulUrl), null));
        when(testMetadataSource.downloadMetadata(unsuccessfulUrl)).thenReturn(unsuccessfulMetadata);
        when(testMetadataSource.downloadMetadata(successfulUrl)).thenReturn(successfulMetadata);

        doThrow(new MetadataStoreException("Metadata store failed"))
                .when(testMetadataStore).uploadMetadata(HexUtils.encodeString(unsuccessfulUrl), unsuccessfulMetadata);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(successfulUrl), successfulMetadata);
    }

    @Test
    public void shouldDeleteMetadataWhenDownloadIsUnsuccessful()
            throws ConfigSourceException, MetadataSourceException, MetadataStoreException {

        String unsuccessfulUrl = "http://www.unsuccessfulUrl.com";

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(unsuccessfulUrl),null));
        doThrow(new MetadataSourceException("Metadata source failed"))
                .when(testMetadataSource).downloadMetadata(unsuccessfulUrl);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).deleteMetadata(HexUtils.encodeString(unsuccessfulUrl));
    }

    @Test
    public void shouldDeleteMetadataWhenUploadIsUnsuccessful()
            throws ConfigSourceException, MetadataSourceException, MetadataStoreException {

        String unsuccessfulUrl = "http://www.unsuccessfulUrl.com";
        String unsuccessfulMetadata = "unsuccessfulMetadata";

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(unsuccessfulUrl), null));
        when(testMetadataSource.downloadMetadata(unsuccessfulUrl)).thenReturn(unsuccessfulMetadata);

        doThrow(new MetadataStoreException("Metadata store failed"))
                .when(testMetadataStore).uploadMetadata(HexUtils.encodeString(unsuccessfulUrl),unsuccessfulMetadata);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).deleteMetadata(HexUtils.encodeString(unsuccessfulUrl));
    }

    @Test
    public void shouldThrowExceptionWhenDeleteMetadataFails()
            throws MetadataSourceException, ConfigSourceException, MetadataStoreException {

        String unsuccessfulUrl = "http://www.unsuccessfulUrl.com";

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(unsuccessfulUrl), null));

        doThrow(new MetadataSourceException("Download metadata has failed"))
                .when(testMetadataSource).downloadMetadata(unsuccessfulUrl);
        doThrow(new MetadataStoreException("Delete metadata has failed"))
                .when(testMetadataStore).deleteMetadata(HexUtils.encodeString(unsuccessfulUrl));

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).deleteMetadata(HexUtils.encodeString(unsuccessfulUrl));
    }

    @Test
    public void shouldUploadValidMetadataWhenDeleteOfPreviousMetadataFails()
            throws MetadataStoreException, MetadataSourceException, ConfigSourceException {

        String unsuccessfulUrl = "http://www.unsuccessfulUrl.com";
        String successfulUrl = "http://www.successfulUrl.com";
        String successfulMetadata = "successfulMetadata";

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(unsuccessfulUrl, successfulUrl), null));
        when(testMetadataSource.downloadMetadata(successfulUrl)).thenReturn(successfulMetadata);

        doThrow(new MetadataSourceException("Download metadata has failed"))
                .when(testMetadataSource).downloadMetadata(unsuccessfulUrl);
        doThrow(new MetadataStoreException("Delete metadata has failed"))
                .when(testMetadataStore).deleteMetadata(HexUtils.encodeString(unsuccessfulUrl));

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(successfulUrl), successfulMetadata);
    }

    @Test
    public void shouldDeleteMetadataFromS3BucketWhenNotInConfig()
            throws ConfigSourceException, MetadataSourceException, MetadataStoreException {

        String testUrl1 = "testUrl1";
        String testMetadata1 = "testMetadata1";
        String testUrl2 = "testUrl2";
        String testMetadata2 = "testMetadata2";
        String testUrl3 = "testUrl3";

        List<String> s3BucketUrls = new ArrayList<>();
        s3BucketUrls.add(HexUtils.encodeString(testUrl1));
        s3BucketUrls.add(HexUtils.encodeString(testUrl2));
        s3BucketUrls.add(HexUtils.encodeString(testUrl3));

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(testUrl1, testUrl2), null));
        when(testMetadataSource.downloadMetadata(testUrl1)).thenReturn(testMetadata1);
        when(testMetadataSource.downloadMetadata(testUrl2)).thenReturn(testMetadata2);
        when(testMetadataStore.getAllHexEncodedUrlsFromS3Bucket()).thenReturn(s3BucketUrls);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).deleteMetadata(HexUtils.encodeString(testUrl3));
        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl1), testMetadata1);
        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl2), testMetadata2);
    }

    @Test
    public void shouldUploadMetadataWhenRetrievingKeysFromS3BucketFails() throws ConfigSourceException, MetadataSourceException, MetadataStoreException {
        String testUrl1 = "testUrl1";
        String testMetadata1 = "testMetadata1";
        String testUrl2 = "testUrl2";
        String testMetadata2 = "testMetadata2";

        when(testConfigSource.downloadConfig())
                .thenReturn(new AggregatorConfig(ImmutableSet.of(testUrl1, testUrl2), null));
        doThrow(new MetadataStoreException("Unable to retrieve keys from S3 Bucket"))
                .when(testMetadataStore).getAllHexEncodedUrlsFromS3Bucket();
        when(testMetadataSource.downloadMetadata(testUrl1)).thenReturn(testMetadata1);
        when(testMetadataSource.downloadMetadata(testUrl2)).thenReturn(testMetadata2);

        testAggregator.aggregateMetadata();

        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl1), testMetadata1);
        verify(testMetadataStore).uploadMetadata(HexUtils.encodeString(testUrl2), testMetadata2);
    }
}
