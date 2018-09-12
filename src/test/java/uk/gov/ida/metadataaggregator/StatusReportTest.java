package uk.gov.ida.metadataaggregator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import uk.gov.ida.metadataaggregator.core.StatusReport;

public class StatusReportTest {
  private URL metadataUrl;
  private HashMap<String, URL> metadataUrls;

  @Before
  public void setUp() throws MalformedURLException {
    metadataUrl = new URL("http://localhost:80");
    metadataUrls = new HashMap<>();
  }

  @Test
  public void testOnlySuccessfulWhenAllMetadataProcessed() {
    StatusReport report = new StatusReport(1);

    assertFalse(report.wasSuccessful());
    report.recordSuccess();
    assertTrue(report.wasSuccessful());
  }

  @Test
  public void testUnsuccessfulWhenMetadataFails() {
    StatusReport report = new StatusReport(1);

    report.recordFailure(metadataUrl, new Exception());
    assertFalse(report.wasSuccessful());
  }
}
