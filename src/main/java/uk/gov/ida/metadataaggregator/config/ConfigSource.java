package uk.gov.ida.metadataaggregator.config;

import java.io.IOException;

public interface ConfigSource {
    AggregatorConfig downloadConfig() throws IOException;
}
