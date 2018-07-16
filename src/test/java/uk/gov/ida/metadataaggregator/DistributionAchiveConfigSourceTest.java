package uk.gov.ida.metadataaggregator;

import org.junit.Before;
import org.junit.Test;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.metadataaggregator.config.ConfigSourceException;
import uk.gov.ida.metadataaggregator.config.DistributionArchiveConfigSource;

import java.net.URL;
import java.util.Collection;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class DistributionAchiveConfigSourceTest {

    private static final String TEST_JSON = "Test";
    private static final String ERROR_JSON = "Error";
    private static final String MISSING_ENVIRONMENT = "missingEnvironment";

    @Before
    public void setUp() throws InitializationException {
        InitializationService.initialize();
    }

    @Test
    public void shouldLocateConfigFileAndMapIntoConfigObject() throws ConfigSourceException {
        AggregatorConfig aggregatorConfig = new DistributionArchiveConfigSource(TEST_JSON).downloadConfig();

        Collection<URL> metadataUrls = aggregatorConfig.getMetadataUrls().values();

        assertThat(metadataUrls).hasSize(4);
    }

    @Test
    public void shouldThrowWhenConfigFileCannotBeLocated() {
        assertThatThrownBy(() -> new DistributionArchiveConfigSource(MISSING_ENVIRONMENT).downloadConfig()).isInstanceOf(ConfigSourceException.class);
    }

    @Test
    public void shouldThrowWhenJsonFileIsNotWellFormed() {
        assertThatThrownBy(() -> new DistributionArchiveConfigSource(ERROR_JSON).downloadConfig()).isInstanceOf(ConfigSourceException.class);
    }
}
