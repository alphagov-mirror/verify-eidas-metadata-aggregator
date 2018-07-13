package uk.gov.ida.metadataaggregator.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class DistributionArchiveConfigSource implements ConfigSource {

    private String environment;

    public DistributionArchiveConfigSource(String environment) {
        this.environment = environment;
    }

    @Override
    public AggregatorConfig downloadConfig() throws ConfigSourceException {

        InputStream configFile = getClass().getClassLoader().getResourceAsStream(environment + "AggregatorConfig.json");

        String result = new BufferedReader(new InputStreamReader(configFile))
                .lines().collect(Collectors.joining("\n"));
        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(result, AggregatorConfig.class);
        } catch (IOException e) {
            throw new ConfigSourceException("Unable to deserialise downloaded config", e);
        }
    }
}
