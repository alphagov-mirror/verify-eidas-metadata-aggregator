package uk.gov.ida.metadataaggregator;

import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.metadataaggregator.config.ConfigSource;
import uk.gov.ida.metadataaggregator.config.ConfigSourceException;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataSource;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataSourceException;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStore;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStoreException;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MetadataAggregator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataAggregator.class);

    private final ConfigSource configSource;
    private final CountryMetadataSource countryMetadataCurler;
    private final MetadataStore metadataStore;

    public MetadataAggregator(ConfigSource configSource,
                              CountryMetadataSource countryMetadataCurler,
                              MetadataStore metadataStore) {
        this.configSource = configSource;
        this.metadataStore = metadataStore;
        this.countryMetadataCurler = countryMetadataCurler;
    }

    public boolean aggregateMetadata() {
        AggregatorConfig config;
        try {
            config = configSource.downloadConfig();
        } catch (ConfigSourceException e) {
            LOGGER.error("Metadata Aggregator error - Unable to download Aggregator Config file: {}", e.getMessage());
            return false;
        }

        LOGGER.info("Processing country metadatasource");

        int successfulUploads = 0;
        Collection<URL> configMetadataUrls = config.getMetadataUrls();

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
            countryMetadataFile = countryMetadataCurler.downloadMetadata(metadataUrl);
        } catch (MetadataSourceException e) {
            LOGGER.error("Error downloading metadatasource file {} Exception: {}", metadataUrl, e.getMessage());
            deleteMetadataWithHexEncodedMetadataUrl(HexUtils.encodeString(metadataUrl.toString()));
            return false;
        }

        try {
            metadataStore.uploadMetadata(HexUtils.encodeString(metadataUrl.toString()), countryMetadataFile);
        } catch (MetadataStoreException e) {
            LOGGER.error("Error uploading metadatasource file {} Exception: {}", metadataUrl, e.getMessage());
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
            hexEncodedUrls = metadataStore.getAllHexEncodedUrlsFromS3Bucket();
        } catch (MetadataStoreException e) {
            LOGGER.error("Metadata Aggregator error - Unable to retrieve keys from S3 bucket", e.getMessage());
        }
        return hexEncodedUrls;
    }

    private void deleteMetadataWithHexEncodedMetadataUrl(String hexEncodedUrl) {
        try {
            metadataStore.deleteMetadata(hexEncodedUrl);
        } catch (MetadataStoreException e) {
            LOGGER.error("Error deleting metadatasource file with hexEncodedUrl: {} Exception: {}", hexEncodedUrl, e.getMessage());
        }
    }
}
