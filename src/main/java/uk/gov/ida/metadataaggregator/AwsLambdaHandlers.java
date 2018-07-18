package uk.gov.ida.metadataaggregator;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.metadataaggregator.apigateway.ApiGatewayProxyResponse;
import uk.gov.ida.metadataaggregator.apigateway.ApiGatewayRequest;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.metadataaggregator.config.EnvironmentFileConfigSource;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataCurler;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataValidatingResolver;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataSourceException;

import javax.xml.parsers.ParserConfigurationException;

import static uk.gov.ida.metadataaggregator.LambdaConstants.AWS_ACCESS_KEY;
import static uk.gov.ida.metadataaggregator.LambdaConstants.AWS_SECRET_KEY;
import static uk.gov.ida.metadataaggregator.LambdaConstants.BUCKET_NAME;
import static uk.gov.ida.metadataaggregator.LambdaConstants.ENVIRONMENT;
import static uk.gov.ida.metadataaggregator.LambdaConstants.SERVER_ERROR_STATUS_CODE;
import static uk.gov.ida.metadataaggregator.LambdaConstants.SUCCESS_STATUS_CODE;
import static uk.gov.ida.metadataaggregator.LambdaConstants.TRUST_ANCHOR_PASSCODE;
import static uk.gov.ida.metadataaggregator.LambdaConstants.TRUST_ANCHOR_URI;

@SuppressWarnings("unused")
public class AwsLambdaHandlers {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLambdaHandlers.class);

    public ApiGatewayProxyResponse s3BucketLambda(ApiGatewayRequest testObject) {
        S3BucketClient s3BucketClient;
        String environmentVariable;
        try {
            environmentVariable = getEnvironmentVariable(ENVIRONMENT);
            s3BucketClient = getS3BucketClient();
        } catch (EnvironmentVariableException e) {
            LOGGER.error("Environment variable is not defined", e);
            return new ApiGatewayProxyResponse(SERVER_ERROR_STATUS_CODE, null, null);
        }
        EnvironmentFileConfigSource environmentFileConfigSource = new EnvironmentFileConfigSource(environmentVariable);

        CountryMetadataCurler countryMetadataCurler;
        try {
            countryMetadataCurler = new CountryMetadataCurler();
        } catch (ParserConfigurationException e) {
            LOGGER.error("Unable to create XML parser", e);
            return new ApiGatewayProxyResponse(SERVER_ERROR_STATUS_CODE, null, null);
        }

        boolean wasSuccessful = new MetadataAggregator(environmentFileConfigSource, countryMetadataCurler, s3BucketClient).aggregateMetadata();
        if (!wasSuccessful) LOGGER.error("Metadata Aggregator failed");
        return new ApiGatewayProxyResponse(SUCCESS_STATUS_CODE, null, null);
    }

    public void s3BucketLambda(AggregatorConfig testObject) {
        S3BucketClient s3BucketClient;
        try {
            s3BucketClient = getS3BucketClient();
        } catch (EnvironmentVariableException e) {
            LOGGER.error("Environment variable is not defined", e);
            return;
        }
        EnvironmentFileConfigSource environmentFileConfigSource = new EnvironmentFileConfigSource(ENVIRONMENT);

        CountryMetadataCurler countryMetadataCurler;
        try {
            countryMetadataCurler = new CountryMetadataCurler();
        } catch (ParserConfigurationException e) {
            LOGGER.error("Unable to create XML parser: {}", e.getMessage());
            return;
        }

        boolean wasSuccessful = new MetadataAggregator(environmentFileConfigSource, countryMetadataCurler, s3BucketClient).aggregateMetadata();
        if (!wasSuccessful) LOGGER.error("Metadata Aggregator failed");
    }

    public void s3BucketValidatingLambda(AggregatorConfig testObject) {
        String password;
        String eidasTrustAnchorUriString;
        S3BucketClient s3BucketClient;

        try {
            s3BucketClient = getS3BucketClient();

            password = getEnvironmentVariable(TRUST_ANCHOR_PASSCODE);
            eidasTrustAnchorUriString = getEnvironmentVariable(TRUST_ANCHOR_URI);
        } catch (EnvironmentVariableException e) {
            LOGGER.error("Environment variable is not defined", e);
            return;
        }
        CountryMetadataValidatingResolver validatingResolver;
        try {
            validatingResolver = CountryMetadataValidatingResolver.build(testObject, password, eidasTrustAnchorUriString);
        } catch (MetadataSourceException e) {
            LOGGER.error("Unable to build country metadatasource resolver. MetadataSourceException : {}", e.getMessage());
            return;
        }

        EnvironmentFileConfigSource environmentFileConfigSource = new EnvironmentFileConfigSource(ENVIRONMENT);
        boolean wasSuccessful = new MetadataAggregator(environmentFileConfigSource, validatingResolver, s3BucketClient).aggregateMetadata();
        if (!wasSuccessful) LOGGER.error("Metadata Aggregator failed");
    }

    private S3BucketClient getS3BucketClient() throws EnvironmentVariableException {
        String awsAccessKey = getEnvironmentVariable(AWS_ACCESS_KEY);
        String awsSecretKey = getEnvironmentVariable(AWS_SECRET_KEY);
        String bucketName = getEnvironmentVariable(BUCKET_NAME);

        return new S3BucketClient(
                bucketName,
                new AmazonS3Client(new BasicAWSCredentials(
                        awsAccessKey,
                        awsSecretKey)
                )
        );
    }

    private String getEnvironmentVariable(String envKey) throws EnvironmentVariableException {
        String awsAccessKey = System.getenv(envKey);
        if (awsAccessKey == null) {
            throw new EnvironmentVariableException(envKey + " is not defined");
        }
        LOGGER.info("Environment variable set for {} is {}", envKey, awsAccessKey);
        return awsAccessKey;
    }
}
