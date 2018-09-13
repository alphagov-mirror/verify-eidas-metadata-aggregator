package uk.gov.ida.metadataaggregator;

import com.codahale.metrics.health.HealthCheck;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.ida.metadataaggregator.configuration.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.core.S3BucketMetadataStore;
import uk.gov.ida.metadataaggregator.exceptions.MetadataStoreException;
import uk.gov.ida.metadataaggregator.healthcheck.ReconciliationHealthCheck;
import uk.gov.ida.metadataaggregator.util.HexUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReconciliationHealthCheckTest {

    private ReconciliationHealthCheck reconciliationHealthCheck;
    private final S3BucketMetadataStore metadataStore = mock(S3BucketMetadataStore.class);
    private final MetadataSourceConfiguration config = mock(MetadataSourceConfiguration.class);
    private final static String BUCKET_URL_A = HexUtils.encodeString("http://localhost-country-a");
    private final static String BUCKET_URL_C = HexUtils.encodeString("http://localhost-country-c");
    private URL countryAConfigUrl;
    private URL countryBConfigUrl;
    private HashMap<String, URL> configUrls;

    @Before
    public void setUp() throws MalformedURLException {
        configUrls = new HashMap<>();
        countryAConfigUrl = new URL("http://localhost-country-a");
        countryBConfigUrl = new URL("http://localhost-country-b");
        reconciliationHealthCheck = new ReconciliationHealthCheck(metadataStore, config);
    }

    @Test
    public void shouldReturnHealthyWhenMetadataMatches() throws MetadataStoreException {
        configUrls.put("someCountry", countryAConfigUrl);

        when(config.getMetadataUrls()).thenReturn(configUrls);
        when(metadataStore.getAllHexEncodedUrlsFromS3Bucket()).thenReturn(Arrays.asList(BUCKET_URL_A));

        HealthCheck.Result check = reconciliationHealthCheck.check();

        assertTrue(check.isHealthy());
    }

    @Test
    public void shouldReturnHealthyWhenBothConfigAndBucketAreEmpty() throws MetadataStoreException {
        when(config.getMetadataUrls()).thenReturn(configUrls);
        when(metadataStore.getAllHexEncodedUrlsFromS3Bucket()).thenReturn(emptyList());

        HealthCheck.Result check = reconciliationHealthCheck.check();

        assertTrue(check.isHealthy());
    }

    @Test
    public void shouldReturnUnhealthyWhenMetadataIsNotInBucket() throws MetadataStoreException {
        configUrls.put("someCountry", countryAConfigUrl);

        when(config.getMetadataUrls()).thenReturn(configUrls);
        when(metadataStore.getAllHexEncodedUrlsFromS3Bucket()).thenReturn(emptyList());

        HealthCheck.Result check = reconciliationHealthCheck.check();

        assertFalse(check.isHealthy());
    }

    @Test
    public void shouldReturnUnhealthyWhenMetadataIsNotInConfig() throws MetadataStoreException {
        when(config.getMetadataUrls()).thenReturn(configUrls);
        when(metadataStore.getAllHexEncodedUrlsFromS3Bucket()).thenReturn(Arrays.asList(BUCKET_URL_A));

        HealthCheck.Result check = reconciliationHealthCheck.check();

        assertFalse(check.isHealthy());
    }

    @Test
    public void shouldReturnUnhealthyWhenMetadataIsNotInConfigOrNotInBucket() throws MetadataStoreException {
        configUrls.put("countryA", countryAConfigUrl);
        configUrls.put("countryB", countryBConfigUrl);

        when(config.getMetadataUrls()).thenReturn(configUrls);
        when(metadataStore.getAllHexEncodedUrlsFromS3Bucket()).thenReturn(Arrays.asList(BUCKET_URL_A,BUCKET_URL_C));

        HealthCheck.Result check = reconciliationHealthCheck.check();

        assertFalse(check.isHealthy());
    }
}
