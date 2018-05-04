package uk.gov.ida.metadataaggregator;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.StringInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Hex;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.stream.Collectors;

//TODO Rename this
class S3Handler {
    private static final String CONFIG_BUCKET_NAME = System.getenv("CONFIG_BUCKET"); //"govukverify-eidas-metadata-aggregator-config-dev";
    private static final String METADATA_BUCKET_NAME = System.getenv("METADATA_BUCKET"); //"govukverify-eidas-metadata-aggregator-dev";

    private static final java.lang.String CONFIG_BUCKET_KEY = "CONFIG_BUCKET_KEY";
    private final java.lang.String accessKey = java.lang.System.getenv("AWS_ACCESS_KEY");
    private final java.lang.String secretKey = java.lang.System.getenv("AWS_SECRET_KEY");
    private final com.amazonaws.services.s3.AmazonS3Client s3Client = new com.amazonaws.services.s3.AmazonS3Client(new com.amazonaws.auth.BasicAWSCredentials(accessKey, secretKey));

    AggregatorConfig downloadResource() throws java.io.IOException {

        MetadataAggregator.Logging.log("Downloading config file from {0}", CONFIG_BUCKET_NAME);

        S3Object object = s3Client.getObject(CONFIG_BUCKET_NAME, CONFIG_BUCKET_KEY);
        S3ObjectInputStream objectContent = object.getObjectContent();
        String result = new BufferedReader(new InputStreamReader(objectContent))
                .lines().collect(Collectors.joining("\n"));
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result, AggregatorConfig.class);
    }

    void uploadMetadata(String url, String countryMetadata, ObjectMetadata metadata) throws UnsupportedEncodingException {
        String hexEncodedUrl = Hex.encodeHexString(url.getBytes());
        StringInputStream metadataStream = new StringInputStream(countryMetadata);
        s3Client.putObject(new PutObjectRequest(METADATA_BUCKET_NAME, hexEncodedUrl, metadataStream, metadata));
    }

    public String getConfigSource() {
        return "S3:"+CONFIG_BUCKET_NAME;
    }
}
