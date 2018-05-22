package uk.gov.ida.metadataaggregator;

import uk.gov.ida.metadataaggregator.apigateway.ApiGatewayProxyResponse;
import uk.gov.ida.metadataaggregator.apigateway.ApiGatewayRequest;
import uk.gov.ida.metadataaggregator.config.AggregatorConfig;
import uk.gov.ida.metadataaggregator.config.ConfigSource;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataCurler;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataSource;
import uk.gov.ida.metadataaggregator.metadatasource.CountryMetadataValidatingResolver;
import uk.gov.ida.metadataaggregator.metadatasource.MetadataResolverException;
import uk.gov.ida.metadataaggregator.metadatastore.MetadataStore;

import java.io.IOException;
import java.util.Collection;

import static uk.gov.ida.metadataaggregator.Logging.log;

@SuppressWarnings("unused")
public class MetadataAggregator {

    public ApiGatewayProxyResponse s3BucketLambda(ApiGatewayRequest testObject) {
        aggregateMetadata(getConfigS3BucketClient(), getMetadataS3BucketClient(), new CountryMetadataCurler());
        return new ApiGatewayProxyResponse(200, null, null);
    }

    public void s3BucketLambda(AggregatorConfig testObject) {
        aggregateMetadata(getConfigS3BucketClient(), getMetadataS3BucketClient(), new CountryMetadataCurler());
    }

    public void s3BucketValidatingLambda(AggregatorConfig testObject) {
        String password = System.getenv("TRUST_ANCHOR_PASSCODE");
        //TODO What is the password, where does it come from and how do we keep it secure?
        String eidasTrustAnchorUriString = System.getenv("TRUST_ANCHOR_URI");

        CountryMetadataValidatingResolver validatingResolver;
        try {
            validatingResolver = CountryMetadataValidatingResolver.build(testObject, password, eidasTrustAnchorUriString);
        } catch (MetadataResolverException e) {
            log("Unable to build country metadatasource resolver", e);
            return;
        }

        aggregateMetadata(getConfigS3BucketClient(), getMetadataS3BucketClient(), validatingResolver);
    }

    private void aggregateMetadata(ConfigSource configSource,
                                   MetadataStore metadataStore,
                                   CountryMetadataSource countryMetadataCurler) {
        AggregatorConfig config;
        try {
            config = configSource.downloadConfig();
        } catch (IOException e) {
            log("Unable to retrieve config file", e);
            return;
        }

        log("Processing country metadatasource");

        int successfulUploads = 0;
        Collection<String> metadataUrls = config.getMetadataUrls();

        for (String url : metadataUrls) {
            boolean successfulUpload = processMetadataFrom(url, metadataStore, countryMetadataCurler);
            if (successfulUpload) successfulUploads++;
        }

        log(
                "Finished processing country metadatasource with {0} successful uploads out of {1}",
                successfulUploads,
                metadataUrls.size()
        );
    }

    private boolean processMetadataFrom(String url,
                                        MetadataStore metadataDestination,
                                        CountryMetadataSource countryMetadataCurler) {
        String countryMetadataFile;
        try {
            countryMetadataFile = countryMetadataCurler.downloadMetadata(url);
        } catch (IOException | MetadataResolverException e) {
            log("Error downloading metadatasource file {0}", e, url);
            return false;
        }

        try {
            metadataDestination.uploadMetadata(url, countryMetadataFile);
        } catch (IOException e) {
            log("Error uploading metadatasource file {0}", e, url);
            return false;
        }

        return true;
    }

    private S3BucketClient getConfigS3BucketClient() {
        return new S3BucketClient(
                System.getenv("CONFIG_BUCKET"),//"govukverify-eidas-metadatasource-aggregator-config-dev"
                System.getenv("AWS_ACCESS_KEY"),
                System.getenv("AWS_SECRET_KEY")
        );
    }

    private S3BucketClient getMetadataS3BucketClient() {
        return new S3BucketClient(
                System.getenv("METADATA_BUCKET"),//"govukverify-eidas-metadatasource-aggregator-dev"
                System.getenv("AWS_ACCESS_KEY"),
                System.getenv("AWS_SECRET_KEY")
        );
    }
}
