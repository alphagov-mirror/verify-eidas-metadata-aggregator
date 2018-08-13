package uk.gov.ida.metadataaggregator.config;

public interface ConfigSource {
    MetadataSourceConfiguration downloadConfig() throws ConfigSourceException;
}
