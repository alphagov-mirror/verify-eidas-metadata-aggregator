package uk.gov.ida.metadataaggregator;

import org.junit.Before;
import org.junit.Test;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import uk.gov.ida.metadataaggregator.config.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.config.ConfigSourceException;
import uk.gov.ida.metadataaggregator.config.EnvironmentFileConfigSource;

import java.net.URL;
import java.util.Collection;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.api.Java6Assertions.assertThatThrownBy;

public class EnvironmentFileConfigSourceTest {

    private static final String TEST_JSON_DIRECTORY = "testJson";
    private static final String ERROR_JSON_DIRECTORY = "errorJson";
    private static final String MISSING_JSON_DIRECTORY = "missingJson";

    @Before
    public void setUp() throws InitializationException {
        InitializationService.initialize();
    }

    @Test
    public void shouldThrowWhenConfigFileCannotBeLocated() {
        assertThatThrownBy(() -> new EnvironmentFileConfigSource(MISSING_JSON_DIRECTORY).downloadConfig()).isInstanceOf(ConfigSourceException.class);
    }

    @Test
    public void shouldThrowWhenJsonFileIsNotWellFormed() {
        assertThatThrownBy(() -> new EnvironmentFileConfigSource(ERROR_JSON_DIRECTORY).downloadConfig()).isInstanceOf(ConfigSourceException.class);
    }

    @Test
    public void shouldLocateTestConfigFileAndMapIntoConfigObject() throws ConfigSourceException {
        MetadataSourceConfiguration aggregatorConfig = new EnvironmentFileConfigSource(TEST_JSON_DIRECTORY).downloadConfig();

        Collection<URL> metadataUrls = aggregatorConfig.getMetadataUrls().values();

        assertThat(metadataUrls).hasSize(4);
    }
}
