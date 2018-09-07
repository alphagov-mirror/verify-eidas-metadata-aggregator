package uk.gov.ida.metadataaggregator.managed;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.metadataaggregator.configuration.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.core.CountryMetadataResolver;
import uk.gov.ida.metadataaggregator.core.MetadataAggregator;
import uk.gov.ida.metadataaggregator.core.S3BucketMetadataStore;
import uk.gov.ida.saml.metadata.EidasTrustAnchorResolver;

// Wraps all of the dependencies required to run metadata aggregations.
public class MetadataAggregationTaskRunner implements Runnable{
    private final Logger log = LoggerFactory.getLogger(MetadataAggregationTaskRunner.class);

    private final MetadataSourceConfiguration configSource;
    private final S3BucketMetadataStore metadataStore;
    private final EidasTrustAnchorResolver eidasTrustAnchorResolver;

    @Inject
    public MetadataAggregationTaskRunner(MetadataSourceConfiguration configSource,
                                         S3BucketMetadataStore metadataStore,
                                         EidasTrustAnchorResolver eidasTrustAnchorResolver) {
        this.configSource = configSource;
        this.metadataStore = metadataStore;
        this.eidasTrustAnchorResolver = eidasTrustAnchorResolver;
    }

    public void run() {
        try {
            log.info("Beginning metadata aggregation");
            CountryMetadataResolver countryMetadataSource = CountryMetadataResolver.fromTrustAnchor(eidasTrustAnchorResolver);
            MetadataAggregator metadataAggregator = new MetadataAggregator(configSource, countryMetadataSource, metadataStore);
            boolean result = metadataAggregator.aggregateMetadata();
            log.info("Completed metadata aggregation {}", result ? "successfully" : "unsuccessfully");
        } catch (Exception e) {
            log.error("Uncaught error during metadata aggregation", e);
        }
    }
}
