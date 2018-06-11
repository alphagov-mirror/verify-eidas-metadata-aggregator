package uk.gov.ida.metadataaggregator;

import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.metadataaggregator.config.ConfigSource;
import uk.gov.ida.metadataaggregator.config.ConfigSourceException;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataSource;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataSourceException;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStore;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStoreException;

import java.util.Collection;

import static uk.gov.ida.metadataaggregator.Logging.log;

public class MetadataAggregator {

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
            log("Unable to retrieve config file", e);
            return;
        }

        log("Processing country metadatasource");

        int successfulUploads = 0;
        Collection<String> metadataUrls = config.getMetadataUrls();

        for (String url : metadataUrls) {
            boolean successfulUpload = processMetadataFrom(url);
            if (successfulUpload) successfulUploads++;
        }

        log(
                "Finished processing country metadatasource with {0} successful uploads out of {1}",
                successfulUploads,
                metadataUrls.size()
        );
    }

    private boolean processMetadataFrom(String url) {
        String countryMetadataFile;
        try {
            countryMetadataFile = countryMetadataCurler.downloadMetadata(url);
        } catch (MetadataSourceException e) {
            log("Error downloading metadatasource file {0}", e, url);
            return false;
        }

        try {
            metadataStore.uploadMetadata(url, countryMetadataFile);
        } catch (MetadataStoreException e) {
            log("Error uploading metadatasource file {0}", e, url);
            return false;
        }

        return true;
    }
}
