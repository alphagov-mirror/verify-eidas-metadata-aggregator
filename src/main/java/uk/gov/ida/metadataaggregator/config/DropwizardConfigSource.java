package uk.gov.ida.metadataaggregator.config;

import com.google.inject.Inject;

public class DropwizardConfigSource implements ConfigSource {
    private final AggregatorConfig configuration;

    @Inject
    public DropwizardConfigSource(AggregatorConfig configuration) {
        this.configuration = configuration;
    }

    @Override
    public AggregatorConfig downloadConfig() throws ConfigSourceException {
        return this.configuration;
    }
}