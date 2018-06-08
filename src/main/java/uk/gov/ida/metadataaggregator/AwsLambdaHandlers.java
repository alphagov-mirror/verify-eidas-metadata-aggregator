package uk.gov.ida.metadataaggregator;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import uk.gov.ida.metadataaggregator.apigateway.ApiGatewayProxyResponse;
import uk.gov.ida.metadataaggregator.apigateway.ApiGatewayRequest;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataCurler;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataValidatingResolver;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataSourceException;

import static uk.gov.ida.metadataaggregator.Logging.log;

@SuppressWarnings("unused")
public class AwsLambdaHandlers {

    private static final String BUCKET_NAME = "CONFIG_BUCKET";

    public ApiGatewayProxyResponse s3BucketLambda(ApiGatewayRequest testObject) {
        S3BucketClient s3BucketClient = getS3BucketClient();
        new MetadataAggregator(s3BucketClient, new CountryMetadataCurler(), s3BucketClient).aggregateMetadata();
        return new ApiGatewayProxyResponse(200, null, null);
    }

    public void s3BucketLambda(AggregatorConfig testObject) {
        S3BucketClient s3BucketClient = getS3BucketClient();
        new MetadataAggregator(s3BucketClient, new CountryMetadataCurler(), s3BucketClient).aggregateMetadata();
    }

    public void s3BucketValidatingLambda(AggregatorConfig testObject) {
        String password = System.getenv("TRUST_ANCHOR_PASSCODE");
        String eidasTrustAnchorUriString = System.getenv("TRUST_ANCHOR_URI");

        CountryMetadataValidatingResolver validatingResolver;
        try {
            validatingResolver = CountryMetadataValidatingResolver.build(testObject, password, eidasTrustAnchorUriString);
        } catch (MetadataSourceException e) {
            log("Unable to build country metadatasource resolver", e);
            return;
        }

        S3BucketClient s3BucketClient = getS3BucketClient();
        new MetadataAggregator(s3BucketClient, validatingResolver, s3BucketClient).aggregateMetadata();
    }

    private S3BucketClient getS3BucketClient() {
        return new S3BucketClient(
                System.getenv(BUCKET_NAME),
                new AmazonS3Client(new BasicAWSCredentials(
                        System.getenv("AWS_ACCESS_KEY"),
                        System.getenv("AWS_SECRET_KEY"))
                )
        );
    }

}
