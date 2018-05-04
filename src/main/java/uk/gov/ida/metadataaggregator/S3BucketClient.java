package uk.gov.ida.metadataaggregator;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.StringInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Hex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static uk.gov.ida.metadataaggregator.Logging.*;

class S3BucketClient implements ConfigSource, MetadataUploader {
    private static final String CONFIG_BUCKET_NAME = System.getenv("CONFIG_BUCKET"); //"govukverify-eidas-metadata-aggregator-config-dev";
    private static final String METADATA_BUCKET_NAME = System.getenv("METADATA_BUCKET"); //"govukverify-eidas-metadata-aggregator-dev";

    private static final java.lang.String CONFIG_BUCKET_KEY = "CONFIG_BUCKET_KEY";
    private final java.lang.String accessKey = java.lang.System.getenv("AWS_ACCESS_KEY");
    private final java.lang.String secretKey = java.lang.System.getenv("AWS_SECRET_KEY");
    private final AmazonS3Client s3Client = new AmazonS3Client(new com.amazonaws.auth.BasicAWSCredentials(accessKey, secretKey));

    @Override
    public AggregatorConfig downloadConfig() throws IOException {

        log("Downloading config file from {0}", CONFIG_BUCKET_NAME);

        S3Object object;
        try {
            object = s3Client.getObject(CONFIG_BUCKET_NAME, CONFIG_BUCKET_KEY);
        } catch (AmazonClientException e) {
            log("Error retrieving file from {0}", e, "S3:" + CONFIG_BUCKET_NAME);
            throw new IOException(e);
        }

        S3ObjectInputStream objectContent = object.getObjectContent();
        String result = new BufferedReader(new InputStreamReader(objectContent))
                .lines().collect(Collectors.joining("\n"));
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result, AggregatorConfig.class);
    }

    @Override
    public void uploadMetadata(String resourceName, String metadataFile) throws IOException {
        ObjectMetadata objectMetadata = objectMetadata(metadataFile.length());

        String hexEncodedUrl = Hex.encodeHexString(resourceName.getBytes());
        StringInputStream metadataStream = new StringInputStream(metadataFile);
        s3Client.putObject(new PutObjectRequest(METADATA_BUCKET_NAME, hexEncodedUrl, metadataStream, objectMetadata));
    }

    private ObjectMetadata objectMetadata(int contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        return metadata;
    }
}
