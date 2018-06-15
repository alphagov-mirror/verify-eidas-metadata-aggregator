package uk.gov.ida.metadataaggregator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.metadataaggregator.config.ConfigSource;
import uk.gov.ida.metadataaggregator.config.ConfigSourceException;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataSource;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataSourceException;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStore;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStoreException;

import java.util.Collection;

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

    public void aggregateMetadata() {
        AggregatorConfig config;
        try {
            config = configSource.downloadConfig();
        } catch (ConfigSourceException e) {
            LOGGER.error("Metadata Aggregator error - Unable to download Aggregator Config file: {}", e.getMessage());
            return;
        }

        LOGGER.info("Processing country metadatasource");

        int successfulUploads = 0;
        Collection<String> metadataUrls = config.getMetadataUrls();

        for (String url : metadataUrls) {
            boolean successfulUpload = processMetadataFrom(url);
            if (successfulUpload) successfulUploads++;
        }

        LOGGER.info("Finished processing country metadatasource with {} successful uploads out of {}", successfulUploads, metadataUrls.size());
    }

    private boolean processMetadataFrom(String url) {
        String countryMetadataFile;
        try {
            countryMetadataFile = countryMetadataCurler.downloadMetadata(url);
        } catch (MetadataSourceException e) {
            LOGGER.error("Error downloading metadatasource file {} Exception: {}", url, e.getMessage());
            deleteMetadata(url);
            return false;
        }

        try {
            metadataStore.uploadMetadata(url, countryMetadataFile);
        } catch (MetadataStoreException e) {
            LOGGER.error("Error uploading metadatasource file {} Exception: {}", url, e.getMessage());
            deleteMetadata(url);
            return false;
        }
        return true;
    }

    private void deleteMetadata(String url) {
        try {
            metadataStore.deleteMetadata(url);
        } catch (MetadataStoreException e) {
            LOGGER.error("Error deleting metadatasource file {} Exception: {}", url, e.getMessage());
        }
    }
}
