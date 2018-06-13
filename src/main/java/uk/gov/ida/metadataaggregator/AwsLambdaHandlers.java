package uk.gov.ida.metadataaggregator;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.ida.metadataaggregator.apigateway.ApiGatewayProxyResponse;
import uk.gov.ida.metadataaggregator.apigateway.ApiGatewayRequest;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataCurler;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataValidatingResolver;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataSourceException;

import static uk.gov.ida.metadataaggregator.LambdaConstants.AWS_ACCESS_KEY;
import static uk.gov.ida.metadataaggregator.LambdaConstants.AWS_SECRET_KEY;
import static uk.gov.ida.metadataaggregator.LambdaConstants.BUCKET_NAME;
import static uk.gov.ida.metadataaggregator.LambdaConstants.SUCCESS_STATUS_CODE;
import static uk.gov.ida.metadataaggregator.LambdaConstants.TRUST_ANCHOR_PASSCODE;
import static uk.gov.ida.metadataaggregator.LambdaConstants.TRUST_ANCHOR_URI;

@SuppressWarnings("unused")
public class AwsLambdaHandlers {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsLambdaHandlers.class);

    public ApiGatewayProxyResponse s3BucketLambda(ApiGatewayRequest testObject) {
        S3BucketClient s3BucketClient = getS3BucketClient();
        new MetadataAggregator(s3BucketClient, new CountryMetadataCurler(), s3BucketClient).aggregateMetadata();
        return new ApiGatewayProxyResponse(SUCCESS_STATUS_CODE, null, null);
    }

    public void s3BucketLambda(AggregatorConfig testObject) {
        S3BucketClient s3BucketClient = getS3BucketClient();
        new MetadataAggregator(s3BucketClient, new CountryMetadataCurler(), s3BucketClient).aggregateMetadata();
    }

    public void s3BucketValidatingLambda(AggregatorConfig testObject) {
        String password = getEnvironmentVariable(TRUST_ANCHOR_PASSCODE);
        String eidasTrustAnchorUriString = getEnvironmentVariable(TRUST_ANCHOR_URI);

        CountryMetadataValidatingResolver validatingResolver;
        try {
            validatingResolver = CountryMetadataValidatingResolver.build(testObject, password, eidasTrustAnchorUriString);
        } catch (MetadataSourceException e) {
            LOGGER.error("Unable to build country metadatasource resolver", new Object[]{}, e);
            return;
        }

        S3BucketClient s3BucketClient = getS3BucketClient();
        new MetadataAggregator(s3BucketClient, validatingResolver, s3BucketClient).aggregateMetadata();
    }

    private S3BucketClient getS3BucketClient() {
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

    private String getEnvironmentVariable(String envKey) {
        String awsAccessKey = System.getenv(envKey);
        if(awsAccessKey == null){
            throw new IllegalStateException(envKey+" is not defined");
        }
        return awsAccessKey;
    }
}
