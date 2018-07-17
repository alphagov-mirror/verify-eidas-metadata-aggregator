package uk.gov.ida.metadataaggregator.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static uk.gov.ida.metadataaggregator.LambdaConstants.AGGREGATOR_CONFIG_FILE_NAME;

public class EnvironmentFileConfigSource implements ConfigSource {

    private String environment;

    public EnvironmentFileConfigSource(String environment) {
        this.environment = environment;
    }

    @Override
    public AggregatorConfig downloadConfig() throws ConfigSourceException {

        InputStream configFile = getClass().getClassLoader().getResourceAsStream(environment + AGGREGATOR_CONFIG_FILE_NAME);

        if (configFile == null) {
            throw new ConfigSourceException("Config file could not be located for the following environment: " + environment);
        }

        String result = new BufferedReader(new InputStreamReader(configFile))
                .lines().collect(Collectors.joining("\n"));
        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(result, AggregatorConfig.class);
        } catch (IOException e) {
            throw new ConfigSourceException("Unable to deserialise config file", e);
        }
    }
}
