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

    public boolean aggregateMetadata() {
        LOGGER.info("Processing country metadatasource");

        int successfulUploads = 0;
        Collection<URL> configMetadataUrls = configuration.getMetadataUrls().values();

        deleteMetadataWhichIsNotInConfig(configMetadataUrls);

        for (URL url : configMetadataUrls) {
            boolean successfulUpload = processMetadataFrom(url);
            if (successfulUpload) successfulUploads++;
        }

        LOGGER.info("Finished processing country metadatasource with {} successful uploads out of {}", successfulUploads, configMetadataUrls.size());

        return successfulUploads == configMetadataUrls.size();
    }

    private boolean processMetadataFrom(URL metadataUrl) {
        EntityDescriptor countryMetadataFile;
        try {
            countryMetadataFile = countryMetadataResolver.downloadMetadata(metadataUrl);
        } catch (MetadataSourceException e) {
            LOGGER.error("Error downloading metadatasource file {}", metadataUrl, e);
            deleteMetadataWithHexEncodedMetadataUrl(HexUtils.encodeString(metadataUrl.toString()));
            return false;
        }

        try {
            s3BucketMetadataStore.uploadMetadata(HexUtils.encodeString(metadataUrl.toString()), countryMetadataFile);
        } catch (MetadataStoreException e) {
            LOGGER.error("Error uploading metadatasource file {}", metadataUrl, e);
            deleteMetadataWithHexEncodedMetadataUrl(HexUtils.encodeString(metadataUrl.toString()));
            return false;
        }
        return true;
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
