package uk.gov.ida.metadataaggregator;

import org.junit.Before;
import org.junit.Test;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import uk.gov.ida.metadataaggregator.configuration.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.configuration.ConfigSourceException;
import uk.gov.ida.metadataaggregator.configuration.MetadataSourceConfigurationLoader;

import java.net.URL;
import java.util.Collection;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class MetadataSourceConfigurationLoaderTest {

    private static final String TEST_JSON_DIRECTORY = "testJson";
    private static final String ERROR_JSON_DIRECTORY = "errorJson";
    private static final String MISSING_JSON_DIRECTORY = "missingJson";
    private static final String EXTRA_PROPERTY_JSON_DIRECTORY = "extraPropertyJson";

    @Before
    public void setUp() throws InitializationException {
        InitializationService.initialize();
    }

    @Test
    public void shouldThrowWhenConfigFileCannotBeLocated() {
        assertThatThrownBy(() -> new MetadataSourceConfigurationLoader(MISSING_JSON_DIRECTORY).downloadConfig()).isInstanceOf(ConfigSourceException.class);
    }

    @Test
    public void shouldThrowWhenJsonFileIsNotWellFormed() {
        assertThatThrownBy(() -> new MetadataSourceConfigurationLoader(ERROR_JSON_DIRECTORY).downloadConfig()).isInstanceOf(ConfigSourceException.class);
    }

    @Test
    public void shouldLocateTestConfigFileAndMapIntoConfigObject() throws ConfigSourceException {
        MetadataSourceConfiguration aggregatorConfig = new MetadataSourceConfigurationLoader(TEST_JSON_DIRECTORY).downloadConfig();

        Collection<URL> metadataUrls = aggregatorConfig.getMetadataUrls().values();

        assertThat(metadataUrls).hasSize(4);
    }

    @Test
    public void shouldIgnoreUnknownProperties() throws ConfigSourceException {
        MetadataSourceConfiguration aggregatorConfig = new MetadataSourceConfigurationLoader(EXTRA_PROPERTY_JSON_DIRECTORY).downloadConfig();

        Collection<URL> metadataUrls = aggregatorConfig.getMetadataUrls().values();

        assertThat(metadataUrls).hasSize(2);
    }
}
