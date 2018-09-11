package uk.gov.ida.metadataaggregator;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.codahale.metrics.health.HealthCheck.Result;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

import uk.gov.ida.metadataaggregator.configuration.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.core.MetadataStore;
import uk.gov.ida.metadataaggregator.exceptions.MetadataStoreException;
import uk.gov.ida.metadataaggregator.healthcheck.ReconciliationHealthCheck;

@RunWith(MockitoJUnitRunner.class)
public class ReconcilliationHealthCheckTest {
    private static String TEST_METADATA_URL = "http://localhost:80/metadata";

    @Mock
    MetadataStore store;

    @Mock
    MetadataSourceConfiguration config;

    @Mock
    EntityDescriptor stubEntityDescriptor;

    ReconciliationHealthCheck healthCheck;

    @Before
    public void setUp() {
        healthCheck = new ReconciliationHealthCheck(store, config);
    }

    @Test
    public void testHealthyWhenNoMetadataIsConfigured() throws Exception {
        when(store.list()).thenReturn(asList());
        when(config.getMetadataUrls()).thenReturn(asMap());

        Result result = healthCheck.check();

        assertTrue(result.isHealthy());
    }

    @Test
    public void testHealthyWhenMetadataMatchesAndCanBeDownloaded() throws Exception {
        when(store.list()).thenReturn(asList(TEST_METADATA_URL));
        when(config.getMetadataUrls()).thenReturn(asMap("test", new URL(TEST_METADATA_URL)));
        when(store.download(TEST_METADATA_URL)).thenReturn(stubEntityDescriptor);

        Result result = healthCheck.check();

        assertTrue(result.isHealthy());
    }

    @Test
    public void testUnhealthyWhenMetadataNotInBucket() throws Exception {
        when(store.list()).thenReturn(asList());
        when(config.getMetadataUrls()).thenReturn(asMap("test", new URL(TEST_METADATA_URL)));

        Result result = healthCheck.check();

        assertFalse(result.isHealthy());
        assertTrue(result.getDetails().containsKey("missingFromBucket"));
        assertTrue(((Set<String>) result.getDetails().get("missingFromBucket")).contains(TEST_METADATA_URL));
    }

    @Test
    public void testUnhealthyWhenMetadataNotInConfig() throws Exception {
        when(store.list()).thenReturn(asList(TEST_METADATA_URL));
        when(config.getMetadataUrls()).thenReturn(asMap());
        when(store.download(TEST_METADATA_URL)).thenReturn(stubEntityDescriptor);

        Result result = healthCheck.check();

        assertFalse(result.isHealthy());
        assertTrue(result.getDetails().containsKey("missingFromConfig"));
        assertTrue(((Set<String>) result.getDetails().get("missingFromConfig")).contains(TEST_METADATA_URL));
    }

    @Test
    public void testUnhealthyWhenMetadataInBucketIsInvalid() throws Exception {
        when(store.list()).thenReturn(asList(TEST_METADATA_URL));
        when(config.getMetadataUrls()).thenReturn(asMap("test", new URL(TEST_METADATA_URL)));
        when(store.download(TEST_METADATA_URL)).thenThrow(new MetadataStoreException("Metadata has expired"));

        Result result = healthCheck.check();

        assertFalse(result.isHealthy());
        assertTrue(result.getDetails().containsKey("invalid"));
        assertTrue(((Set<String>) result.getDetails().get("invalid")).contains(TEST_METADATA_URL));
    }

    private static <T, U> Map<T, U> asMap() {
        return new HashMap<T, U>(0);
    }

    private static <T, U> Map<T, U> asMap(T key, U value) {
        Map<T, U> map = new HashMap<>(1);
        map.put(key, value);
        return map;
    }
}