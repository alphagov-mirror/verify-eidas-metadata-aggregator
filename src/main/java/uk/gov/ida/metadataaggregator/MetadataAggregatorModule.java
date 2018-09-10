package uk.gov.ida.metadataaggregator;

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import uk.gov.ida.metadataaggregator.configuration.MetadataSourceConfiguration;
import uk.gov.ida.metadataaggregator.configuration.MetadataSourceConfigurationLoader;
import uk.gov.ida.metadataaggregator.core.S3BucketMetadataStore;
import uk.gov.ida.metadataaggregator.exceptions.ConfigSourceException;
import uk.gov.ida.metadataaggregator.managed.MetadataAggregationTaskRunner;
import uk.gov.ida.metadataaggregator.managed.ScheduledMetadataAggregator;
import uk.gov.ida.saml.metadata.EidasTrustAnchorResolver;

import javax.ws.rs.client.ClientBuilder;
import java.net.URI;
import java.security.KeyStore;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

class MetadataAggregatorModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ScheduledMetadataAggregator.class);
        bind(MetadataAggregationTaskRunner.class);
    }

    @Provides
    @Named("TrustAnchorURI")
    public URI getTrustAnchorURI(MetadataAggregatorConfiguration configuration) {
        return configuration.getTrustAnchorUri();
    }

    @Provides
    @Named("ScheduleFrequency")
    public long getScheduleFrequency(MetadataAggregatorConfiguration configuration) {
        return configuration.getHoursBetweenEachRun();
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
    private S3BucketMetadataStore getS3BucketClient(MetadataAggregatorConfiguration configuration, AmazonS3 amazonS3Client) {
        return new S3BucketMetadataStore(configuration.getS3BucketName(), amazonS3Client);
    }

    @Provides
    private AmazonS3 getAmazonS3Client(MetadataAggregatorConfiguration configuration) {
        return AmazonS3ClientBuilder.standard()
                .withRegion(configuration.getAwsRegion())
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .build();
    }

    @Provides
    @Singleton
    @Named("BlockingExecutor")
    public ScheduledExecutorService getScheduledExecutorService(){
        return Executors.newSingleThreadScheduledExecutor();
    }
}
