package uk.gov.ida.metadataaggregator;

import uk.gov.ida.metadataaggregator.apigateway.ApiGatewayProxyResponse;
import uk.gov.ida.metadataaggregator.apigateway.ApiGatewayRequest;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataCurler;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataValidatingResolver;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataSourceException;

import static uk.gov.ida.metadataaggregator.Logging.log;

@SuppressWarnings("unused")
public class AwsLambdaHandlers {

    public ApiGatewayProxyResponse s3BucketLambda(ApiGatewayRequest testObject) {
        S3BucketClient s3BucketClient = getConfigS3BucketClient();
        new MetadataAggregator(s3BucketClient, new CountryMetadataCurler(), s3BucketClient).aggregateMetadata();
        return new ApiGatewayProxyResponse(200, null, null);
    }

    public void s3BucketLambda(AggregatorConfig testObject) {
        S3BucketClient s3BucketClient = getConfigS3BucketClient();
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

        new MetadataAggregator(getConfigS3BucketClient(), validatingResolver, getMetadataS3BucketClient()).aggregateMetadata();
    }

    private S3BucketClient getConfigS3BucketClient() {
        return new S3BucketClient(
                System.getenv("CONFIG_BUCKET"),
                System.getenv("AWS_ACCESS_KEY"),
                System.getenv("AWS_SECRET_KEY")
        );
    }

    private S3BucketClient getMetadataS3BucketClient() {
        return new S3BucketClient(
                System.getenv("METADATA_BUCKET"),
                System.getenv("AWS_ACCESS_KEY"),
                System.getenv("AWS_SECRET_KEY")
        );
    }
}
