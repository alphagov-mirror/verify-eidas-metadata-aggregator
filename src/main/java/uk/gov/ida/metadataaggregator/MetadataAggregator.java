package uk.gov.ida.metadataaggregator;

import java.io.IOException;
import java.util.Collection;

import static uk.gov.ida.metadataaggregator.Logging.*;

@SuppressWarnings("unused")
public class MetadataAggregator {

    public void s3BucketLambda(AggregatorConfig testObject) {
        S3BucketClient s3BucketClient = new S3BucketClient();
        aggregateMetadata(s3BucketClient, s3BucketClient, new CountryMetadataCurler());
    }

    private void aggregateMetadata(ConfigSource configSource,
                                   MetadataUploader metadataDestination,
                                   CountryMetadataSource countryMetadataCurler) {
        AggregatorConfig config;
        try {
            config = configSource.downloadConfig();
        } catch (IOException e) {
            log("Unable to retrieve config file", e);
            return;
        }

        log("Processing country metadata");

        int successfulUploads = 0;
        Collection<String> metadataUrls = config.getMetadataUrls();

        for (String url : metadataUrls) {
            boolean successfulUpload = processMetadataFrom(url, metadataDestination, countryMetadataCurler);
            if (successfulUpload) successfulUploads++;
        }

        log(
                "Finished processing country metadata with {0} successful uploads out of {1}",
                successfulUploads,
                metadataUrls.size()
        );
    }

    private boolean processMetadataFrom(String url,
                                        MetadataUploader metadataDestination,
                                        CountryMetadataSource countryMetadataCurler) {
        String countryMetadataFile;
        try {
            countryMetadataFile = countryMetadataCurler.downloadMetadata(url);
        } catch (IOException e) {
            log("Error downloading metadata file {0}", e, url);
            return false;
        }

        try {
            metadataDestination.uploadMetadata(url, countryMetadataFile);
        } catch (IOException e) {
            log("Error uploading metadata file {0}", e, url);
            return false;
        }

        return true;
    }
}
