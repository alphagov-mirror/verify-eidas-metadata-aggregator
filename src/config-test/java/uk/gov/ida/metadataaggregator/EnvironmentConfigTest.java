package uk.gov.ida.metadataaggregator;

import org.junit.Before;
import org.junit.Test;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import uk.gov.ida.metadataaggregator.config.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.config.ConfigSourceException;
import uk.gov.ida.metadataaggregator.config.MetadataSourceConfigurationLoader;

import java.net.URL;
import java.util.Collection;

import static org.assertj.core.api.Java6Assertions.assertThat;

public class EnvironmentConfigTest {

    private static final String JOINT = "joint";
    private static final String PROD = "production";
    private static final String INTEGRATION = "integration";
    private static final String STAGING = "staging";

    @Before
    public void setUp() throws InitializationException {
        InitializationService.initialize();
    }

    @Test
    public void shouldLocateJointConfigFileAndSuccessfullyDeserialize() throws ConfigSourceException {
        MetadataSourceConfiguration aggregatorConfig = new MetadataSourceConfigurationLoader(JOINT).downloadConfig();

        Collection<URL> metadataUrls = aggregatorConfig.getMetadataUrls().values();

        assertThat(metadataUrls).isNotEmpty();
    }

    @Test
    public void shouldLocateProdConfigFileAndSuccessfullyDeserialize() throws ConfigSourceException {
        MetadataSourceConfiguration aggregatorConfig = new MetadataSourceConfigurationLoader(PROD).downloadConfig();

        Collection<URL> metadataUrls = aggregatorConfig.getMetadataUrls().values();

        assertThat(metadataUrls).isEmpty();
    }

    @Test
    public void shouldLocateStagingConfigFileAndSuccessfullyDeserialize() throws ConfigSourceException {
        MetadataSourceConfiguration aggregatorConfig = new MetadataSourceConfigurationLoader(STAGING).downloadConfig();

        Collection<URL> metadataUrls = aggregatorConfig.getMetadataUrls().values();

        assertThat(metadataUrls).isEmpty();
    }

    @Test
    public void shouldLocateIntegrationConfigFileAndSuccessfullyDeserialize() throws ConfigSourceException {
        MetadataSourceConfiguration aggregatorConfig = new MetadataSourceConfigurationLoader(INTEGRATION).downloadConfig();

        Collection<URL> metadataUrls = aggregatorConfig.getMetadataUrls().values();

        assertThat(metadataUrls).isNotEmpty();
    }
}
