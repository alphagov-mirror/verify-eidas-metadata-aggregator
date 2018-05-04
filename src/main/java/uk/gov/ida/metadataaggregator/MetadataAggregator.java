package uk.gov.ida.metadataaggregator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;

import static uk.gov.ida.metadataaggregator.Logging.*;

@SuppressWarnings("unused")
public class MetadataAggregator {

    public void s3BucketLambda(AggregatorConfig testObject) {
        S3BucketClient s3BucketClient = new S3BucketClient();
        aggregateMetadata(s3BucketClient, s3BucketClient);
    }

    private void aggregateMetadata(ConfigSource configSource, MetadataUploader metadataDestination) {
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
            boolean successfulUpload = processMetadataFrom(url, metadataDestination);
            if (successfulUpload) successfulUploads++;
        }

        log(
                "Finished processing country metadata with {0} successful uploads out of {1}",
                successfulUploads,
                metadataUrls.size()
        );
    }

    private boolean processMetadataFrom(String url, MetadataUploader metadataDestination) {
        String countryMetadataFile;
        try {
            countryMetadataFile = downloadMetadata(url);
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

    private String downloadMetadata(String url) throws IOException {
        URLConnection urlConnection = new URL(url).openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(
                urlConnection.getInputStream()));
        String inputLine;
        StringBuilder html = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            html.append(inputLine);
        }
        in.close();

        return html.toString();
    }
}
