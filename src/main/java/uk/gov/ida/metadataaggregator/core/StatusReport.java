package uk.gov.ida.metadataaggregator.core;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

public class StatusReport {
  private int numberOfSuccesses = 0;
  private final DateTime runAt = DateTime.now();
  private final int expectedSuccesses;
  private final HashMap<URL, Throwable> errorsFromMetadataUrl = new HashMap<URL, Throwable>();

  public StatusReport(int expectedSuccesses) {
    this.expectedSuccesses = expectedSuccesses;
  }

  public DateTime getRunAt() {
    return runAt;
  }

  public Map<URL, Throwable> getErrors() {
    return errorsFromMetadataUrl;
  }

  public void recordSuccess(URL metadataUrl) {
    numberOfSuccesses++;
  }

  public void recordFailure(URL metadataUrl, Throwable result) {
    errorsFromMetadataUrl.put(metadataUrl, result);
  }

  public boolean wasSuccessful() {
    return numberOfSuccesses == expectedSuccesses;
  }

  public int numberOfSuccesses() {
    return numberOfSuccesses;
  }
}