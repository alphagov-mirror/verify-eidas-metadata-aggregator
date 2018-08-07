package uk.gov.ida.metadataaggregator;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.ida.metadataaggregator.config.ConfigSource;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataSource;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataValidatingResolver;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStore;
import uk.gov.ida.saml.metadata.EidasTrustAnchorResolver;

import static java.lang.String.format;

// Wraps all of the dependencies required to run metadata aggregations.
class MetadataAggregationTaskRunner {
    private final Logger log = LoggerFactory.getLogger(MetadataAggregationTaskRunner.class);

    private final ConfigSource configSource;
    private final MetadataStore metadataStore;
    private final EidasTrustAnchorResolver eidasTrustAnchorResolver;

    @Inject
    public MetadataAggregationTaskRunner(
        ConfigSource configSource,
        MetadataStore metadataStore,
        EidasTrustAnchorResolver eidasTrustAnchorResolver) {
        this.configSource = configSource;
        this.metadataStore = metadataStore;
        this.eidasTrustAnchorResolver = eidasTrustAnchorResolver;
    }

    public void run(String taskDescription) {
        try {
            log.info("Beginning {} metadata aggregation", taskDescription.toLowerCase());
            CountryMetadataSource countryMetadataSource = CountryMetadataValidatingResolver.fromTrustAnchor(eidasTrustAnchorResolver);
            MetadataAggregator metadataAggregator = new MetadataAggregator(configSource, countryMetadataSource, metadataStore);
            boolean result = metadataAggregator.aggregateMetadata();
            log.info("{} metadata aggregation completed {}", taskDescription, result ? "successfully" : "unsuccessfully");
        } catch (Exception e) {
            log.error(format("Uncaught error during {} metadata aggregation", taskDescription.toLowerCase()), e);
        }
    }
}