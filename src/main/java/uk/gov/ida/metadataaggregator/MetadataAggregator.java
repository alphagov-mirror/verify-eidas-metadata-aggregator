package uk.gov.ida.metadataaggregator;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.Collection;

@SuppressWarnings("unused")
public class MetadataAggregator {

    private final S3Handler s3Handler = new S3Handler();

    @SuppressWarnings("unused")
    public void myHandler(AggregatorConfig testObject) {

        AggregatorConfig config;
        try {
            config = s3Handler.downloadResource();
        } catch(AmazonClientException e) {
            Logging.log("Error retrieving file from {0}", e, s3Handler.getConfigSource());
            return;
        } catch (IOException e) {
            Logging.log("Unable to parse config file", e);
            return;
        }

        Logging.log("Processing country metadata");

        int successfulUploads = 0;
        Collection<String> metadataUrls = config.getMetadataUrls();

        for (String url : metadataUrls) {
            boolean successfulUpload = processMetadataFrom(url);
            if (successfulUpload) successfulUploads++;
        }

        Logging.log(
                "Finished processing country metadata with {0} successful uploads out of {1}",
                successfulUploads,
                metadataUrls.size()
        );
    }

    AggregatorConfig downloadResource() throws IOException {
        return s3Handler.downloadResource();
    }

    private boolean processMetadataFrom(String url) {
        String aggregatedMetadata;
        try {
            aggregatedMetadata = downloadUrl(url);
        } catch (IOException e) {
            Logging.log("Error downloading metadata file {0}", e, url);
            return false;
        }

        int contentLength = aggregatedMetadata.length();

        try {
            s3Handler.uploadMetadata(url, aggregatedMetadata, objectMetadata(contentLength));
        } catch (UnsupportedEncodingException e) {
            Logging.log("Error uploading metadata file {0} to S3 bucket", e, url);
            return false;
        }
        return true;
    }

    private String downloadUrl(String url) throws IOException {
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

    private ObjectMetadata objectMetadata(int contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        return metadata;
    }

    static class Logging {
        static void log(String message, Throwable e, Object... args) {
            log(message + ": " + e.getMessage(), args);
            e.printStackTrace();
        }

        static void log(String message, Object... args) {
            System.out.println(MessageFormat.format(message, args));
        }
    }
}
