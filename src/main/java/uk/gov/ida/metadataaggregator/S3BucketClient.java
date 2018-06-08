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
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.metadataaggregator.config.ConfigSource;
import uk.gov.ida.metadataaggregator.config.ConfigSourceException;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStore;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStoreException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.stream.Collectors;

import static uk.gov.ida.metadataaggregator.Logging.*;

class S3BucketClient implements ConfigSource, MetadataStore {
    private static final String CONFIG_BUCKET_KEY = "CONFIG_BUCKET_KEY";

    private final String bucketName;
    private final AmazonS3Client s3Client;

    public S3BucketClient(String configBucket, AmazonS3Client s3Client) {
        this.bucketName = configBucket;
        this.s3Client = s3Client;
    }

    @Override
    public AggregatorConfig downloadConfig() throws ConfigSourceException {

        log("Downloading config file from {0}", bucketName);

        S3Object object;
        try {
            object = s3Client.getObject(bucketName, CONFIG_BUCKET_KEY);
        } catch (AmazonClientException e) {
            throw new ConfigSourceException(MessageFormat.format("Error retrieving file from {0}", e, "S3:" + bucketName), e);
        }

        S3ObjectInputStream objectContent = object.getObjectContent();
        String result = new BufferedReader(new InputStreamReader(objectContent))
                .lines().collect(Collectors.joining("\n"));
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(result, AggregatorConfig.class);
        } catch (IOException e) {
            throw new ConfigSourceException("Unable to deserialise downloaded config", e);
        }
    }

    @Override
    public void uploadMetadata(String resourceName, String metadataFile) throws MetadataStoreException {
        ObjectMetadata objectMetadata = objectMetadata(metadataFile.length());

        String hexEncodedUrl = Hex.encodeHexString(resourceName.getBytes());
        StringInputStream metadataStream;
        try {
            metadataStream = new StringInputStream(metadataFile);
        } catch (UnsupportedEncodingException e) {
            throw new MetadataStoreException("Error opening metadata file stream to store", e);
        }
        s3Client.putObject(new PutObjectRequest(bucketName, hexEncodedUrl, metadataStream, objectMetadata));
    }

    private ObjectMetadata objectMetadata(int contentLength) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        return metadata;
    }
}
