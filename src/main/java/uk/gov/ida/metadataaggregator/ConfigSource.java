package uk.gov.ida.metadataaggregator;

import java.io.IOException;

public interface ConfigSource {
    AggregatorConfig downloadConfig() throws IOException;
}
