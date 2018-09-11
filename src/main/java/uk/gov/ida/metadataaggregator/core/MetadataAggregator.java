package uk.gov.ida.metadataaggregator.core;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.metadataaggregator.configuration.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.exceptions.MetadataSourceException;
import uk.gov.ida.metadataaggregator.exceptions.MetadataStoreException;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MetadataAggregator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataAggregator.class);

    private final MetadataSourceConfiguration configuration;
    private final CountryMetadataResolver countryMetadataResolver;
    private final MetadataStore metadataStore;

    public MetadataAggregator(MetadataSourceConfiguration configuration,
                              CountryMetadataResolver countryMetadataResolver,
                              MetadataStore metadataStore) {
        this.configuration = configuration;
        this.metadataStore = metadataStore;
        this.countryMetadataResolver = countryMetadataResolver;
    }

    public StatusReport aggregateMetadata() {
        LOGGER.info("Processing country metadatasource");

        StatusReport report = new StatusReport(configuration.getMetadataUrls());
        Collection<URL> configMetadataUrls = configuration.getMetadataUrls().values();

        deleteMetadataWhichIsNotInConfig(configMetadataUrls);

        for (URL metadataUrl : configMetadataUrls) {
            try {
                EntityDescriptor countryMetadataFile = countryMetadataResolver.downloadMetadata(metadataUrl);
                metadataStore.upload(metadataUrl.toString(), countryMetadataFile);
                report.recordSuccess(metadataUrl);
            } catch (MetadataSourceException | MetadataStoreException e) {
                LOGGER.error("Error processing metadata {}", metadataUrl, e);
                deleteMetadata(metadataUrl.toString());
                report.recordFailure(metadataUrl, e);
            }
        }

        LOGGER.info("Finished processing country metadatasource with {} successful uploads out of {}", report.numberOfSuccesses(), configMetadataUrls.size());

        return report;
    }

    private void deleteMetadataWhichIsNotInConfig(Collection<URL> configMetadataUrls) {
        List<String> toRemoveBucketUrls = getAllUrlsFromS3Bucket();
        toRemoveBucketUrls.removeAll(configMetadataUrls.stream().map(URL::toString).collect(Collectors.toSet()));

        for (String bucketUrl : toRemoveBucketUrls) {
            deleteMetadata(bucketUrl);
        }
    }

    private List<String> getAllUrlsFromS3Bucket() {
        try {
            return metadataStore.list();
        } catch (MetadataStoreException e) {
            LOGGER.error("Metadata Aggregator error - Unable to retrieve keys from S3 bucket", e);
            return Collections.emptyList();
        }
    }

    private void deleteMetadata(String hexEncodedUrl) {
        try {
            metadataStore.delete(hexEncodedUrl);
        } catch (MetadataStoreException e) {
            LOGGER.error("Error deleting metadatasource file with hexEncodedUrl: {}", hexEncodedUrl, e);
        }
    }
}
