package uk.gov.ida.metadataaggregator.core;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.metadataaggregator.configuration.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.exceptions.MetadataSourceException;
import uk.gov.ida.metadataaggregator.exceptions.MetadataStoreException;
import uk.gov.ida.metadataaggregator.util.HexUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MetadataAggregator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataAggregator.class);

    private final MetadataSourceConfiguration configuration;
    private final CountryMetadataResolver countryMetadataResolver;
    private final S3BucketMetadataStore s3BucketMetadataStore;

    public MetadataAggregator(MetadataSourceConfiguration configuration,
                              CountryMetadataResolver countryMetadataResolver,
                              S3BucketMetadataStore s3BucketMetadataStore) {
        this.configuration = configuration;
        this.s3BucketMetadataStore = s3BucketMetadataStore;
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
                s3BucketMetadataStore.uploadMetadata(HexUtils.encodeString(metadataUrl.toString()), countryMetadataFile);
                report.recordSuccess(metadataUrl);
            } catch (MetadataSourceException | MetadataStoreException e) {
                LOGGER.error("Error processing metadata {}", metadataUrl, e);
                deleteMetadataWithHexEncodedMetadataUrl(HexUtils.encodeString(metadataUrl.toString()));
                report.recordFailure(metadataUrl, e);
            }
        }

        LOGGER.info("Finished processing country metadatasource with {} successful uploads out of {}", report.numberOfSuccesses(), configMetadataUrls.size());

        return report;
    }

    private void deleteMetadataWhichIsNotInConfig(Collection<URL> configMetadataUrls) {
        List<String> hexedConfigUrls = new ArrayList<>();

        for (URL configMetadataUrl : configMetadataUrls) {
            hexedConfigUrls.add(HexUtils.encodeString(configMetadataUrl.toString()));
        }

        List<String> toRemoveHexedBucketUrls = getAllHexEncodedUrlsFromS3Bucket();

        toRemoveHexedBucketUrls.removeAll(hexedConfigUrls);

        for (String hexedBucketUrl : toRemoveHexedBucketUrls) {
            deleteMetadataWithHexEncodedMetadataUrl(hexedBucketUrl);
        }
    }

    private List<String> getAllHexEncodedUrlsFromS3Bucket() {
        List<String> hexEncodedUrls = new ArrayList<>();
        try {
            hexEncodedUrls = s3BucketMetadataStore.getAllHexEncodedUrlsFromS3Bucket();
        } catch (MetadataStoreException e) {
            LOGGER.error("Metadata Aggregator error - Unable to retrieve keys from S3 bucket", e);
        }
        return hexEncodedUrls;
    }

    private void deleteMetadataWithHexEncodedMetadataUrl(String hexEncodedUrl) {
        try {
            s3BucketMetadataStore.deleteMetadata(hexEncodedUrl);
        } catch (MetadataStoreException e) {
            LOGGER.error("Error deleting metadatasource file with hexEncodedUrl: {}", hexEncodedUrl, e);
        }
    }
}
