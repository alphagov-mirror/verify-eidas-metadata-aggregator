package uk.gov.ida.metadataaggregator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.codahale.metrics.health.HealthCheck.Result;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

import uk.gov.ida.metadataaggregator.core.StatusReport;
import uk.gov.ida.metadataaggregator.healthcheck.AggregationStatusHealthCheck;

@RunWith(MockitoJUnitRunner.class)
public class AggregationStatusHealthCheckTest {
    @Mock
    private StatusReport statusReport;

    private URL metadataUrl;
    private Map<URL, Throwable> metadataUrls;
    private AtomicReference<StatusReport> reportRef;
    private AggregationStatusHealthCheck healthCheck;

    @Before
    public void setUp() throws MalformedURLException {
        metadataUrl = new URL("https://localhost:80/");
        metadataUrls = new HashMap<>();
        metadataUrls.put(metadataUrl, new RuntimeException("test"));
        reportRef = new AtomicReference<StatusReport>();
        reportRef.set(statusReport);
        healthCheck = new AggregationStatusHealthCheck(reportRef);
    }

    @Test
    public void testHealthCheckStatusIsUnhealthyWhenFailuresReported() {
        when(statusReport.wasSuccessful()).thenReturn(false);
        Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }

    @Test
    public void testHealthCheckStatusIsHealthyWhenSuccessReported() {
        when(statusReport.wasSuccessful()).thenReturn(true);
        Result result = healthCheck.check();
        assertTrue(result.isHealthy());
    }

    @Test
    public void testHealthCheckContainsURLsOfFailingMetadata() {
        when(statusReport.wasSuccessful()).thenReturn(false);
        when(statusReport.getErrors()).thenReturn(metadataUrls);
        Result result = healthCheck.check();
        assertTrue(result.getDetails().containsKey("errors"));
        assertTrue(((Map<URL, Throwable>) result.getDetails().get("errors")).containsKey(metadataUrl));
    }

    @Test
    public void testHealthCheckWithNoReportIsUnhealthy() throws Exception {
        reportRef.set(null);
        Result result = healthCheck.check();
        assertFalse(result.isHealthy());
    }
}
