package uk.gov.ida.metadataaggregator.config;

public interface ConfigSource {
    AggregatorConfig downloadConfig() throws ConfigSourceException;
}
