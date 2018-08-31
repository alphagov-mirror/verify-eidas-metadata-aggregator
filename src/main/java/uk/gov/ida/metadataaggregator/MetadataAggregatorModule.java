package uk.gov.ida.metadataaggregator;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import uk.gov.ida.metadataaggregator.exceptions.ConfigSourceException;
import uk.gov.ida.metadataaggregator.configuration.MetadataAggregatorConfiguration;
import uk.gov.ida.metadataaggregator.configuration.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.configuration.MetadataSourceConfigurationLoader;
import uk.gov.ida.saml.metadata.EidasTrustAnchorResolver;

import javax.ws.rs.client.ClientBuilder;
import java.net.URI;
import java.security.KeyStore;

class MetadataAggregatorModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(S3BucketClient.class).to(uk.gov.ida.metadataaggregator.S3BucketClient.class);
    }

    @Provides
    @Named("TrustAnchorURI")
    public URI getTrustAnchorURI(MetadataAggregatorConfiguration configuration) {
        return configuration.getTrustAnchorUri();
    }

    @Provides
    public KeyStore getTrustStoreForTrustAnchor(MetadataAggregatorConfiguration configuration) {
        return configuration.getTrustStore();
    }

    @Provides
    public EidasTrustAnchorResolver getEidasTrustAnchorResolver(
        @Named("TrustAnchorURI") URI eidasTrustAnchorUriString,
        KeyStore trustStore
    ) {
        return new EidasTrustAnchorResolver(
            eidasTrustAnchorUriString,
            ClientBuilder.newClient(),
            trustStore);
    }

    @Provides
    public MetadataSourceConfigurationLoader getMetadataSourceConfigurationLoader(MetadataAggregatorConfiguration configuration) {
        return new MetadataSourceConfigurationLoader(configuration.getMetadataSourcesFile());
    }

    @Provides
    public MetadataSourceConfiguration getMetadataSourceConfiguration(MetadataSourceConfigurationLoader loader) throws ConfigSourceException {
        return loader.downloadConfig();
    }

    @Provides
    private uk.gov.ida.metadataaggregator.S3BucketClient getS3BucketClient(MetadataAggregatorConfiguration configuration, AmazonS3 amazonS3Client) {
        return new uk.gov.ida.metadataaggregator.S3BucketClient(configuration.getS3BucketName(), amazonS3Client);
    }

    @Provides
    private AmazonS3 getAmazonS3Client() {
        return AmazonS3ClientBuilder.standard()
            .withCredentials(new EnvironmentVariableCredentialsProvider())
            .build();
    }
}