package uk.gov.ida.metadataaggregator;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.metadataaggregator.apigateway.ApiGatewayProxyResponse;
import uk.gov.ida.metadataaggregator.apigateway.ApiGatewayRequest;
import uk.gov.ida.metadataaggregator.config.EnvironmentFileConfigSource;
import uk.gov.ida.metadataaggregator.config.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataCurler;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataSource;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataValidatingResolver;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataSourceException;

import javax.xml.parsers.ParserConfigurationException;

import static uk.gov.ida.metadataaggregator.LambdaConstants.BUCKET_NAME;
import static uk.gov.ida.metadataaggregator.LambdaConstants.ENVIRONMENT_KEY;
import static uk.gov.ida.metadataaggregator.LambdaConstants.SERVER_ERROR_STATUS_CODE;
import static uk.gov.ida.metadataaggregator.LambdaConstants.SUCCESS_STATUS_CODE;
import static uk.gov.ida.metadataaggregator.LambdaConstants.TRUST_ANCHOR_PASSCODE;
import static uk.gov.ida.metadataaggregator.LambdaConstants.TRUST_ANCHOR_URI;

@SuppressWarnings("unused")
public class AwsLambdaHandlers {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLambdaHandlers.class);

    public ApiGatewayProxyResponse s3BucketLambda(ApiGatewayRequest testObject) {

        try {
            CountryMetadataCurler countryMetadataCurler = new CountryMetadataCurler();
            boolean wasSuccessful = isMetadataAggregatorSuccessful(countryMetadataCurler);
            if (!wasSuccessful) {
                LOGGER.error("Metadata Aggregator failed");
                return new ApiGatewayProxyResponse(SERVER_ERROR_STATUS_CODE, null, null);
            }
            return new ApiGatewayProxyResponse(SUCCESS_STATUS_CODE, null, null);

        } catch (EnvironmentVariableException e) {
            LOGGER.error("Environment variable is not defined", e);
            return new ApiGatewayProxyResponse(SERVER_ERROR_STATUS_CODE, null, null);
        } catch (ParserConfigurationException e) {
            LOGGER.error("Unable to create XML parser", e);
            return new ApiGatewayProxyResponse(SERVER_ERROR_STATUS_CODE, null, null);
        }
    }

    public void s3BucketLambda(MetadataSourceConfiguration testObject) {

        try {
            CountryMetadataCurler countryMetadataCurler = new CountryMetadataCurler();
            boolean wasSuccessful = isMetadataAggregatorSuccessful(countryMetadataCurler);
            if (!wasSuccessful) {
                LOGGER.error("Metadata Aggregator failed");
            }
        } catch (EnvironmentVariableException e) {
            LOGGER.error("Environment variable is not defined", e);
        } catch (ParserConfigurationException e) {
            LOGGER.error("Unable to create XML parser", e);
        }
    }

    public void s3BucketValidatingLambda(MetadataSourceConfiguration configObject) {

        try {
            CountryMetadataValidatingResolver validatingResolver = getValidatingResolver(configObject);
            boolean wasSuccessful = isMetadataAggregatorSuccessful(validatingResolver);
            if (!wasSuccessful) {
                LOGGER.error("Metadata Aggregator failed");
            }
        } catch (EnvironmentVariableException e) {
            LOGGER.error("Environment variable is not defined", e);
        } catch (MetadataSourceException e) {
            LOGGER.error("Unable to build country metadatasource resolver. MetadataSourceException", e);
        }
    }

    private boolean isMetadataAggregatorSuccessful(CountryMetadataSource countryMetadataSource) throws EnvironmentVariableException {
        S3BucketClient s3BucketClient = getS3BucketClient();
        String environmentVariable = getEnvironmentVariable(ENVIRONMENT_KEY);
        EnvironmentFileConfigSource environmentFileConfigSource = new EnvironmentFileConfigSource(environmentVariable);

        return new MetadataAggregator(environmentFileConfigSource, countryMetadataSource, s3BucketClient).aggregateMetadata();
    }

    private CountryMetadataValidatingResolver getValidatingResolver(MetadataSourceConfiguration configObject) throws EnvironmentVariableException, MetadataSourceException {
        String password = getEnvironmentVariable(TRUST_ANCHOR_PASSCODE);
        String eidasTrustAnchorUriString = getEnvironmentVariable(TRUST_ANCHOR_URI);

        return CountryMetadataValidatingResolver.build(configObject, password, eidasTrustAnchorUriString);
    }

    private S3BucketClient getS3BucketClient() throws EnvironmentVariableException {
        String bucketName = getEnvironmentVariable(BUCKET_NAME);

        return new S3BucketClient(
                bucketName,
                AmazonS3ClientBuilder.standard()
                    .withCredentials(new EnvironmentVariableCredentialsProvider())
                    .build()
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
