package uk.gov.ida.metadataaggregator.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import uk.gov.ida.metadataaggregator.exceptions.ConfigSourceException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static uk.gov.ida.metadataaggregator.util.Constants.AGGREGATOR_CONFIG_FILE_NAME;

public class MetadataSourceConfigurationLoader {

    private String environment;

    @Inject
    public MetadataSourceConfigurationLoader(String environment) {
        this.environment = environment;
    }

    public MetadataSourceConfiguration downloadConfig() throws ConfigSourceException {
        InputStream configFile = getClass().getClassLoader().getResourceAsStream(environment + "/" + AGGREGATOR_CONFIG_FILE_NAME);

        if (configFile == null) {
            throw new ConfigSourceException("Config file could not be located for the following environment: " + environment);
        }

        String result = new BufferedReader(new InputStreamReader(configFile))
                .lines().collect(Collectors.joining("\n"));

        try {
            return new ObjectMapper().readValue(result, MetadataSourceConfiguration.class);
        } catch (IOException e) {
            throw new ConfigSourceException("Unable to deserialise config file", e);
        }
    }
}
